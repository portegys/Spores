/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 * Copyright (c) 2003 by Tom Portegys, All Rights Reserved.
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */
package spores;

import java.io.*;

import java.net.*;

import java.util.*;


/**
 * Peer-to-peer.
 */
public class P2P extends Thread implements Parameters {
   // Controls.
   private Controls controls;
   private Utils    utils;

   // Latest version.
   double version       = VERSION;
   String updateAddress = null;

   // Local host.
   String   localHost;
   int      localPort;
   PeerList connectedPeers;
   PeerList pendingPeers;

   // Constructor: cache peer addresses.
   public P2P(Controls controls, int port) throws Exception
   {
      String         peerAddress;
      String         webAddress;
      String         input;
      BufferedReader in;

      this.controls = controls;
      utils         = controls.utils;

      // Get local address.
      localHost = utils.getLocalAddress();
      localPort = port;

      // Create peer lists.
      connectedPeers = new PeerList();
      pendingPeers   = new PeerList();

      // Create web cache lists.
      WebCacheList cacheList              = new WebCacheList();
      WebCacheList cacheDiscoveryList     = new WebCacheList();
      WebCacheList cacheDiscoverySaveList = new WebCacheList();

      // Check web caches for newer version.
      // Display notice later.
      updateAddress = null;

      String fileName = WEB_CACHE_FILE;
      Log.getLog().logInformation("Checking web caches from file " +
                                  fileName + " for newer version");
      in = null;

      try {
         in = new BufferedReader(new FileReader(fileName));

         while (true)
         {
            webAddress = in.readLine();

            if (webAddress == null)
            {
               in.close();

               if (fileName.equals(WEB_CACHE_FILE))
               {
                  fileName = WEB_CACHE_DISCOVERY_FILE;
                  Log.getLog().logInformation("Checking web caches from file " +
                                              fileName + " for newer version");
                  in = new BufferedReader(new FileReader(fileName));

                  continue;
               }
               else
               {
                  in = null;

                  break;
               }
            }

            if (webAddress.equals(""))
            {
               continue;
            }

            BufferedReader reader = null;

            try {
               if (!webAddress.startsWith("http://"))
               {
                  webAddress = new String("http://" + webAddress);
               }

               Log.getLog().logInformation("Checking web cache " +
                                           webAddress);

               String s = webAddress.substring(0,
                                               webAddress.lastIndexOf("/"));
               URL               url            = new URL(s + "/version.html");
               URLConnection     urlConnection  = url.openConnection();
               HttpURLConnection httpConnection = (HttpURLConnection)urlConnection;
               int               code           = httpConnection.getResponseCode();

               if (code == HttpURLConnection.HTTP_OK)
               {
                  reader = new BufferedReader(new InputStreamReader(
                                                 urlConnection.getInputStream()));
                  input = reader.readLine();

                  if (input != null)
                  {
                     try {
                        double v = Double.parseDouble(input);

                        if (v > version)
                        {
                           version       = v;
                           updateAddress = s;
                        }
                     }
                     catch (NumberFormatException e) {
                     }
                  }
               }

               reader.close();
               reader = null;

               // If primary web cache, check master switch.
               if (!webAddress.startsWith("http://" + SPORES_WWW_0))
               {
                  continue;
               }

               if (!webAddress.startsWith("http://" + SPORES_WWW_1))
               {
                  continue;
               }

               s              = webAddress.substring(0, webAddress.lastIndexOf("/"));
               url            = new URL(s + "/master-switch.html");
               urlConnection  = url.openConnection();
               httpConnection = (HttpURLConnection)urlConnection;
               code           = httpConnection.getResponseCode();

               if (code == HttpURLConnection.HTTP_OK)
               {
                  reader = new BufferedReader(new InputStreamReader(
                                                 urlConnection.getInputStream()));
                  input = reader.readLine();

                  if (input != null)
                  {
                     if (input.startsWith("false"))
                     {
                        controls.sharingEnabled = false;

                        String msgString = new String(
                           "File sharing disabled by primary web cache " +
                           s);
                        Log.getLog().logInformation(msgString);
                     }
                  }
               }
            }
            catch (MalformedURLException e) {
               String msgString = new String("HTTP error for web cache " +
                                             webAddress + ": " + e.toString());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }
            catch (IOException e) {
               String msgString = new String("HTTP error for web cache " +
                                             webAddress + ": " + e.toString());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }
            finally {
               if (reader != null)
               {
                  reader.close();
               }
            }
         }
      }
      catch (Exception e) {
         String msgString = new String(
            "Error accessing web caches from file " + fileName + ": " +
            e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (in != null)
         {
            in.close();
         }
      }

      // Add static peers.
      Log.getLog().logInformation("Accessing peers from file " + PEER_FILE);
      in = null;

      try {
         in          = new BufferedReader(new FileReader(PEER_FILE));
         peerAddress = null;

         while (pendingPeers.size() < MAX_PENDING_PEERS)
         {
            peerAddress = in.readLine();

            if (peerAddress == null)
            {
               break;
            }

            controls.peerText.append(peerAddress + "\n");
            Log.getLog().logInformation("Adding peer " + peerAddress);
            pendingPeers.addPeer(peerAddress);
         }

         if (peerAddress != null)
         {
            peerAddress = in.readLine();

            if (peerAddress != null)
            {
               while (true)
               {
                  controls.peerText.append(peerAddress + "\n");
                  peerAddress = in.readLine();

                  if (peerAddress == null)
                  {
                     break;
                  }
               }

               String msgString = new String("Number of peers in file " +
                                             PEER_FILE + " exceeds maximum of " +
                                             MAX_PENDING_PEERS);
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }
         }
      }
      catch (Exception e) {
         String msgString = new String("Error adding peers from file " +
                                       PEER_FILE + ": " + e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (in != null)
         {
            in.close();
         }
      }

      // Read in web caches.
      Log.getLog().logInformation("Accessing web caches from file " +
                                  WEB_CACHE_FILE);
      in = null;

      try {
         in = new BufferedReader(new FileReader(WEB_CACHE_FILE));

         while (true)
         {
            webAddress = in.readLine();

            if (webAddress == null)
            {
               break;
            }

            if (webAddress.equals(""))
            {
               continue;
            }

            controls.webCacheText.append(webAddress + "\n");

            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            cacheList.addCache(new String(webAddress));
         }
      }
      catch (IOException e) {
         String msgString = new String("Error reading web cache " +
                                       WEB_CACHE_FILE + ": " + e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (in != null)
         {
            in.close();
         }
      }

      in = null;

      try {
         in = new BufferedReader(new FileReader(WEB_CACHE_DISCOVERY_FILE));

         while (true)
         {
            webAddress = in.readLine();

            if (webAddress == null)
            {
               break;
            }

            if (webAddress.equals(""))
            {
               continue;
            }

            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            cacheDiscoveryList.addCache(new String(webAddress));
         }

         in.close();
      }
      catch (IOException e) {
         String msgString = new String("Error reading web cache " +
                                       WEB_CACHE_DISCOVERY_FILE + ": " + e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (in != null)
         {
            in.close();
         }
      }

      // Add peers from randomly accessed web caches.
      int numCache;

      // Add peers from randomly accessed web caches.
      int     index;
      boolean discList;
      int     accessCount = 0;

      while ((pendingPeers.size() < MAX_PENDING_PEERS) &&
             ((numCache = cacheList.size() + cacheDiscoveryList.size()) > 0) &&
             (accessCount < 100))
      {
         accessCount++;
         index = controls.random.nextInt(numCache);

         if (index < cacheList.size())
         {
            webAddress = (String)cacheList.get(index);
            cacheList.remove(index);
            discList = false;
         }
         else
         {
            index     -= cacheList.size();
            webAddress = (String)cacheDiscoveryList.get(index);
            cacheDiscoveryList.remove(index);
            discList = true;
         }

         BufferedReader reader = null;

         try {
            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            Log.getLog().logInformation("Adding peers from web cache " +
                                        webAddress);

            URL url = new URL(webAddress + "?client=" + SPORES_VENDOR_CODE +
                              "&version=1&hostfile=1");
            URLConnection     urlConnection  = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection)urlConnection;
            int               code           = httpConnection.getResponseCode();

            if (code != HttpURLConnection.HTTP_OK)
            {
               String msgString = new String("Cannot access web cache " +
                                             webAddress + ": " +
                                             httpConnection.getResponseMessage());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);

               continue;
            }

            reader = new BufferedReader(new InputStreamReader(
                                           urlConnection.getInputStream()));

            while (pendingPeers.size() < MAX_PENDING_PEERS)
            {
               peerAddress = reader.readLine();

               if (peerAddress == null)
               {
                  break;
               }

               Log.getLog().logInformation("Adding peer " + peerAddress);
               pendingPeers.addPeer(peerAddress);
            }

            reader.close();
            reader = null;

            // Acquire additional web caches addresses.
            url = new URL(webAddress + "?client=" + SPORES_VENDOR_CODE +
                          "&version=1&urlfile=1");
            urlConnection  = url.openConnection();
            httpConnection = (HttpURLConnection)urlConnection;
            code           = httpConnection.getResponseCode();

            if (code != HttpURLConnection.HTTP_OK)
            {
               String msgString = new String(
                  "Cannot acquire additional URLs from web cache " +
                  webAddress + ": " +
                  httpConnection.getResponseMessage());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }

            reader = new BufferedReader(new InputStreamReader(
                                           urlConnection.getInputStream()));

            while (true)
            {
               input = reader.readLine();

               if (input == null)
               {
                  break;
               }

               if (!webAddress.equals(input) &&
                   !cacheList.isDuplicate(input) &&
                   !cacheDiscoverySaveList.isDuplicate(input))
               {
                  cacheDiscoveryList.addCache(new String(input));
               }
            }

            reader.close();
            reader = null;

            // Send my address to cache for other peers to see.
            String host = InetAddress.getLocalHost().getHostAddress();
            url = new URL(webAddress + "?client=" + SPORES_VENDOR_CODE +
                          "&version=1&ip=" + host + ":" + localPort);
            urlConnection  = url.openConnection();
            httpConnection = (HttpURLConnection)urlConnection;
            code           = httpConnection.getResponseCode();

            if (code != HttpURLConnection.HTTP_OK)
            {
               String msgString = new String("Cannot update web cache " +
                                             webAddress + ": " +
                                             httpConnection.getResponseMessage());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }

            reader = new BufferedReader(new InputStreamReader(
                                           urlConnection.getInputStream()));
            input = reader.readLine();

            if (input == null)
            {
               input = new String("<no response>");
            }

            if (!input.startsWith("OK"))
            {
               String msgString = new String("Cannot update web cache " +
                                             webAddress + ": " + input);
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }

            reader.close();
         }
         catch (MalformedURLException e) {
            String msgString = new String("HTTP error for web cache " +
                                          webAddress + ": " + e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);

            continue;
         }
         catch (IOException e) {
            String msgString = new String("HTTP error for web cache " +
                                          webAddress + ": " + e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);

            continue;
         }
         finally {
            if (reader != null)
            {
               reader.close();
            }
         }

         if (discList)
         {
            cacheDiscoverySaveList.addCache(new String(webAddress));
         }
      }

      // Update discovered web caches.
      for (numCache = cacheDiscoveryList.size(); numCache > 0; numCache--)
      {
         index      = controls.random.nextInt(numCache);
         webAddress = (String)cacheDiscoveryList.get(index);
         cacheDiscoveryList.remove(index);
         cacheDiscoverySaveList.addCache(new String(webAddress));
      }

      PrintWriter out = null;

      try {
         out = new PrintWriter(new BufferedWriter(
                                  new FileWriter(WEB_CACHE_DISCOVERY_FILE)));

         for (int i = 0;
              (i < cacheDiscoverySaveList.size()) &&
              (i < MAX_WEB_CACHE_DISCOVERED); i++)
         {
            webAddress = (String)cacheDiscoverySaveList.get(i);
            out.println(webAddress);
            controls.webCacheDiscText.append(webAddress + "\n");
         }
      }
      catch (IOException e) {
         String msgString = new String("Cannot save web cache URLs to file " +
                                       WEB_CACHE_DISCOVERY_FILE + ": " + e.toString());
         Log.getLog().logError(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         if (out != null)
         {
            out.close();
         }
      }
   }


   // Dummy constructor.
   public P2P()
   {
   }


   // Create peer list.
   PeerList createPeerList()
   {
      return(new PeerList());
   }


   /**
    * Peer connection maintenance.
    */
   public void run()
   {
      PeerAddress peerAddress;
      int         peerIndex = 0;
      int         timer     = 0;
      int         size;
      int         index;

      while ((Thread.currentThread() == this) && !isInterrupted())
      {
         // Set the timer for the next search.
         try {
            Thread.sleep(CONNECTION_CHECK_FREQ);
            timer += CONNECTION_CHECK_FREQ;
         }
         catch (InterruptedException e) {
            break;
         }

         // Need more connections?
         size = connectedPeers.size();

         if (size < MAX_CONNECTED_PEERS)
         {
            // Pending peer list empty?
            if (pendingPeers.size() == 0)
            {
               // Request pending peer list from a random peer.
               if (size > 0)
               {
                  peerAddress = null;

                  try {
                     index       = controls.random.nextInt(size);
                     peerAddress = (PeerAddress)connectedPeers.get(index);
                  }
                  catch (IndexOutOfBoundsException e) {
                  }

                  if (peerAddress != null)
                  {
                     pendingPeers = requestPeers(peerAddress);
                  }
               }
            }

            // Confirm a connection to a random pending peer.
            if ((size = pendingPeers.size()) > 0)
            {
               peerAddress = null;

               try {
                  index       = controls.random.nextInt(size);
                  peerAddress = (PeerAddress)pendingPeers.get(index);
                  pendingPeers.remove(index);
               }
               catch (IndexOutOfBoundsException e) {
               }

               if ((peerAddress != null) &&
                   !connectedPeers.isDuplicate(peerAddress))
               {
                  if (confirmPeer(peerAddress))
                  {
                     connectedPeers.addPeer(peerAddress);
                     refreshConnections();
                  }
               }
            }
         }

         // Periodic confirmation of connected peers.
         size = connectedPeers.size();

         if ((size > 0) && (timer >= CONFIRMATION_FREQ))
         {
            timer       = 0;
            peerIndex   = (peerIndex + 1) % size;
            peerAddress = null;

            try {
               peerAddress = (PeerAddress)connectedPeers.get(peerIndex);
            }
            catch (IndexOutOfBoundsException e) {
            }

            if (peerAddress != null)
            {
               if (!confirmPeer(peerAddress))
               {
                  // Remove peer.
                  connectedPeers.remove(peerAddress);
                  refreshConnections();
               }
            }
         }

         // If all connections lost, request peers from webcaches.
         if ((connectedPeers.size() == 0) && (pendingPeers.size() == 0))
         {
            requestWebCaches();
         }
      }
   }


   /**
    * Request peer addresses.
    */
   PeerList requestPeers(PeerAddress peerAddress)
   {
      Socket   socket;
      long     numPeers;
      PeerList peerList = new PeerList();

      // Create connection to peer.
      socket = null;

      try {
         socket = utils.createSocket(peerAddress.host, peerAddress.port);

         // Request peers.
         PrintWriter out = utils.getPrintWriter(socket);
         utils.printToStream(out,
                             Double.toString(VERSION) + "\n" + Integer.toString(localPort) +
                             "\n" + PEER_REQUEST);

         // Get peer list.
         DataInputStream in = utils.getDataInputStream(socket);
         numPeers = utils.readLongFromStream(in);

         for (int i = 0; (i < numPeers) && (i < MAX_TRANSMITTED_PEERS);
              i++)
         {
            String address = utils.readLineFromStream(in);

            if (address == null)
            {
               break;
            }

            if (!peerList.isDuplicate(address))
            {
               peerList.addPeer(address);
            }
         }
      }
      catch (Exception e) {
         String msgString = new String("Error requesting peers from " +
                                       peerAddress.host + ":" + peerAddress.port + ": " +
                                       e.toString());
         Log.getLog().logWarning(msgString);
      }
      finally {
         try {
            if (socket != null)
            {
               utils.closeSocket(socket);
            }
         }
         catch (Exception e) {
         }
      }

      return(peerList);
   }


   /**
    * Confirm peer connection.
    */
   boolean confirmPeer(PeerAddress peerAddress)
   {
      Socket socket;
      byte   response = NEGATIVE_RESPONSE;

      // Create connection to peer.
      socket = null;

      try {
         socket = utils.createSocket(peerAddress.host, peerAddress.port);

         // Send confirmation request.
         PrintWriter out = utils.getPrintWriter(socket);
         utils.printToStream(out,
                             Double.toString(VERSION) + "\n" + Integer.toString(localPort) +
                             "\n" + CONFIRMATION_REQUEST);

         // Get reply.
         DataInputStream in = utils.getDataInputStream(socket);
         response = utils.readByteFromStream(in);
      }
      catch (Exception e) {
         String msgString = new String("Error confirming peer " +
                                       peerAddress.host + ":" + peerAddress.port + ": " +
                                       e.toString());
         Log.getLog().logWarning(msgString);
      }
      finally {
         try {
            if (socket != null)
            {
               utils.closeSocket(socket);
            }
         }
         catch (Exception e) {
         }
      }

      if (response == NEGATIVE_RESPONSE)
      {
         return(false);
      }
      else
      {
         return(true);
      }
   }


   // Request peer addresses from web caches.
   void requestWebCaches()
   {
      WebCacheList cacheList          = new WebCacheList();
      WebCacheList cacheDiscoveryList = new WebCacheList();
      String       webAddress;

      // Read in web caches.
      BufferedReader in = null;

      try {
         in = new BufferedReader(new FileReader(WEB_CACHE_FILE));

         while (true)
         {
            webAddress = in.readLine();

            if (webAddress == null)
            {
               break;
            }

            if (webAddress.equals(""))
            {
               continue;
            }

            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            cacheList.addCache(new String(webAddress));
         }
      }
      catch (IOException e) {
         String msgString = new String("Error reading web cache " +
                                       WEB_CACHE_FILE + ": " + e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         try {
            if (in != null)
            {
               in.close();
            }
         }
         catch (Exception e) {
         }
      }

      in = null;

      try {
         in = new BufferedReader(new FileReader(WEB_CACHE_DISCOVERY_FILE));

         while (true)
         {
            webAddress = in.readLine();

            if (webAddress == null)
            {
               break;
            }

            if (webAddress.equals(""))
            {
               continue;
            }

            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            cacheDiscoveryList.addCache(new String(webAddress));
         }

         in.close();
      }
      catch (IOException e) {
         String msgString = new String("Error reading web cache " +
                                       WEB_CACHE_DISCOVERY_FILE + ": " + e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         try {
            if (in != null)
            {
               in.close();
            }
         }
         catch (Exception e) {
         }
      }

      // Read peer addresses.
      int numCache;

      // Read peer addresses.
      int    index;
      int    accessCount = 0;
      String peerAddress;

      while ((pendingPeers.size() < MAX_PENDING_PEERS) &&
             ((numCache = cacheList.size() + cacheDiscoveryList.size()) > 0) &&
             (accessCount < 100))
      {
         accessCount++;
         index = controls.random.nextInt(numCache);

         if (index < cacheList.size())
         {
            webAddress = (String)cacheList.get(index);
            cacheList.remove(index);
         }
         else
         {
            index     -= cacheList.size();
            webAddress = (String)cacheDiscoveryList.get(index);
            cacheDiscoveryList.remove(index);
         }

         BufferedReader reader = null;

         try {
            if (!webAddress.startsWith("http://"))
            {
               webAddress = new String("http://" + webAddress);
            }

            URL url = new URL(webAddress + "?client=" + SPORES_VENDOR_CODE +
                              "&version=1&hostfile=1");
            URLConnection     urlConnection  = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection)urlConnection;
            int               code           = httpConnection.getResponseCode();

            if (code != HttpURLConnection.HTTP_OK)
            {
               String msgString = new String("Cannot access web cache " +
                                             webAddress + ": " +
                                             httpConnection.getResponseMessage());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);

               continue;
            }

            reader = new BufferedReader(new InputStreamReader(
                                           urlConnection.getInputStream()));

            while (pendingPeers.size() < MAX_PENDING_PEERS)
            {
               peerAddress = reader.readLine();

               if (peerAddress == null)
               {
                  break;
               }

               pendingPeers.addPeer(peerAddress);
            }

            reader.close();
            reader = null;
         }
         catch (MalformedURLException e) {
            String msgString = new String("HTTP error for web cache " +
                                          webAddress + ": " + e.toString());
            Log.getLog().logWarning(msgString);
         }
         catch (IOException e) {
            String msgString = new String("HTTP error for web cache " +
                                          webAddress + ": " + e.toString());
            Log.getLog().logWarning(msgString);
         }
         finally {
            try {
               if (reader != null)
               {
                  reader.close();
               }
            }
            catch (Exception e) {
            }
         }
      }
   }


   // Refresh connections count.
   void refreshConnections()
   {
      controls.connectedText.setText(Integer.toString(connectedPeers.size()));
   }


   // Peer address.
   class PeerAddress {
      String host;
      int    port;

      PeerAddress(String host, int port)
      {
         this.host = host;
         this.port = port;
      }


      boolean equals(PeerAddress address)
      {
         if (host.equals(address.host) && (port == address.port))
         {
            return(true);
         }

         return(false);
      }
   }

   // Peer lists.
   class PeerList extends LinkedList {
      PeerList()
      {
         super();
      }


      // Add peer address if valid.
      boolean addPeer(String address)
      {
         String host;
         int    port;

         int i = address.indexOf(":");

         if (i == -1)
         {
            host = address;
            port = SPORES_PORT;
         }
         else
         {
            host = address.substring(0, i);

            try {
               port = Integer.parseInt(address.substring(i + 1,
                                                         address.length()));
            }
            catch (NumberFormatException e) {
               return(false);
            }
         }

         return(addPeer(host, port));
      }


      boolean addPeer(String host, int port)
      {
         PeerAddress address = new PeerAddress(host, port);

         return(addPeer(address));
      }


      synchronized boolean addPeer(PeerAddress address)
      {
         if (address.host.equals("") || (address.port < 0))
         {
            return(false);
         }

         if (address.host.equals(localHost) && (address.port == localPort))
         {
            return(false);
         }

         if (isDuplicate(address))
         {
            return(false);
         }

         addLast(address);

         return(true);
      }


      // Duplicate?
      boolean isDuplicate(String address)
      {
         String host;
         int    port;

         int i = address.indexOf(":");

         if (i == -1)
         {
            host = address;
            port = SPORES_PORT;
         }
         else
         {
            host = address.substring(0, i);

            try {
               port = Integer.parseInt(address.substring(i + 1,
                                                         address.length()));
            }
            catch (NumberFormatException e) {
               return(true);
            }
         }

         return(isDuplicate(host, port));
      }


      boolean isDuplicate(String host, int port)
      {
         PeerAddress address;

         for (int i = 0; i < size(); i++)
         {
            // List may be shrinking.
            try {
               address = (PeerAddress)get(i);

               if (address.host.equals(host) && (address.port == port))
               {
                  return(true);
               }
            }
            catch (IndexOutOfBoundsException e) {
            }
         }

         return(false);
      }


      boolean isDuplicate(PeerAddress address)
      {
         return(isDuplicate(address.host, address.port));
      }


      // Create a copy of the list.
      PeerList copy()
      {
         PeerList    newList = new PeerList();
         PeerAddress peerAddress;

         for (int i = 0; i < size(); i++)
         {
            peerAddress = null;

            try {
               peerAddress = (PeerAddress)get(i);
            }
            catch (IndexOutOfBoundsException e) {
            }

            if (peerAddress == null)
            {
               continue;
            }

            newList.addPeer(peerAddress.host, peerAddress.port);
         }

         return(newList);
      }
   }

   // Web cache lists.
   class WebCacheList extends LinkedList {
      WebCacheList()
      {
         super();
      }


      // Add cache if valid.
      synchronized boolean addCache(String cacheAddress)
      {
         String address;

         if (cacheAddress.equals(""))
         {
            return(false);
         }

         if (isDuplicate(cacheAddress))
         {
            return(false);
         }

         addLast(cacheAddress);

         return(true);
      }


      // Duplicate?
      boolean isDuplicate(String cacheAddress)
      {
         String address;

         for (int i = 0; i < size(); i++)
         {
            address = (String)get(i);

            if (address.equals(cacheAddress))
            {
               return(true);
            }
         }

         return(false);
      }
   }
}
