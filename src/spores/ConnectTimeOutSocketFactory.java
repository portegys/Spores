/*******************************************************************************
 * Copyright (c) 2000 Novell, Inc. All Rights Reserved.
 *
 * THIS WORK IS SUBJECT TO U.S. AND INTERNATIONAL COPYRIGHT LAWS AND
 * TREATIES. USE AND REDISTRIBUTION OF THIS WORK IS SUBJECT TO THE LICENSE
 * AGREEMENT ACCOMPANYING THE SOFTWARE DEVELOPMENT KIT (SDK) THAT CONTAINS
 * THIS WORK. PURSUANT TO THE SDK LICENSE AGREEMENT, NOVELL HEREBY GRANTS TO
 * DEVELOPER A ROYALTY-FREE, NON-EXCLUSIVE LICENSE TO INCLUDE NOVELL'S SAMPLE
 * CODE IN ITS PRODUCT. NOVELL GRANTS DEVELOPER WORLDWIDE DISTRIBUTION RIGHTS
 * TO MARKET, DISTRIBUTE, OR SELL NOVELL'S SAMPLE CODE AS A COMPONENT OF
 * DEVELOPER'S PRODUCTS. NOVELL SHALL HAVE NO OBLIGATIONS TO DEVELOPER OR
 * DEVELOPER'S CUSTOMERS WITH RESPECT TO THIS CODE.
 */

/**
 * This factory will return connected sockets with the added capability to
 * timeout if the sockets do not connect within a specified time limit. This
 * uses an additional thread to Connect.
 *
 * The socket may not connect for a large amount of time. Even
 * though the program will be able to continue processing and possibily
 * connect to another server, this additional thread will remain alive.
 * This is because the socket will remained blocked and the thread will
 * remain in memory until the socket quits.
 *
 * WARNING: Since every connection timeout may leave a thread lingering in
 * memory, a large amount of connection timeouts may eventually fill up
 * available space in memory.
 */
package spores;

import java.io.IOException;

import java.net.Socket;


public class ConnectTimeOutSocketFactory implements Runnable, Parameters {
   protected String      host;
   protected int         port;
   protected Socket      socket;
   protected IOException socketError;
   protected boolean     hasTimedOut;
   protected boolean     reuse;

   public ConnectTimeOutSocketFactory()
   {
      socket      = null;
      socketError = null;
      hasTimedOut = false;
      reuse       = false;
   }


   public Socket createSocket(String host, int port) throws IOException
   {
      // If not first use, use a new object.
      boolean first = false;

      synchronized (this) {
         if (!reuse)
         {
            first = true;
         }

         reuse = true;
      }

      if (!first)
      {
         ConnectTimeOutSocketFactory factory = new ConnectTimeOutSocketFactory();

         return(factory.createSocket(host, port));
      }

      this.host = host;
      this.port = port;

      Thread r = new Thread(this);
      r.setDaemon(true);   // If this is the last thread running, allow exit.
      r.start();

      try {
         r.join(SOCKET_TIME_OUT);
      }
      catch (java.lang.InterruptedException ie) {
         r.interrupt();
      }

      /* if an error occured creating the socket then throw the error */
      if (socketError != null)
      {
         throw socketError;
      }

      /* if the socket is null then the socket connect has not completed*/
      if (socket == null)
      {
         hasTimedOut = true;
         throw new IOException("Socket connection timed out: " + host + ":" +
                               port);

         /* at this point we leave the connect
          * thread to take care of itself */
      }

      return(socket);
   }


   /**
    * This thread will create and connect the socket.  The connection will
    * block until it is complete.  If an exception occurs it is saved for
    * the main program thread to pick up and throw.
    */
   public void run()
   {
      try {
         //this method will likely block until a connection is made
         socket = new Socket(host, port);
      }
      catch (IOException ioe) {
         this.socketError = ioe;
      }

      if (hasTimedOut)
      {
         try {
            if (socket != null)
            {
               socket.close();
            }
         }
         catch (IOException ioe) {
            // we don't care about this exception
         }

         socket      = null;
         socketError = null;
      }
   }
}
