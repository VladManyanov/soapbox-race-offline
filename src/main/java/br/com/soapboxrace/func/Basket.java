package br.com.soapboxrace.func;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

public class Basket {

	private Functions fx = new Functions();

	public void processBasket(String basketTrans) {
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			String basketId = fx.parseBasketId(basketTrans);
			Functions.log("|| Обнаружена покупка.");
			Functions.log("  || Запуск модуля экономики.");
			Economy economy = new Economy(mapBasketId(basketId), basketId, false);
			if (mapBasketId(basketId) == null)
				return;
			if (economy.transCurrency(true)) {
				if (basketId.contains("SRV-CARSLOT")) {
					Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml"));
					doc.getElementsByTagName("OwnedCarSlotsCount").item(0)
							.setTextContent(String.valueOf(Integer.parseInt(doc.getElementsByTagName("OwnedCarSlotsCount").item(0).getTextContent()) + 1));

					fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");

					Functions.log("|| -> Количество мест в гараже увеличено. [+1]");
				} else if (basketId.contains("SRV-REPAIR")) {
					Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml"));
					doc.getElementsByTagName("Durability").item(Integer.parseInt(fx.ReadCarIndex())).setTextContent("100");

					fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");

					Functions.log("|| -> Машина была отремонтирована.");
				} else if (basketId.contains("SRV-POWERUP")) {
					int index = Integer.parseInt(basketId.replace("SRV-POWERUP", ""));
					Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml"));
					String newAmount = String.valueOf(Integer.parseInt(doc.getElementsByTagName("RemainingUseCount").item(index).getTextContent()) + 15);
					doc.getElementsByTagName("RemainingUseCount").item(index).setTextContent(newAmount);

					fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/objects.xml");
				} else if (basketId.contains("SRV-THREVIVE")) {
					Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml"));
					doc.getElementsByTagName("IsStreakBroken").item(0).setTextContent("false");

					fx.WriteXML(doc, "www/soapbox/Engine.svc/events/gettreasurehunteventsession.xml");
				} else {
					if (Files.exists(Paths.get("www/basket/" + basketId + ".xml"))) {
						Functions.log("|| -> Покупка машины " + basketId + " прошла успешно.");
						fx.FixCarslots();
						Document doc = docBuilder.parse(new File("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml"));
						int lastIdIndex = doc.getElementsByTagName("Id").getLength() - 1;
						String carId = String.valueOf(Integer.parseInt(doc.getElementsByTagName("Id").item(lastIdIndex).getTextContent()) + 1);

						Document doc2 = docBuilder.parse(new File("www/basket/" + basketId + ".xml"));
						doc2.getElementsByTagName("Id").item(1).setTextContent(carId);
						Node carTrans = doc.importNode(doc2.getFirstChild(), true);
						doc.getElementsByTagName("CarsOwnedByPersona").item(0).appendChild(carTrans);
						Functions.log("|| -> Новая машина была включена в файл carslots.");
						int _carId = Integer.parseInt(carId) - 1;
						doc.getElementsByTagName("DefaultOwnedCarIndex").item(0).setTextContent(String.valueOf(_carId));
						Functions.log("|| -> CarIndex был назначен под номер новой машины.");
						fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");
						fx.WriteTempCar(new String(Files.readAllBytes(Paths.get("www/basket/" + basketId + ".xml")), StandardCharsets.UTF_8));
						Functions.log("|| -> Машина [ID = " + carId + "; Index = " + _carId + "] куплена и выбрана.");
					} else {
						Functions.log("|| !!!! -> Файл Basket отсутствует: " + basketId);
					}
				}
			} else {
				Functions.answerData = "";
				Functions.log("  || !! -> У вас недостаточно денег!");
			}
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SAXException sae) {
			sae.printStackTrace();
		}
	}

	public void SellCar(String serialNumber) {
		if (serialNumber != null) {
			try {
				Functions.log("|| Обнаружено действие продажи.");

				int carId = fx
						.CountInstances(new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml")),
								StandardCharsets.UTF_8), "<OwnedCarTrans>", "<Id>" + serialNumber + "</Id>")
						- 1;
				int carAm = fx
						.CountInstances(new String(Files.readAllBytes(Paths.get("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml")),
								StandardCharsets.UTF_8), "<OwnedCarTrans>", "</CarsOwnedByPersona>");
				if (carAm > 1) {
					Functions.log("|| -> В гараже более одной машины, можно продолжать.");
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
							.parse("www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");
					doc.getElementsByTagName("DefaultOwnedCarIndex").item(0).setTextContent("0");

					Functions.log("  || Запуск модуля экономики.");
					Economy economy = new Economy(doc.getElementsByTagName("ResalePrice").item(carId).getTextContent(), "0", true);
					Functions.log("  || -> " + doc.getElementsByTagName("ResalePrice").item(carId).getTextContent() + "IGC добавлены на ваш счёт.");
					economy.transCurrency(false);

					Node carToSell = doc.getElementsByTagName("OwnedCarTrans").item(carId);
					doc.getElementsByTagName("CarsOwnedByPersona").item(0).removeChild(carToSell);
					Functions.log("|| -> Машина удалена из файла Carslots.");

					Node OwnedCar = doc.getElementsByTagName("OwnedCarTrans").item(0);
					DOMImplementationLS lsImpl = (DOMImplementationLS) OwnedCar.getOwnerDocument().getImplementation().getFeature("LS", "3.0");
					LSSerializer serializer = lsImpl.createLSSerializer();
					serializer.getDomConfig().setParameter("xml-declaration", false);
					String StringOwnedCar = serializer.writeToString(OwnedCar);
					fx.WriteTempCar(StringOwnedCar);
					fx.WriteXML(doc, "www/soapbox/Engine.svc/personas/" + Functions.personaId + "/carslots.xml");

					Functions.log("|| Машина [ID = " + serialNumber + "; Index = " + carId + "] продана.");
				} else {
					Functions.log("|| Машина [ID = " + serialNumber + "; Index = 0] является последней в гараже, нельзя продать.");
				}
			} catch (ParserConfigurationException pce) {
				pce.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (SAXException sae) {
				sae.printStackTrace();
			}
		}
	}

	private String mapBasketId(String basketId) {
		if (basketId.contains("SRV-POWERUP")) {
			return "productsInCategory_STORE_POWERUPS.xml";
		} else if (basketId.contains("SRV-CARSLOT")) {
			return "productsInCategory_NFSW_NA_EP_CARSLOTS.xml";
		} else if (basketId.contains("SRV-REPAIR")) {
			return "productsInCategory_NFSW_NA_EP_REPAIRS.xml";
		} else if (basketId.contains("SRV-THREVIVE")) {
			return "productsInCategory_STORE_STREAK_RECOVERY.xml";
		} else if (basketId.contains("SRV-CAR")) {
			return "productsInCategory_NFSW_NA_EP_PRESET_RIDES_ALL_Category.xml";
		}
		return null;
	}
}
