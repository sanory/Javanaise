/***
* JAVANAISE Implementation
* JvnServerImpl class
* Contact:
*
* Authors:
*/

package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.io.*;
import java.rmi.registry.*;
import java.rmi.RemoteException;


public class JvnServerImpl extends UnicastRemoteObject implements JvnLocalServer, JvnRemoteServer {
	// A JVN server is managed as a singleton
	private static JvnServerImpl js = null;
	private static String host = "localhost";
	private JvnRemoteCoord remoteCoord;

	private HashMap<Integer,JvnObject> objects; // List of objects present on this server

	/**
	* Default constructor
	* @throws JvnException
	**/
	private JvnServerImpl() throws Exception {
		super();

		Registry registry = LocateRegistry.getRegistry(this.host,1333);
		this.remoteCoord = (JvnRemoteCoord) registry.lookup("JavService");

		this.objects = new HashMap<>();
	}

	/**
	* Static method allowing an application to get a reference to
	* a JVN server instance
	* @throws JvnException
	**/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}

	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public synchronized void jvnTerminate() throws JvnException {
		try {
			this.remoteCoord.jvnTerminate(this);
		} catch (Exception e) {
			throw new JvnException("Erreur lors de la fermeture du serveur");
		}
	}

	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public synchronized JvnObject jvnCreateObject(Serializable o) throws JvnException {
		try {
			int id = this.remoteCoord.jvnGetObjectId();
			JvnObjectImpl newJO = new JvnObjectImpl(o,id);
			this.objects.put(id,newJO);
			return newJO;
		} catch (Exception e) {
			throw new JvnException("Erreur lors de la création de l'objet");
		}
	}

	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object
	* @throws JvnException
	**/
	public synchronized void jvnRegisterObject(String jon, JvnObject jo) throws JvnException {
		try {
			this.remoteCoord.jvnRegisterObject(jon,jo,this);
		} catch (Exception e) {
			throw new JvnException("Erreur : Objet non enregistré (nom déjà utilisé ?)");
		}
	}

	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object
	* @throws JvnException
	**/
	public synchronized JvnObject jvnLookupObject(String jon) throws JvnException {
		try {
			JvnObject newJO = this.remoteCoord.jvnLookupObject(jon,this);
			if (newJO!= null && !this.objects.containsKey(newJO.jvnGetObjectId())) this.objects.put(newJO.jvnGetObjectId(),newJO);
			return newJO;
		} catch (Exception e) {
                        System.out.println(e);
			throw new JvnException("Erreur lors de la récupération de l'objet");
		}
	}

	/**
	* Get a Read lock on a JVN object
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public synchronized Serializable jvnLockRead(int joi) throws JvnException {
		try {
			return this.remoteCoord.jvnLockRead(joi,this);
		} catch (Exception e) {
			throw new JvnException("Erreur lors du verrouillage de l'objet");
		}
	}
	/**
	* Get a Write lock on a JVN object
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public synchronized Serializable jvnLockWrite(int joi) throws JvnException {
		try {
			return this.remoteCoord.jvnLockWrite(joi,this);
		} catch (Exception e) {
			throw new JvnException("Erreur lors du verrouillage de l'objet");
		}
	}

	/**
	* Invalidate the Read lock of the JVN object identified by id
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized void jvnInvalidateReader(int joi) throws JvnException {
		this.objects.get(joi).jvnInvalidateReader();
	};

	/**
	* Invalidate the Write lock of the JVN object identified by id
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized Serializable jvnInvalidateWriter(int joi) throws RemoteException,JvnException {
		return this.objects.get(joi).jvnInvalidateWriter();
	};

	/**
	* Reduce the Write lock of the JVN object identified by id
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
	public synchronized Serializable jvnInvalidateWriterForReader(int joi) throws RemoteException,JvnException {
		return this.objects.get(joi).jvnInvalidateWriterForReader();
	};
}
