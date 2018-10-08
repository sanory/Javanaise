package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{
	
	/**
	 * 
	 */
	private int id;
	private LockState lockstate;
	private Serializable obj;
	
	
	public enum LockState {
		  NL, /*: no local lock */
		  RC, /*: read lock cached */
		  WC, /*: write lock cached */
		  R, /*: read lock taken */
		  W, /*: write lock taken */
		  WRC /*: read lock taken – write lock cached */     
	}
	
	/**
	* Default constructor
    * @throws JvnException
    **/
	public JvnObjectImpl(Serializable obj) throws Exception {
		this.id = 1 ; //TODO id
		this.lockstate= LockState.NL;
		this.obj = obj;
	}

	/**
	* Get a Read lock on the object 
	* @throws JvnException
	**/
	public void jvnLockRead() throws jvn.JvnException{
		switch (this.lockstate) {
			case NL:
				// appel serveur
				JvnServerImpl.jvnGetServer().jvnLockRead(this.id);
				this.lockstate=LockState.R;
				break;
				
			case WC:
				this.lockstate=LockState.WRC;
				break;
				
			case RC:
				this.lockstate=LockState.R;
				break;	
	
			default: // case R & W & WRC
				// Rien à faire
				break;
		}
	}

	/**
	* Get a Write lock on the object 
	* @throws JvnException
	**/
	public void jvnLockWrite() throws jvn.JvnException{
		switch (this.lockstate) {
			case NL:
				JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate=LockState.W;
				break;
				
			case R:
				this.lockstate = LockState.RC;
				JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;
				
			case WC:
				this.lockstate=LockState.W;
				break;
				
			case RC:
				JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;
				
			case WRC:
				throw new JvnException("Passage du lock de WRC à W !");		
	
			default: // case W
				// Rien à faire
				break;
		}
	} 

	/**
	* Unlock  the object 
	* @throws JvnException
	**/
	public void jvnUnLock()	throws jvn.JvnException{
		switch (this.lockstate) {
			case R:
				this.lockstate=LockState.RC;
				break;
				
			case W:
				this.lockstate=LockState.WC;
				break;
				
			case WRC:
				this.lockstate=LockState.WC;
				break;		
	
			default: // case WC & RC & NL
				// Rien à faire
				break;
		}
	} 
	
	
	/**
	* Get the object identification
	* @throws JvnException
	**/
	public int jvnGetObjectId()	throws jvn.JvnException{
		return this.id;
	} 
	
	/**
	* Get the object state
	* @throws JvnException
	**/
	public Serializable jvnGetObjectState()	throws jvn.JvnException{
		return this.obj;
	} 
	
	
	/**
	* Invalidate the Read lock of the JVN object 
	* @throws JvnException
	**/
  public void jvnInvalidateReader()	throws jvn.JvnException{
	  switch (this.lockstate) {
		case R:
			//throw new JvnException("Verrou non relaché");		
			
		case W:
			//throw new JvnException("Verrou non relaché");
			break;
			
		case WC:
			
			break;
			
		case RC:
			this.lockstate=LockState.NL;
			break;
			
		case WRC:
			//throw new JvnException("Verrou non relaché");
			break;		

		default:
			break;
		}
  }
	    
	/**
	* Invalidate the Write lock of the JVN object  
	* @return the current JVN object state
	* @throws JvnException
	**/
  public Serializable jvnInvalidateWriter()	throws jvn.JvnException{
	  switch (this.lockstate) {
		case R:
			
			
		case W:
			
			break;
			
		case WC:
			this.lockstate=LockState.NL;
			break;
			
		case RC:
			
			break;
			
		case WRC:
			//throw new JvnException("Verrou non relaché");
			break;		

		default:
			break;
		}
	return obj;
  }
	
	/**
	* Reduce the Write lock of the JVN object 
	* @return the current JVN object state
	* @throws JvnException
	**/
   public Serializable jvnInvalidateWriterForReader() throws jvn.JvnException{
	   switch (this.lockstate) {
		case R:
				
			
		case W:
			
			break;
			
		case WC:
			this.lockstate=LockState.RC;
			break;
			
		case RC:
			
			break;
			
		case WRC:
			//throw new JvnException("Verrou non relaché");
			break;		

		default:
			break;
		}
	   return null;
   }	
}
