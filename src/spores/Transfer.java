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

import java.awt.*;

import java.io.*;

import java.math.*;

import java.net.*;

import java.util.*;

import javax.swing.*;


/**
 * File searches and transfers.
 */
public class Transfer implements Parameters, Runnable {
   // Transfer/search operation thread.
   static Thread  transferThread    = null;
   static boolean transferInterrupt = false;

   // GUI controls and connections.
   private Controls controls;
   private P2P      p2p;
   private Utils    utils;

   // File folder.
   File transferFolder;

   // Operation.
   int operation = NOP;

   // Constructor.
   public Transfer(Controls controls)
   {
      this.controls = controls;
      p2p           = controls.p2p;
      utils         = controls.utils;

      // Create folder.
      transferFolder = new File(TRANSFER_FOLDER);

      if (!transferFolder.exists())
      {
         transferFolder.mkdir();
      }
   }


   // Dummy constructor.
   public Transfer()
   {
   }


   // Start/stop operation.
   void operate(boolean start)
   {
      // Stop operation?
      if (!start)
      {
         // Ongoing operation?
         if (transferThread != null)
         {
            switch (operation)
            {
            case SEARCH:
               controls.transferResultsText.append(
                  "Stopping file search...\n");

               break;

            case DOWNLOAD:
               controls.transferResultsText.append(
                  "Stopping file download...\n");

               break;

            case UPLOAD:
               controls.transferResultsText.append(
                  "Stopping file upload...\n");

               break;
            }

            // Interrrupt thread and wait for termination.
            transferInterrupt = true;

            while ((transferThread != null) && transferThread.isAlive())
            {
               try {
                  Thread.sleep(10);
               }
               catch (InterruptedException e) {
                  break;
               }
            }

            operation         = NOP;
            transferThread    = null;
            transferInterrupt = false;
            controls.transferResultsText.append("Stopped\n");
         }

         return;
      }

      // Sharing disabled?
      if (!controls.sharingEnabled)
      {
         controls.transferResultsText.append("Sharing disabled!\n");
         controls.transferOperationSwitch.setSelected(false);
         controls.transferOperationSwitch.setText("Start");

         return;
      }

      // Start selected operation.
      operation         = controls.transferOperationChoice.getSelectedIndex();
      transferInterrupt = false;
      transferThread    = new Thread(this);
      transferThread.setDaemon(true);
      transferThread.start();
   }


