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
import java.util.ArrayList;
import java.io.Serializable;
import java.rmi.RemoteException;
///////////////////////////////// TODO TODO TODO : Possiblement des méthodes à mettre en synchronized /////////////////////////////////////////////
public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord {
	HashMap<Integer, String> mapJoiToName; // Id -> Object name
	HashMap<String, JvnObject> mapNameToObj; // Object name -> Javanaise Object
	HashMap<Integer,JvnRemoteServer> lockWrites; // Objects write locked by which JvnServer
	HashMap<Integer,ArrayList<JvnRemoteServer>> lockReads; // Objects read locked by which JvnServer (multiple possible)

	private int idCnt = 0; // Count for JOI

	/**
	* Default constructor
	* @throws JvnException
	**/
	public JvnCoordImpl() throws Exception {
		this.mapJoiToName = new HashMap<>();
		this.mapNameToObj = new HashMap<>();
		this.lockWrites = new HashMap<>();
		this.lockReads = new HashMap<>();
	}


	/**
	*  Allocate a NEW JVN object id (usually allocated to a
	*  newly created JVN object)
	* @throws java.rmi.RemoteException,JvnException
	**/
	public int jvnGetObjectId() throws RemoteException, JvnException {
		this.idCnt++;
		return this.idCnt;
	}

	/**
	* Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo  : the JVN object
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the JVNServer
	* @throws java.rmi.RemoteException,JvnException
	**/
	public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) throws RemoteException, JvnException {
		mapJoiToName.put(jo.jvnGetObjectId(), jon);
		mapNameToObj.put(jon, jo);
		lockWrites.put(jo.jvnGetObjectId(), js);
	}

	/**
	* Get the reference of a JVN object managed by a given JVN server
	* @param jon : the JVN object name
	* @param js : the remote reference of the JVNServer
	* @throws java.rmi.RemoteException,JvnException
	**/
	public JvnObject jvnLookupObject(String jon, JvnRemoteServer js) throws RemoteException, JvnException {
		return mapNameToObj.get(jon);
	}

	/**
	* Get a Read lock on a JVN object managed by a given JVN server
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public Serializable jvnLockRead(int joi, JvnRemoteServer js) throws RemoteException, JvnException {
		if(!lockWrites.containsKey(joi)) {
			if(lockReads.containsKey(joi)) {
				ArrayList<JvnRemoteServer> l = lockReads.get(joi);
				l.add(js);
				lockReads.replace(joi, l);
			} else {
				ArrayList<JvnRemoteServer> l = new ArrayList<>();
				l.add(js);
				lockReads.put(joi, l);
			}
		} else {
			//TODO envoi de jvnInvalidateWriter() au tenant du verrou write
		}

		return null;
	}

	/**
	* Get a Write lock on a JVN object managed by a given JVN server
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws RemoteException, JvnException {
		// to be completed
		return null;
	}

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
	public void jvnTerminate(JvnRemoteServer js) throws RemoteException, JvnException {
		// on récup les objets en WC / RWC / W? du JVN server
	}
}
