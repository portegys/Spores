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
 * Server.
 */
public class Server extends Thread implements Parameters {
   private Controls     controls;
   private P2P          p2p;
   private Transfer     transfer;
   private Utils        utils;
   private ServerSocket socket;

   // Constructor.
   public Server(Controls controls)
   {
      this.controls = controls;
      p2p           = controls.p2p;
      transfer      = controls.transfer;
      utils         = controls.utils;

      // Create the server socket.
      socket = null;

      try {
         socket = utils.createServerSocket(p2p.localPort);
      }
      catch (Exception e) {
         String msgString = new String("Cannot create server socket: " +
                                       e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
   }


   // Dummy constructor.
   public Server()
   {
   }


   /**
    * Server main loop.
    */
   public void run()
   {
      try {
         while ((Thread.currentThread() == this) && (socket != null) &&
                !isInterrupted())
         {
            // Spawn peer server thread.
            PeerServer peerServer = new PeerServer(utils.acceptSocket(
                                                      socket));
            peerServer.start();
         }
      }
      catch (IOException e) {
         String msgString = new String("Server socket error: " +
                                       e.toString());
         Log.getLog().logWarning(msgString);
         controls.statusText.setText(msgString);
      }
      finally {
         try {
            if (socket != null)
            {
               utils.closeServerSocket(socket);
            }
         }
         catch (Exception e) {
         }
      }
   }


   /**
    * Peer server.
    */
   class PeerServer extends Thread implements Parameters {
      Socket peerSocket;
      String peerHost;
      int    peerPort;

      // Constructor.
      PeerServer(Socket peerSocket)
      {
         this.peerSocket = peerSocket;
         peerHost        = utils.getRemoteAddress(peerSocket);
      }


      // Talk to client peer.
      public void run()
      {
         boolean validRequest;
         int     size;

         if (Thread.currentThread() != this)
         {
            return;
         }

         // Get request.
         try {
            DataInputStream in      = utils.getDataInputStream(peerSocket);
            String          version = utils.readLineFromStream(in);
            String          s       = utils.readLineFromStream(in);
            peerPort = SPORES_PORT;

            try {
               peerPort = Integer.parseInt(s);
            }
            catch (NumberFormatException e) {
            }

            String request = utils.readLineFromStream(in);
            validRequest = true;
            if (request.startsWith(PEER_REQUEST))
            {
               peerRequest();
            }
            else if (request.startsWith(SEARCH_REQUEST))
            {
               searchRequest(in);
            }
            else if (request.startsWith(DOWNLOAD_REQUEST))
            {
               downloadRequest(in);
            }
            else if (request.startsWith(UPLOAD_REQUEST))
            {
               uploadRequest(in);
            }
            else if (request.startsWith(CONFIRMATION_REQUEST))
            {
               confirmationRequest();
            }
            else
            {
               validRequest = false;
            }

            if (validRequest)
            {
               // Add peer to connected list, possibly replacing
               // random peer to keep list dynamically changing.
               if (!(p2p.localHost.equals(peerHost) &&
                     (p2p.localPort == peerPort)) &&
                   !p2p.connectedPeers.isDuplicate(peerHost, peerPort))
               {
                  while ((size = p2p.connectedPeers.size()) >= MAX_CONNECTED_PEERS)
                  {
                     try {
                        p2p.connectedPeers.remove(controls.random.nextInt(
                                                     size));
                     }
                     catch (IndexOutOfBoundsException e) {
                     }
                  }

                  p2p.connectedPeers.addPeer(peerHost, peerPort);
                  p2p.refreshConnections();
               }
            }
            else
            {
               String msgString = new String("Invalid request " + request +
                                             " from " + peerHost);
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }
         }
         catch (InterruptedIOException e) {
            String msgString = new String("Time-out serving peer " +
                                          peerHost);
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
         catch (IOException e) {
            String msgString = new String(
               "Error receiving request from peer " + peerHost + ": " +
               e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
         finally {
            try {
               if (peerSocket != null)
               {
                  utils.closeSocket(peerSocket);
               }
            }
            catch (Exception e) {
               String msgString = new String(
                  "Error closing socket to peer " + peerHost + ": " +
                  e.toString());
               Log.getLog().logWarning(msgString);
               controls.statusText.setText(msgString);
            }
         }
      }


      /**
       * Send peer list to peer.
       */
      void peerRequest()
      {
         P2P.PeerAddress address;

         try {
            DataOutputStream out       = utils.getDataOutputStream(peerSocket);
            String           peerList  = "";
            long             peerCount = 0;

            for (int i = 0;
                 (i < p2p.connectedPeers.size()) &&
                 (peerCount < MAX_TRANSMITTED_PEERS); i++)
            {
               try {
                  address  = (P2P.PeerAddress)p2p.connectedPeers.get(i);
                  peerList = peerList.concat(address.host + ":" +
                                             address.port + "\n");
                  peerCount++;
               }
               catch (IndexOutOfBoundsException e) {
               }
            }

            utils.writeLongToStream(out, peerCount);

            byte[] data = peerList.getBytes();
            utils.writeBytesToStream(out, data, data.length);
         }
         catch (IOException e) {
            String msgString = new String(
               "Server error sending peers to peer " + peerHost +
               ": " + e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
      }


      /**
       * Search for file.
       */
      void searchRequest(DataInputStream in)
      {
         byte    response    = NEGATIVE_RESPONSE;
         String  searchFile  = null;
         String  searchType  = null;
         String  searchCode  = null;
         long    searchDepth = 0;
         long    copies      = -1;
         long    copiesFound = 0;
         long    searchCopies;
         String  searchID    = null;
         boolean firstSearch = false;

         P2P.PeerList            connectedPeers;
         P2P.PeerAddress         peerAddress;
         PeerSearch              peerSearch;
         PeerSearch.SearchThread searchResult;
         String targetName = null;
         String targetPeer = null;
         long   targetPort = SPORES_PORT;
         long   targetSize = 0;

         try {
            DataOutputStream out = utils.getDataOutputStream(peerSocket);

            if (controls.sharingEnabled)
            {
               searchFile = utils.readLineFromStream(in);

               if (!searchFile.equals("") &&
                   (searchFile.indexOf(File.separator) == -1))
               {
                  searchType = utils.readLineFromStream(in);

                  if (searchType.equals("file") ||
                      searchType.equals("folder"))
                  {
                     searchCode = utils.readLineFromStream(in);
                     searchID   = utils.readLineFromStream(in);

                     String s = utils.readLineFromStream(in);
                     copies = 0;

                     try {
                        copies = Long.parseLong(s);
                     }
                     catch (NumberFormatException e) {
                     }

                     s           = utils.readLineFromStream(in);
                     searchDepth = 0;

                     try {
                        searchDepth = Long.parseLong(s);
                     }
                     catch (NumberFormatException e) {
                     }

                     if (searchDepth > MAX_SEARCH_DEPTH)
                     {
                        searchDepth = MAX_SEARCH_DEPTH;
                     }

                     searchDepth--;

                     // Prevent search looping by checking and storing
                     // search request ID.
                     if (controls.searchCache.get(searchID) == null)
                     {
                        controls.searchCache.put(searchID,
                                                 new Boolean(true));
                        firstSearch = true;
                     }
                     else
                     {
                        searchDepth = 0;
                     }

                     if (firstSearch)
                     {
                        // Searching solely on code?
                        if (searchFile.equals("*") &&
                            !searchCode.equals(""))
                        {
                           // Check all shared files for matching code.
                           File dir = new File(controls.sharedFolder);
                           String[] fileNames = dir.list();

                           for (int i = 0; i < fileNames.length;
                                i++)
                           {
                              String fileName = controls.sharedFolder +
                                                File.separator + fileNames[i];
                              File file = new File(fileName);

                              if (file.exists() && file.canRead())
                              {
                                 if ((searchType.equals("file") &&
                                      file.isFile()) ||
                                     (searchType.equals("folder") &&
                                      file.isDirectory()))
                                 {
                                    if (searchCode.equals(
                                           Utils.getMD5ForFile(
                                              fileName)))
                                    {
                                       // File found here.
                                       response   = POSITIVE_RESPONSE;
                                       targetName = file.getName();
                                       targetPeer = p2p.localHost;
                                       targetPort = p2p.localPort;
                                       targetSize = Utils.getFileSize(fileName);
                                       copiesFound++;

                                       break;
                                    }
                                 }
                              }
                           }
                        }
                        else
                        {
                           String fileName = controls.sharedFolder +
                                             File.separator + searchFile;
                           File file = new File(fileName);

                           if (file.exists() && file.canRead())
                           {
                              if ((searchType.equals("file") &&
                                   file.isFile()) ||
                                  (searchType.equals("folder") &&
                                   file.isDirectory()))
                              {
                                 if (searchCode.equals("") ||
                                     searchCode.equals(
                                        Utils.getMD5ForFile(
                                           fileName)))
                                 {
                                    // File found here.
                                    response   = POSITIVE_RESPONSE;
                                    targetName = searchFile;
                                    targetPeer = p2p.localHost;
                                    targetPort = p2p.localPort;
                                    targetSize = Utils.getFileSize(fileName);
                                    copiesFound++;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            // Search my connections?
            if (copies == -1)
            {
               searchCopies = -1;
            }
            else
            {
               searchCopies = copies - copiesFound;

               if (searchCopies < 0)
               {
                  searchCopies = 0;
               }
            }

            if ((searchDepth > 0) &&
                ((searchCopies == -1) || (searchCopies > 0)))
            {
               connectedPeers = p2p.connectedPeers.copy();

               for (int i = 0; i < connectedPeers.size(); i++)
               {
                  peerAddress = (P2P.PeerAddress)connectedPeers.get(i);

                  if (peerHost.equals(peerAddress.host))
                  {
                     connectedPeers.remove(peerAddress);

                     break;
                  }
               }

               peerSearch = new PeerSearch(utils, connectedPeers,
                                           p2p.localPort, searchFile, searchType, searchCode,
                                           searchID, searchCopies, searchDepth);
               peerSearch.run();

               // Accumulate search results.
               for (int i = 0; i < peerSearch.searchThreads.length; i++)
               {
                  searchResult = peerSearch.searchThreads[i];

                  if (searchResult.result == PeerSearch.SUCCESS)
                  {
                     response = POSITIVE_RESPONSE;

                     // Accumulate copies found.
                     copiesFound += searchResult.copiesFound;

                     // Save peer containing file.
                     if (targetPeer == null)
                     {
                        targetName = searchResult.targetName;
                        targetPeer = searchResult.targetPeer;
                        targetPort = searchResult.targetPort;
                        targetSize = searchResult.targetSize;
                     }
                  }
               }
            }

            // Send response.
            utils.writeByteToStream(out, response);

            if (response == POSITIVE_RESPONSE)
            {
               utils.writeLineToStream(out, targetName);
               utils.writeLineToStream(out, targetPeer);
               utils.writeLongToStream(out, targetPort);
               utils.writeLongToStream(out, targetSize);
               utils.writeLongToStream(out, copiesFound);
            }
         }
         catch (IOException e) {
            String msgString = new String(
               "Server error sending search response to peer " +
               peerHost + ": " + e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }

         // If put search ID in cache, remove it.
         if (firstSearch)
         {
            controls.searchCache.remove(searchID);
         }
      }


      /**
       * Download file to peer.
       */
      void downloadRequest(DataInputStream in)
      {
         try {
            DataOutputStream out      = utils.getDataOutputStream(peerSocket);
            byte             response = NEGATIVE_RESPONSE;
            String           fileName = null;

            if (controls.sharingEnabled)
            {
               fileName = utils.readLineFromStream(in);

               if (!fileName.equals("") &&
                   (fileName.indexOf(File.separator) == -1))
               {
                  String fileType = utils.readLineFromStream(in);

                  if (fileType.equals("file") ||
                      fileType.equals("folder"))
                  {
                     String searchCode = utils.readLineFromStream(in);
                     fileName = controls.sharedFolder + File.separator +
                                fileName;

                     File file = new File(fileName);

                     if (file.exists() && file.canRead())
                     {
                        if ((fileType.equals("file") && file.isFile()) ||
                            (fileType.equals("folder") &&
                             file.isDirectory()))
                        {
                           if (searchCode.equals("") ||
                               searchCode.equals(
                                  Utils.getMD5ForFile(fileName)))
                           {
                              // Got file.
                              response = POSITIVE_RESPONSE;
                           }
                        }
                     }
                  }
               }
            }

            utils.writeByteToStream(out, response);

            if (response == POSITIVE_RESPONSE)
            {
               transfer.putFile(out, fileName, null, null, null);
               Log.getLog().logInformation("Server downloaded file " +
                                           fileName);
            }
         }
         catch (IOException e) {
            String msgString = new String(
               "Server error downloading to peer " + peerHost + ": " +
               e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
      }


      /**
       * Upload file from peer.
       */
      void uploadRequest(DataInputStream in)
      {
         String sharedFile = null;
         String tempFile   = null;

         try {
            DataOutputStream out = utils.getDataOutputStream(peerSocket);
            byte             response;
            String           fileName = utils.readLineFromStream(in);

            if (!fileName.equals("") &&
                (fileName.indexOf(File.separator) == -1))
            {
               String fileType = utils.readLineFromStream(in);

               if (fileType.equals("file") || fileType.equals("folder"))
               {
                  sharedFile = controls.sharedFolder + File.separator +
                               fileName;

                  File file = new File(sharedFile);

                  // Check for various restrictions.
                  if (controls.sharingEnabled && !file.exists() &&
                      controls.properties.checkFile(fileName).equals(""))
                  {
                     // Upload file.
                     response = POSITIVE_RESPONSE;
                     utils.writeByteToStream(out, response);

                     DataInputStream up = utils.getDataInputStream(peerSocket);
                     File            temp;

                     for (int i = 0; ; i++)       // Pick a unique temp file name
                     {
                        tempFile = TRANSFER_FOLDER + File.separator +
                                   i + fileName;
                        temp = new File(tempFile);

                        if (!temp.exists())
                        {
                           break;
                        }
                     }

                     transfer.getFile(up, tempFile, null, null, null);

                     // Check size constraints.
                     String errmsg = controls.properties.checkFile(tempFile);

                     if (errmsg.equals(""))
                     {
                        // If not simultaneously uploading.
                        if (!file.exists())
                        {
                           // Copy to shared folder.
                           Utils.copyFile(tempFile, sharedFile);

                           // Refresh panels.
                           controls.sharedFiles.refresh();
                           controls.properties.refresh();
                        }

                        Log.getLog().logInformation("Server uploaded file " +
                                                    fileName);
                     }

                     Utils.deleteFile(tempFile);

                     return;
                  }
               }
            }

            response = NEGATIVE_RESPONSE;
            utils.writeByteToStream(out, response);
         }
         catch (IOException e) {
            if (sharedFile != null)
            {
               Utils.deleteFile(sharedFile);
            }

            if (tempFile != null)
            {
               Utils.deleteFile(tempFile);
            }

            String msgString = new String(
               "Server error uploading from peer " + peerHost + ": " +
               e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
      }


      /**
       * Send confirmation reply to peer.
       */
      void confirmationRequest()
      {
         try {
            DataOutputStream out      = utils.getDataOutputStream(peerSocket);
            byte             response = POSITIVE_RESPONSE;
            utils.writeByteToStream(out, response);
         }
         catch (IOException e) {
            String msgString = new String(
               "Server error sending confirmation response to peer " +
               peerHost + ": " + e.toString());
            Log.getLog().logWarning(msgString);
            controls.statusText.setText(msgString);
         }
      }
   }
}