   // Search.
   private void search()
   {
      Thread myThread;
      String searchFile;
      String searchType;
      int    copies;
      int    copiesFound;
      String searchID;

      P2P.PeerList            connectedPeers;
      PeerSearch              peerSearch;
      PeerSearch.SearchThread searchResult;
      String targetPeer = null;
      int    targetPort = SPORES_PORT;
      long   targetSize = 0;

      // Save search info.
      if ((myThread = transferThread) == null)
      {
         return;
      }

      searchFile = controls.currentFileText.getText().trim();

      if (!searchFile.equals(""))
      {
         searchFile = new File(searchFile).getName();
      }

      if (controls.currentTypeChoice.getSelectedIndex() == FILE)
      {
         searchType = "file";
      }
      else
      {
         searchType = "folder";
      }

      String searchCode = controls.currentUniquenessText.getText().trim();

      if (searchFile.equals(""))
      {
         if (searchCode.equals(""))
         {
            return;
         }

         searchFile = "*";
      }

      String s = controls.transferCopiesText.getText().trim();

      if (s.equals(""))
      {
         copies = -1;    // search all.
      }
      else
      {
         try {
            copies = Integer.parseInt(s);

            if (copies <= 0)
            {
               controls.transferResultsText.append("Invalid copies\n");

               return;
            }
         }
         catch (NumberFormatException e) {
            controls.transferResultsText.append("Invalid copies\n");

            return;
         }
      }

      copiesFound = 0;

      // No connections?
      if (p2p.connectedPeers.size() == 0)
      {
         controls.transferResultsText.append("No connections!\n");

         return;
      }

      // Compute a unique search ID to prevent network looping.
      searchID = Utils.getMD5ForString(p2p.localHost + searchFile +
                                       searchType + searchCode);
      controls.searchCache.put(searchID, new Boolean(true));

      // Start network search.
      Log.getLog().logInformation("Searching for " + searchType + " " +
                                  searchFile);
      controls.transferResultsText.append("Searching for " + searchType +
                                          " " + searchFile + "\n");
      connectedPeers = p2p.connectedPeers.copy();
      peerSearch     = new PeerSearch(utils, connectedPeers, p2p.localPort,
                                      searchFile, searchType, searchCode, searchID, copies,
                                      MAX_SEARCH_DEPTH);
      peerSearch.start();

      // Wait for search completion or time-out.
      for (int i = 0; i < SEARCH_TIME_OUT; i += 100)
      {
         if (!peerSearch.isAlive())
         {
            break;
         }

         // Check for interrupt.
         if ((myThread == transferThread) && transferInterrupt)
         {
            break;
         }

         // Sharing disabled?
         if (!controls.sharingEnabled)
         {
            controls.transferResultsText.append("Sharing disabled!\n");

            break;
         }

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }

      // Accumulate search results.
      for (int i = 0; i < peerSearch.searchThreads.length; i++)
      {
         searchResult = peerSearch.searchThreads[i];

         if (searchResult.result == PeerSearch.SUCCESS)
         {
            // Accumulate copies found.
            copiesFound += searchResult.copiesFound;

            // Save peer containing file.
            if ((p2p.localHost.equals(searchResult.targetPeer) &&
                 (p2p.localPort == searchResult.targetPort)) ||
                p2p.connectedPeers.isDuplicate(
                   searchResult.targetPeer,
                   (int)searchResult.targetPort))
            {
               // File currently accessible from this or connected peer.
               targetPeer = null;
            }
            else
            {
               targetPeer = searchResult.targetPeer;
               targetPort = (int)searchResult.targetPort;
            }

            targetSize = searchResult.targetSize;
         }
      }

      // Save connection to peer containing file to facilitate future transfers.
      if (targetPeer != null)
      {
         int size;

         while ((size = p2p.connectedPeers.size()) >= MAX_CONNECTED_PEERS)
         {
            try {
               p2p.connectedPeers.remove(controls.random.nextInt(size));
            }
            catch (IndexOutOfBoundsException e) {
            }
         }

         p2p.connectedPeers.addPeer(targetPeer, targetPort);
      }

      // Remove search cache entry.
      controls.searchCache.remove(searchID);

      if (copiesFound > 0)
      {
         Log.getLog().logInformation("Search completed: " + copiesFound +
                                     " copies found, size=" + (targetSize / 1000) + " Kbytes");
         controls.transferResultsText.append("Search completed: " +
                                             copiesFound + " copies found, size=" + (targetSize / 1000) +
                                             " Kbytes\n");
      }
      else
      {
         Log.getLog().logInformation("Search completed: " + searchType +
                                     " not found");
         controls.transferResultsText.append("Search completed: " +
                                             searchType + " not found\n");
      }
   }


