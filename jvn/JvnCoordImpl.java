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
	/**
	 * 
	 */
	private static final long serialVersionUID = -3380652911182601617L;
	HashMap<Integer, String> mapJoiToName; // Id -> Object name
	HashMap<String, Integer> mapNameToJoi; // Id -> Object name
	HashMap<String, Serializable> mapNameToObj; // Object name -> Javanaise Object
	HashMap<Integer,JvnRemoteServer> lockWrites; // Objects write locked by which JvnServer
	HashMap<Integer,ArrayList<JvnRemoteServer>> lockReads; // Objects read locked by which JvnServer (multiple possible)

	/**
	 * Singleton variable
	*/
	private static JvnCoordImpl jc;
	private int idCnt = 0; // Count for JOI
	private int idSrv = 0; // Count for servers id

	/**
	* Default constructor
	* @throws JvnException
	**/
	public JvnCoordImpl() throws Exception {
		this.mapJoiToName = new HashMap<>();
		this.mapNameToJoi = new HashMap<>();
		this.mapNameToObj = new HashMap<>();
		this.lockWrites = new HashMap<>();
		this.lockReads = new HashMap<>();
	}
	
	public static JvnCoordImpl jvnGetServer() {
		if (jc == null){
			try {
				jc = new JvnCoordImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return jc;
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
	
	public synchronized int jvnGetServerId() throws RemoteException, JvnException {
		this.idSrv++;
		return this.idSrv;
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
		if (mapNameToObj.containsKey(jon)) {
			throw new JvnException("'" + jon + "' is already registered");
		}else {
			mapJoiToName.put(jo.jvnGetObjectId(), jon);
			mapNameToJoi.put(jon, jo.jvnGetObjectId());
			mapNameToObj.put(jon, jo.jvnGetObjectState());
			lockWrites.put(jo.jvnGetObjectId(), js);
			System.out.println("Le lock W est au serveur " + js.jvnGetServerId());
		}
		
	}

	/**
	* Get the reference of a JVN object managed by a given JVN server
	* @param jon : the JVN object name
	* @param js : the remote reference of the JVNServer
	 * @throws Exception 
	**/
	public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js) throws Exception {
		try {			
			Serializable jo = mapNameToObj.get(jon);
			int id = mapNameToJoi.get(jon);
			System.out.println("Object " + jon + " find");
			return new JvnObjectImpl(jo, id, LockState.NL);
		} catch (NullPointerException e) {
			return null;
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
		ArrayList<JvnRemoteServer> l = new ArrayList<JvnRemoteServer>();
		System.out.println("Lock R de : " +  js.jvnGetServerId() );
		Serializable o;
		try {			
			if(!lockWrites.containsKey(joi)) {
				if(lockReads.containsKey(joi)) {
					l = this.lockReads.get(joi);
				}									
					l.add(js);
					lockReads.put(joi, l);
					System.out.println("Le lock R est au serveur " + js.jvnGetServerId());
			
				o = this.mapNameToObj.get(this.mapJoiToName.get(joi));
			} else {
				o = this.lockWrites.get(joi).jvnInvalidateWriterForReader(joi);				
				this.mapNameToObj.put(this.mapJoiToName.get(joi),o);				
				int id = this.lockWrites.get(joi).jvnGetServerId();					
				System.out.println("Le lock W retiré au serveur :" + id);
				l.add(this.lockWrites.get(joi));
				this.lockWrites.remove(joi);
				System.out.println("Le lock R est au serveur " + id);
				if (lockReads.containsKey(joi)) {
					l.addAll(this.lockReads.get(joi));
				}
				l.add(js);
				this.lockReads.put(joi,l);		
				System.out.println("Le lock R est au serveur " + js.jvnGetServerId());
			}
			
		} catch(Exception e) {
			throw new JvnException("LockRead error : " + e.getMessage());	
		}
		return o;
		
	}

	/**
	* Get a Write lock on a JVN object managed by a given JVN server
	* @param joi : the JVN object identification
	* @param js  : the remote reference of the server
	* @return the current JVN object state
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js) throws RemoteException, JvnException {
		System.out.println("Lock W :" + js.jvnGetServerId());
		Serializable o;
		try {
			if(!lockWrites.containsKey(joi)) {
				if(lockReads.containsKey(joi) && !lockReads.get(joi).isEmpty()) {	
					ArrayList<JvnRemoteServer> al =this.lockReads.get(joi);
					for(int i=0; i<al.size(); i++) {	
							if(!(al.get(i).jvnGetServerId()==js.jvnGetServerId())) {
								System.out.println("Le lock R est retiré au serveur " + al.get(i).jvnGetServerId());
								al.get(i).jvnInvalidateReader(joi);	
							}else {
								System.out.println("Le lock R est retiré au serveur " + js.jvnGetServerId());							
							}
						}
				}
				this.lockReads.get(joi).clear();
				o =this.mapNameToObj.get(this.mapJoiToName.get(joi));
				this.lockWrites.put(joi, js);
				System.out.println("Le lock W est au serveur " + js.jvnGetServerId());
			} else {
				if(!(this.lockWrites.get(joi).jvnGetServerId()==js.jvnGetServerId())) {
					o = this.lockWrites.get(joi).jvnInvalidateWriter(joi);	
				}else {
					o =this.mapNameToObj.get(this.mapJoiToName.get(joi));
				}
				this.mapNameToObj.put(this.mapJoiToName.get(joi),o);
				this.lockWrites.put(joi, js);
				System.out.println("Le lock W est au serveur " + js.jvnGetServerId());
				
			}
			
		} catch(Exception e) {
			throw new JvnException("LockWrite error : " + e.getMessage());
		}finally {
		}
		return o;
	}

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
	public synchronized void jvnTerminate(JvnRemoteServer js) throws RemoteException, JvnException {
		try {
			//Trouver et supprimer les références à js
		} finally{
		}
	}
	
	public synchronized void jvnSaveCoord() {
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
	
	public synchronized void jvnRestoreCoord() {
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
