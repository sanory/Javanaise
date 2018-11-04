package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8038646055377784498L;
	private int id;
	private LockState lockstate;
	private Serializable obj;

	public enum LockState {
		NL, /*: no local lock */
		RC, /*: read lock cached */
		WC, /*: write lock cached */
		R, /*: read lock taken */
		W, /*: write lock taken */
		RWC /*: read lock taken – write lock cached */
	}

	/**
	* Default constructor
	* @throws JvnException
	**/
	public JvnObjectImpl(Serializable obj, int id) throws Exception {
		this.id = id;
		this.lockstate = LockState.W; // Write lock on creation
		this.obj = obj;
	}
        
        /**
	* Coord constructor
	* @throws JvnException
	**/
        public JvnObjectImpl(Serializable obj, int id, LockState ls) throws Exception {
		this.id = id;
		this.lockstate = ls;
		this.obj = obj;
	}

	/**
	* Get a Read lock on the object
	* @throws JvnException
	**/
	public synchronized void jvnLockRead() throws JvnException {
		System.out.println("Lock R ");
		switch (this.lockstate) {
			case NL:
				// appel serveur
				this.obj = JvnServerImpl.jvnGetServer().jvnLockRead(this.id);
				this.lockstate = LockState.R;
				break;

			case WC:
				this.lockstate = LockState.RWC;
				break;

			case RC:
				this.lockstate = LockState.R;
				break;

			default: // case R & W & RWC
				throw new JvnException("Read lock already taken");
		}
	}

	/**
	* Get a Write lock on the object
	* @throws JvnException
	**/
	public synchronized void jvnLockWrite() throws JvnException {
		System.out.println("Obj Lock W ");
		switch (this.lockstate) {
			case NL:
				this.obj = JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;

			case R:
				this.obj = JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;

			case WC:
				this.lockstate = LockState.W;
				break;

			case RC:
				this.obj = JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;

			case RWC:
				this.lockstate = LockState.W;
				break;

			default: 
				break;
		}
	}

	/**
	* Unlock  the object
	* @throws JvnException
	**/
	public synchronized void jvnUnLock() throws JvnException {
		switch (this.lockstate) {
			case R:
				this.lockstate = LockState.RC;
				System.out.println("Wait ended");
                notify();
				break;

			case W:
				this.lockstate = LockState.WC;
				System.out.println("Wait ended");
				notify();
				break;

			case RWC:
				this.lockstate = LockState.WC;
				System.out.println("Wait ended");
				notify();
				break;

			default: // case WC & RC & NL
				throw new JvnException("No lock taken");
		}
	}

	/**
	* Get the object identification
	* @throws JvnException
	**/
	public int jvnGetObjectId() throws JvnException {
		return this.id;
	}

	/**
	* Get the object state
	* @throws JvnException
	**/
	public Serializable jvnGetObjectState() throws JvnException {
		return this.obj;
	}


	/**
	* Invalidate the Read lock of the JVN object
	* @throws JvnException
	**/
	public synchronized void jvnInvalidateReader() throws JvnException {
		switch (this.lockstate) {
			case RWC:
			case R:
				try {                    
                    System.out.println("waiting reader");
                    wait();             
				} catch(InterruptedException e) {
					throw new JvnException("InvalidateReader Error");
				}
				this.lockstate = LockState.NL;
				break;

			case RC:
				this.lockstate = LockState.NL;
				break;

			default: 
				this.lockstate = LockState.NL;
				
		}
	}

	/**
	* Invalidate the Write lock of the JVN object
	* @return the current JVN object state
	* @throws JvnException
	**/
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		switch(this.lockstate){
		case W:
		case RWC:
			try {				
				wait();
				System.out.println("waiting writer");
				
			} catch(InterruptedException e) {
				throw new JvnException("InvalidateWriter Error");
			}
			this.lockstate = LockState.NL;
			return obj;
		case WC:
			this.lockstate = LockState.NL;
			return obj;
		default:
			throw new JvnException("InvalidateWriter when no writeLock");
		}
		
	}

	/**
	* Reduce the Write lock of the JVN object
	* @return the current JVN object state
	* @throws JvnException
	**/
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		switch(this.lockstate) {
		case W:
			try {
				System.out.println("Wait for invalidateWriterForReader : W");
				wait();
				this.lockstate = LockState.RC;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		case RWC:
			try {
				System.out.println("Wait for invalidateWriterForReader : RWC");
				wait();
				lockstate = LockState.R;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			break;
		case WC:		
			lockstate = LockState.RC;
			break;
		default:
			throw new JvnException("InvalidateWriterForReader called when no write lock");
	}
	
	return jvnGetObjectState();
}
}