   // Download.
   private void download()
   {
      Thread myThread;
      String downloadFile;
      String downloadType;
      String fileType;
      String tempFile;
      String sharedFile;
      File   file;
      String searchID;

      P2P.PeerList            connectedPeers;
      PeerSearch              peerSearch;
      PeerSearch.SearchThread searchResult;
      String  targetName = null;
      String  targetPeer = null;
      int     targetPort = SPORES_PORT;
      long    targetSize = 0;
      boolean done;
      boolean success;
      Socket  socket;

      // Save download info.
      if ((myThread = transferThread) == null)
      {
         return;
      }

      downloadFile = controls.currentFileText.getText().trim();

      if (!downloadFile.equals(""))
      {
         downloadFile = new File(downloadFile).getName();
      }

      if (controls.currentTypeChoice.getSelectedIndex() == FILE)
      {
         downloadType = "file";
         fileType     = "File";
      }
      else
      {
         downloadType = "folder";
         fileType     = "Folder";
      }

      String downloadCode = controls.currentUniquenessText.getText().trim();

      if (downloadFile.equals(""))
      {
         if (downloadCode.equals(""))
         {
            return;
         }

         downloadFile = "*";
      }

      sharedFile = controls.sharedFolder + File.separator + downloadFile;

      // Prevent overwrite.
      if (!downloadFile.equals("*") && new File(sharedFile).exists())
      {
         controls.transferResultsText.append("Cannot overwrite " +
                                             downloadType + " " + downloadFile + "\n");

         return;
      }

      // Download prohibited?
      String errmsg = "";

      if (!downloadFile.equals("*"))
      {
         errmsg = controls.properties.checkFile(downloadFile);

         if (!errmsg.equals(""))
         {
            controls.transferResultsText.append("Cannot download " +
                                                downloadType + " " + downloadFile + ": " + errmsg + "\n");

            return;
         }
      }

      // No connections?
      if (p2p.connectedPeers.size() == 0)
      {
         controls.transferResultsText.append("No connections!\n");

         return;
      }

      // Compute a unique search ID to prevent network looping.
      searchID = Utils.getMD5ForString(p2p.localHost + downloadFile +
                                       fileType + downloadCode);
      controls.searchCache.put(searchID, new Boolean(true));

      // Search for file.
      Log.getLog().logInformation("Attempting download of " + downloadType +
                                  " " + downloadFile);
      controls.transferResultsText.append("Attempting download of " +
                                          downloadType + " " + downloadFile + "\n");
      controls.transferProgressBar.setMinimum(0);
      done           = success = false;
      connectedPeers = p2p.connectedPeers.copy();
      peerSearch     = new PeerSearch(utils, connectedPeers, p2p.localPort,
                                      downloadFile, downloadType, downloadCode, searchID, 1,
                                      MAX_SEARCH_DEPTH);
      peerSearch.start();

      // Wait for search completion or time-out.
      for (int i = 0; i < SEARCH_TIME_OUT; i += 100)
      {
         if (!peerSearch.isAlive())
         {
            break;
         }

         // Check for interrupt.
         if ((myThread == transferThread) && transferInterrupt)
         {
            break;
         }

         // Sharing disabled?
         if (!controls.sharingEnabled)
         {
            controls.transferResultsText.append("Sharing disabled!\n");

            break;
         }

         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }

      // Check search results.
      done = success = false;

      for (int i = 0; (i < peerSearch.searchThreads.length) && !done; i++)
      {
         searchResult = peerSearch.searchThreads[i];

         if (searchResult.result == PeerSearch.SUCCESS)
         {
            targetName = searchResult.targetName;
            targetPeer = searchResult.targetPeer;
            targetPort = (int)searchResult.targetPort;
         }

         if (targetPeer == null)
         {
            continue;
         }

         // Check search result file name.
         tempFile   = TRANSFER_FOLDER + File.separator + targetName;
         sharedFile = controls.sharedFolder + File.separator + targetName;

         // Prevent overwrite.
         if (new File(sharedFile).exists())
         {
            controls.transferResultsText.append("Cannot overwrite " +
                                                downloadType + " " + targetName + ", continuing...\n");

            continue;
         }

         // Download prohibited?
         errmsg = controls.properties.checkFile(targetName);

         if (!errmsg.equals(""))
         {
            controls.transferResultsText.append("Cannot download " +
                                                downloadType + " " + downloadFile + ": " + errmsg +
                                                ", continuing...\n");

            continue;
         }

         // Found file: attempt download from peer.
         socket = null;

         try {
            socket = utils.createSocket(targetPeer, targetPort);

            // Request download.
            PrintWriter out = utils.getPrintWriter(socket);
            utils.printToStream(out,
                                Double.toString(VERSION) + "\n" +
                                Integer.toString(p2p.localPort) + "\n" + DOWNLOAD_REQUEST +
                                "\n" + targetName + "\n" + downloadType + "\n" +
                                downloadCode);

            // Get response.
            DataInputStream in       = utils.getDataInputStream(socket);
            byte            response = utils.readByteFromStream(in);

            if (response == POSITIVE_RESPONSE)
            {
               // Download file.
               Log.getLog().logInformation("Downloading from " +
                                           targetPeer + "...");
               controls.transferResultsText.append("Downloading from " +
                                                   targetPeer + "...\n");
               getFile(in, tempFile, myThread, controls.currentSizeText,
                       controls.transferProgressBar);

               // File matches requested type?
               file = new File(tempFile);

               if ((file.exists() && file.isDirectory() &&
                    downloadType.equals("folder")) ||
                   (file.exists() && file.isFile() &&
                    downloadType.equals("file")))
               {
                  // File matches requested code?
                  if (downloadCode.equals("") ||
                      downloadCode.equals(Utils.getMD5ForFile(
                                             tempFile)))
                  {
                     done = true;

                     // Check size constraints.
                     errmsg = controls.properties.checkFile(tempFile);

                     if (errmsg.equals(""))
                     {
                        // Copy to shared folder.
                        Utils.copyFile(tempFile, sharedFile);

                        // Refresh shared files panel.
                        controls.sharedFiles.refresh();

                        // Update/refresh panels.
                        controls.currentUniquenessText.setText(Utils.getMD5ForFile(
                                                                  tempFile));

                        if (file.isDirectory())
                        {
                           controls.currentTypeChoice.setSelectedIndex(FOLDER);
                        }
                        else
                        {
                           controls.currentTypeChoice.setSelectedIndex(FILE);
                        }

                        controls.currentSizeText.setText(Long.toString(
                                                            Utils.getFileSize(tempFile) / 1000));
                        controls.sharedFiles.refresh();
                        controls.properties.refresh();
                        success = true;
                     }
                     else
                     {
                        Log.getLog().logInformation("Cannot store " +
                                                    downloadType + ": " + errmsg);
                        controls.transferResultsText.append(
                           "Cannot store " + downloadType + ": " +
                           errmsg + "\n");
                     }
                  }
                  else
                  {
                     Log.getLog().logInformation(fileType +
                                                 " has invalid uniqueness code: continuing...");
                     controls.transferResultsText.append(fileType +
                                                         " has invalid uniqueness code: continuing...\n");
                  }
               }
               else
               {
                  Log.getLog().logInformation(fileType +
                                              " has invalid type: continuing...");
                  controls.transferResultsText.append(fileType +
                                                      " has invalid type: continuing...\n");
               }

               // Remove temp file.
               Utils.deleteFile(tempFile);
            }
         }
         catch (Exception e) {
            // Remove temporary file.
            Utils.deleteFile(tempFile);

            String msgString = new String("Error downloading " +
                                          downloadType + " " + downloadFile + " from " +
                                          targetPeer + ":" + e.toString());
            Log.getLog().logWarning(msgString);
            controls.transferResultsText.append(msgString + "\n");
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
      }

      // Save connection to peer containing file to facilitate future transfers.
      if ((targetPeer != null) &&
          !(p2p.localHost.equals(targetPeer) &&
            (p2p.localPort == targetPort)) &&
          !p2p.connectedPeers.isDuplicate(targetPeer, targetPort))
      {
         int size;

         while ((size = p2p.connectedPeers.size()) >= MAX_CONNECTED_PEERS)
         {
            try {
               p2p.connectedPeers.remove(controls.random.nextInt(size));
            }
            catch (IndexOutOfBoundsException e) {
            }
         }

         p2p.connectedPeers.addPeer(targetPeer, targetPort);
      }

      // Remove search cache entry.
      controls.searchCache.remove(searchID);

      if ((myThread == transferThread) && transferInterrupt)
      {
         return;
      }

      if (success)
      {
         Log.getLog().logInformation("Download of " + targetName +
                                     " completed");
         controls.transferResultsText.append("Download of " + targetName +
                                             " completed\n");
         controls.currentFileText.setText(targetName);
      }
      else
      {
         Log.getLog().logInformation(fileType + " download unsuccessful");
         controls.transferResultsText.append(fileType +
                                             " download unsuccessful\n");
      }
   }


   // Upload.
   private void upload()
   {
      Thread myThread;
      String uploadFile;
      String uploadType;
      String fileName;
      int    copies;
      int    copiesPushed;

      P2P.PeerAddress peerAddress;
      P2P.PeerList    triedList;
      P2P.PeerList    currentList;
      P2P.PeerList    pushList;
      P2P.PeerList    tempList;
      int             size;
      Socket          socket;
      boolean         done = false;

      // Save upload info.
      if ((myThread = transferThread) == null)
      {
         return;
      }

      uploadFile = controls.currentFileText.getText().trim();

      if (uploadFile.equals(""))
      {
         return;
      }

      File file = new File(uploadFile);
      fileName = file.getName();

      if (fileName.equals(""))
      {
         return;
      }

      if (!file.exists() || !file.canRead())
      {
         controls.transferResultsText.append("Cannot access file\n");

         return;
      }

      if (file.isFile())
      {
         uploadType = "file";
      }
      else
      {
         uploadType = "folder";
      }

      String s = controls.transferCopiesText.getText().trim();

      if (s.equals(""))
      {
         controls.transferResultsText.append("Invalid copies\n");

         return;
      }

      try {
         copies = Integer.parseInt(s);

         if (copies <= 0)
         {
            controls.transferResultsText.append("Invalid copies\n");

            return;
         }
      }
      catch (NumberFormatException e) {
         controls.transferResultsText.append("Invalid copies\n");

         return;
      }

      // No connections?
      if (p2p.connectedPeers.size() == 0)
      {
         controls.transferResultsText.append("No connections!\n");

         return;
      }

      // Search for accepting peers.
      Log.getLog().logInformation("Attempting to upload " + copies +
                                  " copies of file " + uploadFile);
      controls.transferResultsText.append("Attempting to upload " + copies +
                                          " copies of file " + uploadFile + "\n");
      controls.transferProgressBar.setMinimum(0);
      controls.transferProgressBar.setMaximum(copies);
      triedList    = p2p.createPeerList();
      currentList  = p2p.connectedPeers.copy();
      copiesPushed = 0;

      while ((copiesPushed < copies) && (currentList.size() > 0) &&
             (triedList.size() < MAX_UPLOAD_ATTEMPTS))
      {
         pushList = currentList.copy();

         while ((size = pushList.size()) > 0)
         {
            // Pick random peer.
            peerAddress = null;

            try {
               int i = controls.random.nextInt(size);
               peerAddress = (P2P.PeerAddress)pushList.get(i);
               pushList.remove(i);
            }
            catch (IndexOutOfBoundsException e) {
            }

            if (peerAddress == null)
            {
               continue;
            }

            // Check for interrupt.
            if ((myThread == transferThread) && transferInterrupt)
            {
               done = true;

               break;
            }

            // Sharing disabled?
            if (!controls.sharingEnabled)
            {
               controls.transferResultsText.append("Sharing disabled!\n");
               done = true;

               break;
            }

            // Create connection to peer.
            socket = null;

            try {
               socket = utils.createSocket(peerAddress.host,
                                           peerAddress.port);

               // Request upload.
               PrintWriter out = utils.getPrintWriter(socket);
               utils.printToStream(out,
                                   Double.toString(VERSION) + "\n" +
                                   Integer.toString(p2p.localPort) + "\n" +
                                   UPLOAD_REQUEST + "\n" + fileName + "\n" + uploadType);

               // Get response.
               DataInputStream in       = utils.getDataInputStream(socket);
               byte            response = utils.readByteFromStream(in);

               if (response == POSITIVE_RESPONSE)
               {
                  // Upload file.
                  Log.getLog().logInformation("Pushing to " +
                                              peerAddress.host + "...");
                  controls.transferResultsText.append("Pushing to " +
                                                      peerAddress.host + "...\n");

                  DataOutputStream up = utils.getDataOutputStream(socket);
                  putFile(up, uploadFile, myThread,
                          controls.currentSizeText,
                          controls.transferProgressBar);
                  copiesPushed++;
               }
            }
            catch (Exception e) {
               String msgString = new String("Error uploading file " +
                                             uploadFile + " to " + peerAddress.host + ":" +
                                             peerAddress.port + ": " + e.toString());
               Log.getLog().logWarning(msgString);
               controls.transferResultsText.append(msgString + "\n");
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
         }

         if (done)
         {
            break;
         }

         // Record tried peers.
         for (int i = 0; i < currentList.size(); i++)
         {
            peerAddress = null;

            try {
               peerAddress = (P2P.PeerAddress)currentList.get(i);
            }
            catch (IndexOutOfBoundsException e) {
            }

            if (peerAddress == null)
            {
               continue;
            }

            triedList.addPeer(peerAddress);
         }

         // Expand to next tier of peers.
         for (int i = 0; i < currentList.size(); i++)
         {
            peerAddress = null;

            try {
               peerAddress = (P2P.PeerAddress)currentList.get(i);
            }
            catch (IndexOutOfBoundsException e) {
            }

            if (peerAddress == null)
            {
               continue;
            }

            tempList = p2p.requestPeers(peerAddress);

            for (int j = 0; j < tempList.size(); j++)
            {
               peerAddress = null;

               try {
                  peerAddress = (P2P.PeerAddress)tempList.get(j);
               }
               catch (IndexOutOfBoundsException e) {
               }

               if (peerAddress == null)
               {
                  continue;
               }

               if (!triedList.isDuplicate(peerAddress) &&
                   !pushList.isDuplicate(peerAddress) &&
                   !peerAddress.host.equals(p2p.localHost))
               {
                  pushList.addPeer(peerAddress);
               }
            }
         }

         currentList = pushList.copy();
      }

      Log.getLog().logInformation("Upload completed: " + copiesPushed +
                                  " copies pushed");
      controls.transferResultsText.append("Upload completed: " +
                                          copiesPushed + " copies pushed\n");
   }


   /**
    * Run transfer thread.
    */
   public void run()
   {
      if (Thread.currentThread() != transferThread)
      {
         return;
      }

      // Determine operation.
      switch (operation)
      {
      case SEARCH:
         search();

         break;

      case DOWNLOAD:
         download();

         break;

      case UPLOAD:
         upload();

         break;
      }

      // Terminate operation.
      transferThread = null;
      operation      = NOP;
      controls.transferOperationSwitch.setSelected(false);
      controls.transferOperationSwitch.setText("Start");
      controls.transferProgressBar.setMinimum(0);
      controls.transferProgressBar.setMaximum(10);
      controls.transferProgressBar.setValue(0);
   }


   // Get file/folder from input stream.
   public void getFile(DataInputStream in, String to, Thread myThread,
                       JTextField sizeText, JProgressBar progressBar)
   throws IOException
   {
      long   size;
      String fileName;

      // Get type: file or folder.
      byte type = utils.readByteFromStream(in);

      if (type == FOLDER)
      {
         // Create the folder and get the files in it.
         File file = new File(to);
         file.mkdir();

         String path = file.getAbsolutePath();
         size = utils.readLongFromStream(in);

         for (int i = 0; i < size; i++)
         {
            fileName = utils.readLineFromStream(in);
            getFile(in, path + File.separator + fileName, myThread,
                    sizeText, progressBar);
         }
      }
      else  // not folder

      {     // Get file size.
         size = utils.readLongFromStream(in);

         if ((size < 0) || (size > MAX_TRANSFER_FILE_SIZE))
         {
            throw new IOException("Invalid getFile size=" + size +
                                  ", MAX_TRANSFER_FILE_SIZE=" + MAX_TRANSFER_FILE_SIZE);
         }

         // Initialize size and progress bar displays.
         if (sizeText != null)
         {
            sizeText.setText(Long.toString(size / 1000));
         }

         if (progressBar != null)
         {
            progressBar.setMinimum(0);
            progressBar.setMaximum((int)size);
            progressBar.setValue(0);
         }

         // Read file.
         BufferedOutputStream bos = Utils.makeBOS(to);
         int rcv = 0;
         int nr;

         while (rcv < size)
         {
            utils.waitForAvailableData(in, 1);

            if ((nr = utils.getAvailableData(in)) == 0)
            {
               bos.close();
               utils.closeDataInputStream(in);
               throw new IOException("getFile time-out");
            }

            nr = Math.min(nr, (int)(size - rcv));

            byte[] data = new byte[nr];
            utils.readBytesFromStream(in, data);
            rcv += nr;
            bos.write(data, 0, nr);

            if (progressBar != null)
            {
               progressBar.setValue(rcv);
            }

            if ((myThread != null) && (transferThread == myThread) &&
                transferInterrupt)
            {
               bos.close();
               utils.closeDataInputStream(in);
               throw new IOException("getFile interrupted");
            }
         }

         bos.close();
      }
   }


   // Put file/folder to output stream.
   public void putFile(DataOutputStream out, String from, Thread myThread,
                       JTextField sizeText, JProgressBar progressBar)
   throws IOException
   {
      long size;

      byte[] data;
      String fileName;
      File   file = new File(from);

      if (!file.exists())
      {
         return;
      }

      if (file.isDirectory())
      {
         // Write folder header.
         utils.writeByteToStream(out, FOLDER);
         size = 0;

         LinkedList fileNames = Utils.listFiles(from);
         size = fileNames.size();
         utils.writeLongToStream(out, size);

         // Write files in folder.
         String path = file.getAbsolutePath();

         for (int i = 0; i < fileNames.size(); i++)
         {
            fileName = (String)fileNames.get(i);
            data     = (fileName + "\n").getBytes();
            utils.writeBytesToStream(out, data, data.length);
            fileName = path + File.separator + (String)fileNames.get(i);
            putFile(out, fileName, myThread, sizeText, progressBar);
         }

         if ((myThread != null) && (transferThread == myThread) &&
             transferInterrupt)
         {
            utils.closeDataOutputStream(out);
            throw new IOException("putFile interrupted");
         }
      }
      else  // not folder

      {     // Write file header.
         utils.writeByteToStream(out, FILE);
         size = 0;

         if (file.exists() && file.canRead())
         {
            size = file.length();
         }

         utils.writeLongToStream(out, size);

         // Initialize size and progress bar displays.
         if (sizeText != null)
         {
            sizeText.setText(Long.toString(size / 1000));
         }

         if (progressBar != null)
         {
            progressBar.setMinimum(0);
            progressBar.setMaximum((int)size);
            progressBar.setValue(0);
         }

         // Write file.
         BufferedInputStream bis = Utils.makeBIS(from);
         byte[] buf = new byte[BLOCK_SIZE];
         int sent = 0;
         int ns;

         while ((sent < size) && ((ns = bis.read(buf, 0, buf.length)) > 0))
         {
            sent += ns;
            utils.writeBytesToStream(out, buf, ns);

            if (progressBar != null)
            {
               progressBar.setValue(sent);
            }

            if ((myThread != null) && (transferThread == myThread) &&
                transferInterrupt)
            {
               bis.close();
               utils.closeDataOutputStream(out);
               throw new IOException("putFile interrupted");
            }
         }

         bis.close();
      }
   }
}
