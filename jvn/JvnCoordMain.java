package jvn;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class JvnCoordMain {

	
	public void main() throws JvnException {
		JvnRemoteCoord jav;
		try {
			jav = new JvnCoordImpl();
			JvnRemoteCoord j_stub = (JvnRemoteCoord) UnicastRemoteObject.exportObject(jav, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.bind("JavService", j_stub);
		}
		catch (Exception e) {
			throw new JvnException("Erreur lors de la création de l'objet");
		}
	}
}
