/***
 * JAVANAISE API
 * Contact:
 *
 * Authors:
 */

package jvn;

import java.rmi.*;
import java.io.*;
import java.rmi.RemoteException;

/**
* Remote interface of a JVN server (used by a remote JvnCoord)
* 
*/
public interface JvnRemoteServer extends Remote {

	/**
	* Invalidate the Read lock of a JVN object
	* @param joi : the JVN object id
	* @throws java.rmi.RemoteException,JvnException
	**/
	public void jvnInvalidateReader(int joi) throws RemoteException,JvnException;

	/**
	* Invalidate the Write lock of a JVN object
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
	public Serializable jvnInvalidateWriter(int joi) throws RemoteException,JvnException;

	/**
	* Reduce the Write lock of a JVN object
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi) throws RemoteException,JvnException;
   
   /**
	 * Allocate a NEW JVN server id (usually allocated to a newly created JVNServerImp)
	 *  
	 * @throws java.rmi.RemoteException
	 *             ,JvnException
	 **/
	public int jvnGetServerId() throws java.rmi.RemoteException, jvn.JvnException;

   

}
