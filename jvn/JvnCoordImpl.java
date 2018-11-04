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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import jvn.JvnObjectImpl.LockState;

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
	public synchronized int jvnGetObjectId() throws RemoteException, JvnException {
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
	public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js) throws RemoteException, JvnException {
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
	public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js) throws RemoteException, JvnException {
		try {
			if(this.mapNameToObj.containsKey(jon)){
				Serializable s = this.jvnLockRead(this.mapNameToObj.get(jon).jvnGetObjectId(),js);
				JvnObject newJO = new JvnObjectImpl(s,this.mapNameToObj.get(jon).jvnGetObjectId(),LockState.R);
				mapNameToObj.replace(jon, newJO);
				return newJO;
			} else {
				return null;
			}
		} catch(Exception e) {
			throw new JvnException("LookUp error : " + e);
		}
	}

	/**
	* Get a Read lock on a JVN object managed by a given JVN server
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js) throws RemoteException, JvnException {
		try {
			if(!lockWrites.containsKey(joi)) {
				if(lockReads.containsKey(joi)) {
					lockReads.get(joi).add(js);
				} else {
					ArrayList<JvnRemoteServer> l = new ArrayList<>();
					l.add(js);
					lockReads.put(joi, l);
				}
			} else {
				Serializable o = this.lockWrites.get(joi).jvnInvalidateWriterForReader(joi);
				JvnObject newJO = new JvnObjectImpl(o,joi,LockState.R);
				this.mapNameToObj.replace(this.mapJoiToName.get(joi),newJO);
				this.lockWrites.remove(joi);
				if (!lockReads.containsKey(joi)) this.lockReads.put(joi,new ArrayList<>());
				this.lockReads.get(joi).add(js);
			}
			return this.mapNameToObj.get(this.mapJoiToName.get(joi)).jvnGetObjectState();
		} catch(Exception e) {
			throw new JvnException("LockRead error : " + e.getMessage());
		}
	}

	/**
	* Get a Write lock on a JVN object managed by a given JVN server
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws RemoteException, JvnException {
		try {
			if(!lockWrites.containsKey(joi)) {
				for(JvnRemoteServer ts : this.lockReads.get(joi)) {
					ts.jvnInvalidateReader(joi);
				}
				this.lockReads.get(joi).clear();
			} else {
				Serializable o = this.lockWrites.get(joi).jvnInvalidateWriter(joi);
				JvnObject newJO = new JvnObjectImpl(o,joi);
				this.mapNameToObj.replace(this.mapJoiToName.get(joi),newJO);
				this.lockWrites.remove(joi);
			}
			return this.mapNameToObj.get(this.mapJoiToName.get(joi)).jvnGetObjectState();
		} catch(Exception e) {
			throw new JvnException("LockWrite error : " + e.getMessage());
		}
	}

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized void jvnTerminate(JvnRemoteServer js) throws RemoteException, JvnException {
		// on rÃ©cup les objets en WC / RWC / W? du JVN server
	}
	
	public synchronized void jvnSaveCoordState() {
		try {
			FileOutputStream backupCoord = new FileOutputStream("coordbackup.txt");
			ObjectOutputStream oos = new ObjectOutputStream(backupCoord);
			oos.writeObject(this);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void jvnRestoreCoordState() {
		File f = new File("coordbackup.txt");
		if (f.exists()) {
			System.out.println("Restauration de la dernière instance du  coordinateur");
			try {
				FileInputStream backupCoord = new FileInputStream("coord.ser");
				ObjectInputStream ois = new ObjectInputStream(backupCoord);
				JvnCoordImpl coord = (JvnCoordImpl) ois.readObject();
				this.mapJoiToName = coord.mapJoiToName;
				this.mapNameToObj = coord.mapNameToObj;
				this.lockWrites = coord.lockWrites;
				this.lockReads = coord.lockReads;
				this.idCnt = coord.idCnt;				
				ois.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
