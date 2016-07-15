package br.com.soapboxrace.xmpp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import br.com.soapboxrace.func.Functions;

public class XmppSrv {

	public static ConcurrentHashMap<Long, XmppTalk> xmppClients = new ConcurrentHashMap<Long, XmppTalk>();

	public static void addXmppClient(long personaId, XmppTalk xmppClient) {
		xmppClients.put(personaId, xmppClient);
	}

	public static void sendMsg(long personaId, String msg) {
		if (xmppClients.containsKey(personaId)) {
			XmppTalk xTalk = xmppClients.get(personaId);
			if (xTalk != null) {
				xTalk.write(msg);
			} else {
				System.err.println("xmppClient с personaId " + personaId + " присоединён к нулевой XmppTalk копии!");
			}
		} else {
			System.err.println("xmppClients не содержит personaId " + personaId);
		}
	}

	public static void removeXmppClient(int personaId) {
		xmppClients.remove(personaId);
	}

	public static void main(String[] args) throws Exception {
		XmppSrv xmppSrv = new XmppSrv();
		xmppSrv.start();
	}

	public void start() {
		XmppSrvRun xmppSrvRun = new XmppSrvRun();
		xmppSrvRun.start();
	}

	private static class XmppSrvRun extends Thread {
		public void run() {
			try {
				Functions.log("XMPP сервер запущен.");
				Functions.log("");
				ServerSocket listener = new ServerSocket(5222);
				try {
					while (true) {
						new Capitalizer(listener.accept()).start();
					}
				} finally {
					listener.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class Capitalizer extends Thread {
		private Socket socket;
		private XmppTalk xmppTalk;

		public Capitalizer(Socket socket) {
			this.socket = socket;
			xmppTalk = new XmppTalk(this.socket);
			Functions.log("Новое соединение на " + socket);
		}

		public void run() {
			try {
				new XmppHandShake(xmppTalk);
				XmppHandler xmppHandler = new XmppHandler(xmppTalk);
				while (true) {
					String input = xmppHandler.read();
					if (input == null || input.contains("</stream:stream>")) {
						break;
					}
				}
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					Functions.log("Невозможно закрыть соединение, в чём дело?");
				}
				XmppSrv.removeXmppClient(xmppTalk.getPersonaId());
				Functions.log("Соединение с клиентом закрыто");
			}
		}

	}

	public static XmppTalk get(Long personaId) {
		return xmppClients.get(personaId);
	}

}