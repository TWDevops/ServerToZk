package com.e104.common;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;

public class ServerInit implements LifecycleListener {

	/**
	 * 
	 */
	private static final int SESSION_TIMEOUT = 10000; 
	private ZooKeeper zk; 
	Thread monitorDaemon;
	
	 
	private static InetAddress getEth0Address() throws SocketException {
		 
		 NetworkInterface ni = NetworkInterface.getByName("eth0");    
		 Enumeration<InetAddress> inetAddresses =  ni.getInetAddresses();
		 while(inetAddresses.hasMoreElements()) {  
		          InetAddress addr = inetAddresses.nextElement();  
		          if (!addr.isLoopbackAddress() && addr instanceof Inet4Address)
	                    return addr;
		 }  
		 return null;
	}

	public class ConnWatcher implements Watcher {  
		
		CountDownLatch connectedSignal;
		ConnWatcher(CountDownLatch connectedSignal) {
			this.connectedSignal = connectedSignal;
		}
		
        public void process(WatchedEvent event) {  
        	if (event.getState() == KeeperState.SyncConnected) {  
                connectedSignal.countDown();  
            } 
        }  
    }

/*
	
			        System.out.println("/server/"+hostname);
			        zk.create("/server/"+hostname, ip.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
		
*/
	
	ZooKeeper connectServer() throws IOException {
		ZooKeeper zk = null;
		try {
			String host = System.getProperty("zk.address") + ":2181";
	        System.out.println("zk.address + " + host);
	        
			CountDownLatch connectedSignal = new CountDownLatch(1);
			zk = new ZooKeeper(host, SESSION_TIMEOUT, new ConnWatcher(connectedSignal));
	        connectedSignal.await();  
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return zk;
	}
	
	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		Lifecycle source = event.getLifecycle();
		if (source instanceof StandardServer) {
			if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
				try {
					zk = connectServer();
					String hostname = InetAddress.getLocalHost().getHostName();
					String ip = getEth0Address().getHostAddress();
					System.out.println("current IP address : " +ip);
			        System.out.println("current Hostname : " + hostname);
					System.out.println("/server/"+hostname);
				    zk.create("/server/"+hostname, ip.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
			
				} catch (Exception e) {
					e.printStackTrace();
				}
				/*
				monitorDaemon = new Thread(this);
				monitorDaemon.setDaemon(true);
				monitorDaemon.start();*/
			} else if (Lifecycle.AFTER_STOP_EVENT.equals(event.getType())) {
				try {
					zk.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}

