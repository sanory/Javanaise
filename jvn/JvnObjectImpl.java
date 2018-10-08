package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {
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
	* Get a Read lock on the object
	* @throws JvnException
	**/
	public synchronized void jvnLockRead() throws JvnException {
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
				// Rien à faire
				break;
		}
	}

	/**
	* Get a Write lock on the object
	* @throws JvnException
	**/
	public synchronized void jvnLockWrite() throws JvnException {
		switch (this.lockstate) {
			case NL:
				this.obj = JvnServerImpl.jvnGetServer().jvnLockWrite(this.id);
				this.lockstate = LockState.W;
				break;

			case R:
				this.lockstate = LockState.RC;
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
				throw new JvnException("Passage du lock de RWC à W !");

			default: // case W
				// Rien à faire
				break;
		}
	}

	/**
	* Unlock  the object
	* @throws JvnException
	**/
	public synchronized void jvnUnLock()	throws JvnException {
		switch (this.lockstate) {
			case R:
				notify();
				this.lockstate = LockState.RC;
				break;

			case W:
				notify();
				this.lockstate = LockState.WC;
				break;

			case RWC:
				notify();
				this.lockstate = LockState.WC;
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
	public int jvnGetObjectId()	throws JvnException {
		return this.id;
	}

	/**
	* Get the object state
	* @throws JvnException
	**/
	public Serializable jvnGetObjectState()	throws JvnException {
		return this.obj;
	}


	/**
	* Invalidate the Read lock of the JVN object
	* @throws JvnException
	**/
	public synchronized void jvnInvalidateReader() throws JvnException {
		switch (this.lockstate) {
			case R:
				try {
					wait();
				} catch(InterruptedException e) {
					throw new JvnException("InvalidateReader Error");
				}
				this.lockstate = LockState.NL;
				break;

			case RC:
				this.lockstate = LockState.NL;
				break;

			default: // case W & WC & NL & RWC
				throw new JvnException("InvalidateReader Error");
		}
	}

	/**
	* Invalidate the Write lock of the JVN object
	* @return the current JVN object state
	* @throws JvnException
	**/
	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		if (this.lockstate == LockState.W || this.lockstate == LockState.RWC) {
			try {
				wait();
			} catch(InterruptedException e) {
				throw new JvnException("InvalidateWriter Error");
			}
			this.lockstate = LockState.NL;
			return obj;
		} else if (this.lockstate == LockState.WC) {
			this.lockstate = LockState.NL;
			return obj;
		} else throw new JvnException("InvalidateWriter Error"); // case R & RC & NL
	}

	/**
	* Reduce the Write lock of the JVN object
	* @return the current JVN object state
	* @throws JvnException
	**/
	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {
		if (this.lockstate == LockState.W || this.lockstate == LockState.RWC) {
			try {
				wait();
			} catch(InterruptedException e) {
				throw new JvnException("InvalidateWriterForReader Error");
			}
			this.lockstate = LockState.RC;
			return obj;
		} else if (this.lockstate == LockState.WC) {
			this.lockstate = LockState.RC;
			return obj;
		} else throw new JvnException("InvalidateWriterForReader Error"); // case R & RC & NL
	}
}
