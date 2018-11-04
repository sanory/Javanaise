package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class JvnCoordMain {

	
	public static void main(String argv[]) throws JvnException {
		JvnRemoteCoord jav;
		try {
			jav = new JvnCoordImpl();
			Registry registry = LocateRegistry.createRegistry(1339);
			registry.bind("JavService", jav);
			System.out.println("Coordinateur pret");
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
}
