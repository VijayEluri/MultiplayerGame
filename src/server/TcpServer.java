package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import net.GameStateManager;


public class TcpServer extends Thread {
	
	private ServerSocket servSock;
	private GameStateManager gm;
	
	public TcpServer(int p, GameStateManager g) {
		gm = g;
		try {
			servSock = new ServerSocket(p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run() {
		String msg;
		byte[] buf = new byte[NetworkEngine.TCP_BUF_SIZE]; 
		
		for(;;) {
			try {
				
				Socket clientSock = servSock.accept();
				InputStream in = clientSock.getInputStream();
				in.read(buf, 0, NetworkEngine.TCP_BUF_SIZE);
		
				msg = new String(buf);
				//System.out.println(msg + " len:"+msg.length());
				gm.processAction(msg);
				
				clientSock.close(); 
				
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}
	}
}