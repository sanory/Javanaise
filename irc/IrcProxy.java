/***
 * Irc class : simple implementation of a chat using JAVANAISE
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*;

import jvn.*;

import java.io.*;


public class IrcProxy {
	public TextArea		text;
	public TextField	data;
	Frame 			frame;
	SentenceProxy   sentence;
	Button unlock_button;


  /**
  * main method
  * create a JVN object named IRC for representing the Chat application
  **/
	public static void main(String argv[]) {
	   try {
		   
		   SentenceProxy s = (SentenceProxy) JvnProxy.newInstance("IRC", Sentence.class);
		   
		// create the graphical part of the Chat application
		 new IrcProxy(s);
	   
	   } catch (Exception e) {
		   System.out.println("IRC problem : " + e.getMessage());
		   e.printStackTrace();
	   }
	}

  /**
   * IRC Constructor
   @param jo the JVN object representing the Chat
   **/
	public IrcProxy(SentenceProxy jo) {
		sentence = jo;
		frame=new Frame();
		frame.setLayout(new GridLayout(1,1));
		text=new TextArea(10,60);
		text.setEditable(false);
		text.setForeground(Color.red);
		frame.add(text);
		data=new TextField(40);
		frame.add(data);
		Button read_button = new Button("read");
		read_button.addActionListener(new readListener2(this));
		frame.add(read_button);
		Button write_button = new Button("write");
		write_button.addActionListener(new writeListener2(this));
		frame.add(write_button);
		/*unlock_button = new Button("is unlocked");
		unlock_button.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					sentence.jvnUnLock();
					unlock_button.setLabel("is unlocked");
				} catch (JvnException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
			
		});
		frame.add(unlock_button);*/
		frame.setSize(545,201);
		text.setBackground(Color.black); 
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e){
				try {
					JvnServerImpl.jvnGetServer().jvnTerminate();
				} catch (JvnException e1) {
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});
	}
}


 /**
  * Internal class to manage user events (read) on the CHAT application
  **/
 class readListener2 implements ActionListener {
	 IrcProxy irc;
  
	public readListener2 (IrcProxy i) {
		irc = i;
	}
   
 /**
  * Management of user events
  **/
	public void actionPerformed (ActionEvent e) {
	 try {
		// lock the object in read mode
//		irc.sentence.jvnLockRead();
//		irc.unlock_button.setLabel("need unlock");
		
		// invoke the method
		String s = irc.sentence.read();
		
		// unlock the object
		//irc.sentence.jvnUnLock();
		
		// display the read value
		irc.data.setText(s);
		irc.text.append(s+"\n");
	   } catch (JvnException je) {
		   System.out.println("IRC problem : " + je.getMessage());
	   }
	}
}

 /**
  * Internal class to manage user events (write) on the CHAT application
  **/
 class writeListener2 implements ActionListener {
	 IrcProxy irc;
  
	public writeListener2 (IrcProxy i) {
        	irc = i;
	}
  
  /**
    * Management of user events
   **/
	public void actionPerformed (ActionEvent e) {
	   try {	
		// get the value to be written from the buffer
    String s = irc.data.getText();
        	
    // lock the object in write mode
//		irc.sentence.jvnLockWrite();
//		irc.unlock_button.setLabel("need unlock");
		
		// invoke the method
		irc.sentence.write(s);
		
		// unlock the object
		//irc.sentence.jvnUnLock();
	 } catch (JvnException je) {
		   System.out.println("IRC problem  : " + je.getMessage());
	 }
	}
}

