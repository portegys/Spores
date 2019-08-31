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

import java.math.*;

import java.net.*;

import java.util.*;


/**
 * Utilities.
 */
public class Utils implements Parameters {
   // Maximum number of bytes to hash in file.
   public static final int MAX_HASH_BYTES = 1000;

   // Id generator.
   static int idGenerator = 0;

   // Instance id.
   int id;

   // Constructor.
   Utils()
   {
      id = getID();
   }


   // Create a server socket.
   ServerSocket createServerSocket(int port) throws IOException
   {
      return(new ServerSocket(port));
   }


   // Accept server socket connection.
   Socket acceptSocket(ServerSocket serverSocket) throws IOException
   {
      return(serverSocket.accept());
   }


   // Close a server socket.
   void closeServerSocket(ServerSocket socket) throws IOException
   {
      socket.close();
   }


   // Create a socket with time-out of SOCKET_TIME_OUT.
   // Throw exception of not created in time.
   Socket createSocket(String host, int port) throws IOException
   {
      Socket socket = null;
      ConnectTimeOutSocketFactory socketFactory = new ConnectTimeOutSocketFactory();

      socket = socketFactory.createSocket(host, port);
      socket.setSoTimeout(SOCKET_TIME_OUT);

      return(socket);
   }


   // Close a socket.
   void closeSocket(Socket socket) throws IOException
   {
      socket.close();
   }


   // Get local address.
   String getLocalAddress() throws Exception
   {
      return(getLocalHostLANAddress().getHostAddress());
   }


   /**
    * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
    * <p/>
    * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
    * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
    * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
    * specify the algorithm used to select the address returned under such circumstances, and will often return the
    * loopback address, which is not valid for network communication. Details
    * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
    * <p/>
    * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
    * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
    * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
    * first site-local address if the machine has more than one), but if the machine does not hold a site-local
    * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
    * <p/>
    * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
    * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
    * <p/>
    *
    * @throws UnknownHostException If the LAN address of the machine cannot be found.
    * Source: https://issues.apache.org/jira/browse/JCS-40
    */
   public static InetAddress getLocalHostLANAddress() throws UnknownHostException
   {
      try
      {
         InetAddress candidateAddress = null;
         // Iterate all NICs (network interface cards)...
         for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); )
         {
            NetworkInterface iface = (NetworkInterface)ifaces.nextElement();
            // Iterate all IP addresses assigned to each card...
            for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); )
            {
               InetAddress inetAddr = (InetAddress)inetAddrs.nextElement();
               if (!inetAddr.isLoopbackAddress())
               {
                  if (inetAddr.isSiteLocalAddress())
                  {
                     // Found non-loopback site-local address. Return it immediately...
                     return(inetAddr);
                  }
                  else if (candidateAddress == null)
                  {
                     // Found non-loopback address, but not necessarily site-local.
                     // Store it as a candidate to be returned if site-local address is not subsequently found...
                     candidateAddress = inetAddr;
                     // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                     // only the first. For subsequent iterations, candidate will be non-null.
                  }
               }
            }
         }
         if (candidateAddress != null)
         {
            // We did not find a site-local address, but we found some other non-loopback address.
            // Server might have a non-site-local address assigned to its NIC (or it might be running
            // IPv6 which deprecates the "site-local" concept).
            // Return this non-loopback candidate address...
            return(candidateAddress);
         }
         // At this point, we did not find a non-loopback address.
         // Fall back to returning whatever InetAddress.getLocalHost() returns...
         InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
         if (jdkSuppliedAddress == null)
         {
            throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
         }
         return(jdkSuppliedAddress);
      }
      catch (Exception e)
      {
         UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
         unknownHostException.initCause(e);
         throw unknownHostException;
      }
   }


   // Get remote address.
   String getRemoteAddress(Socket socket)
   {
      InetAddress inetAddress = socket.getInetAddress();

      return(inetAddress.getHostAddress());
   }


   // Get a DataInputStream for a socket.
   DataInputStream getDataInputStream(Socket socket) throws IOException
   {
      DataInputStream in = new DataInputStream(socket.getInputStream());

      return(in);
   }


   // Close DataInputStream.
   void closeDataInputStream(DataInputStream in) throws IOException
   {
      in.close();
   }


   // Get a DataOutputStream for a socket.
   DataOutputStream getDataOutputStream(Socket socket)
   throws IOException
   {
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());

      return(out);
   }


   // Close DataOutputStream.
   void closeDataOutputStream(DataOutputStream out) throws IOException
   {
      out.close();
   }


   // Get a PrintWriter for a socket.
   PrintWriter getPrintWriter(Socket socket) throws IOException
   {
      PrintWriter out = new PrintWriter(new BufferedWriter(
                                           new OutputStreamWriter(socket.getOutputStream())), true);

      return(out);
   }


   // Close PrintWriter.
   void closePrintWriter(PrintWriter out) throws IOException
   {
      out.close();
   }


   // Wait for minimun data availability on given stream.
   // Time-out and return false if not available.
   boolean waitForAvailableData(DataInputStream in, int minData, int timeOut)
   throws IOException
   {
      for (int i = 0; (i < timeOut) && (in.available() < minData);
           i += 100)
      {
         try {
            Thread.sleep(100);
         }
         catch (InterruptedException e) {
         }
      }

      if (in.available() >= minData)
      {
         return(true);
      }
      else
      {
         return(false);
      }
   }


   boolean waitForAvailableData(DataInputStream in, int minData)
   throws IOException
   {
      return(waitForAvailableData(in, minData, SOCKET_TIME_OUT));
   }


   // Get available data on stream.
   int getAvailableData(DataInputStream in) throws IOException
   {
      return(in.available());
   }


   // Read a byte from a given stream.
   // Time-out and throw exception if not available.
   byte readByteFromStream(DataInputStream in) throws IOException
   {
      return(readByteFromStream(in, SOCKET_TIME_OUT));
   }


   // Read a byte from a given stream given a timer.
   // Time-out and throw exception if not available.
   byte readByteFromStream(DataInputStream in, int timer)
   throws IOException
   {
      if (waitForAvailableData(in, 1, timer))
      {
         return(in.readByte());
      }

      throw new IOException("readByteFromStream time-out");
   }


   // Write a byte to a given stream.
   void writeByteToStream(DataOutputStream out, byte data)
   throws IOException
   {
      out.writeByte(data);
      out.flush();
   }


   // Read bytes from a given stream.
   // Time-out and throw exception if not available.
   void readBytesFromStream(DataInputStream in, byte[] data)
   throws IOException
   {
      readBytesFromStream(in, data, SOCKET_TIME_OUT);
   }


   // Read bytes from a given stream with a timer.
   // Time-out and throw exception if not available.
   void readBytesFromStream(DataInputStream in, byte[] data, int timer)
   throws IOException
   {
      if (waitForAvailableData(in, data.length, timer))
      {
         in.readFully(data);

         return;
      }

      throw new IOException("readBytesFromStream time-out");
   }


   // Write bytes to a given stream.
   void writeBytesToStream(DataOutputStream out, byte[] data, int size)
   throws IOException
   {
      out.write(data, 0, size);
      out.flush();
   }


   // Read a line from a given stream.
   // Time-out and throw exception if not available.
   String readLineFromStream(DataInputStream in) throws IOException
   {
      return(readLineFromStream(in, SOCKET_TIME_OUT));
   }


   // Read a line from a given stream.
   // Time-out and throw exception if not available.
   String readLineFromStream(DataInputStream in, int timer)
   throws IOException
   {
      String line = "";

      byte[] data = new byte[1];
      boolean gotLine = false;

      while (!gotLine)
      {
         if (!waitForAvailableData(in, 1, timer))
         {
            throw new IOException("readLineFromStream time-out");
         }

         data[0] = in.readByte();

         String s = new String(data);

         if (s.equals("\n"))
         {
            break;
         }

         line = line.concat(s);

         if (line.length() >= MAX_LINE_SIZE)
         {
            break;
         }
      }

      return(line.trim());
   }


   // Write a line to a given stream.
   void writeLineToStream(DataOutputStream out, String line)
   throws IOException
   {
      byte[] data = (line + "\n").getBytes();
      out.write(data, 0, data.length);
      out.flush();
   }


   // Print a line to a given stream.
   void printToStream(PrintWriter out, String line) throws IOException
   {
      out.println(line);
      out.flush();
   }


   // Read a long int from a given stream.
   // Time-out and throw exception if not available.
   long readLongFromStream(DataInputStream in) throws IOException
   {
      if (waitForAvailableData(in, 4))
      {
         byte[] data = new byte[4];
         in.readFully(data);

         return(Utils.bytesToLong(data));
      }

      throw new IOException("readLongFromStream time-out");
   }


   // Write a long int to a given stream.
   void writeLongToStream(DataOutputStream out, long num)
   throws IOException
   {
      byte[] data = longToBytes(num);
      out.write(data, 0, data.length);
      out.flush();
   }


   // Get MD5 hash of given file.
   // If folder, hash is hash of concatenated file hashes.
   public static String getMD5ForFile(String fileName)
   {
      MD5InputStream md5in;
      String         code = "";
      File           file = new File(fileName);

      if (!file.exists())
      {
         return(code);
      }

      if (file.isDirectory())
      {
         String     path      = file.getAbsolutePath();
         LinkedList fileNames = listFiles(fileName);

         for (int i = 0; i < fileNames.size(); i++)
         {
            fileName = (String)fileNames.get(i);
            code     = code.concat(getMD5ForFile(path + File.separator +
                                                 fileName));
         }

         // Hash the string.
         code = getMD5ForString(code);
      }
      else
      {
         // Hash file.
         FileInputStream in = null;

         try {
            in    = new FileInputStream(fileName);
            md5in = new MD5InputStream(in);

            for (int i = 0; (i < MAX_HASH_BYTES) && (md5in.read() != -1);
                 i++)
            {
            }

            code = md5in.getHashString().trim();
         }
         catch (Exception e) {
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
      }

      return(code);
   }


   // Get MD5 hash of given string.
   public static String getMD5ForString(String string)
   {
      MD5InputStream       md5in;
      String               code = "";
      ByteArrayInputStream in   = null;

      try {
         in    = new ByteArrayInputStream(string.getBytes());
         md5in = new MD5InputStream(in);

         for (int i = 0; (i < MAX_HASH_BYTES) && (md5in.read() != -1);
              i++)
         {
         }

         code = md5in.getHashString().trim();
      }
      catch (Exception e) {
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

      return(code);
   }


   // Recursively copy a file/folder.
   public static void copyFile(String from, String to)
   throws IOException
   {
      File fromFile = new File(from);

      if (!fromFile.exists())
      {
         return;
      }

      if (fromFile.isDirectory())
      {
         File toFile = new File(to);
         toFile.mkdir();

         String fromDir = fromFile.getAbsolutePath();
         String toDir   = toFile.getAbsolutePath();
         String[] fileNames = fromFile.list();
         String fileName;

         for (int i = 0; i < fileNames.length; i++)
         {
            fileName = fileNames[i];
            copyFile(fromDir + File.separator + fileName,
                     toDir + File.separator + fileName);
         }
      }
      else
      {
         // Copy file.
         BufferedInputStream  bis = makeBIS(from);
         BufferedOutputStream bos = makeBOS(to);
         byte[] buf = new byte[BLOCK_SIZE];
         int nr;

         while ((nr = bis.read(buf, 0, buf.length)) > 0)
         {
            bos.write(buf, 0, nr);
         }

         bis.close();
         bos.close();
      }
   }


   // Delete file.
   // If folder, recursively delete.
   public static void deleteFile(String fileName)
   {
      File file = new File(fileName);

      if (!file.exists())
      {
         return;
      }

      if (file.isDirectory())
      {
         String path = file.getAbsolutePath();
         String[] fileNames = file.list();

         for (int i = 0; i < fileNames.length; i++)
         {
            deleteFile(path + File.separator + fileNames[i]);
         }
      }

      file.delete();
   }


   // Get file size.
   // If folder, size is recursive sum of files sizes.
   public static long getFileSize(String fileName)
   {
      File file = new File(fileName);

      if (!file.exists())
      {
         return(0);
      }

      if (file.isDirectory())
      {
         long   size = 0;
         String path = file.getAbsolutePath();
         String[] fileNames = file.list();

         for (int i = 0; i < fileNames.length; i++)
         {
            size += getFileSize(path + File.separator + fileNames[i]);
         }

         return(size);
      }
      else
      {
         return(file.length());
      }
   }


   // Get file count.
   // If folder, count is recursive count of files.
   public static long getFileCount(String fileName)
   {
      File file = new File(fileName);

      if (!file.exists())
      {
         return(0);
      }

      long count = 1;

      if (file.isDirectory())
      {
         String path = file.getAbsolutePath();
         String[] fileNames = file.list();

         for (int i = 0; i < fileNames.length; i++)
         {
            count += getFileCount(path + File.separator + fileNames[i]);
         }
      }

      return(count);
   }


   // Name-ordered list of files in given folder.
   public static LinkedList listFiles(String folderName)
   {
      File folder = new File(folderName);

      if (!folder.exists() || !folder.isDirectory())
      {
         return(null);
      }

      LinkedList fileList = new LinkedList();
      String[] fileNames = folder.list();

      for (int i = 0; i < fileNames.length; i++)
      {
         fileList.add(fileNames[i]);
      }

      Collections.sort(fileList);

      return(fileList);
   }


   // Make a buffered input stream.
   public static BufferedInputStream makeBIS(String fileName)
   throws IOException
   {
      FileInputStream fis = new FileInputStream(fileName);

      return(new BufferedInputStream(fis, BLOCK_SIZE));
   }


   // Make a buffered output stream.
   public static BufferedOutputStream makeBOS(String fileName)
   throws IOException
   {
      FileOutputStream fos = new FileOutputStream(fileName);

      return(new BufferedOutputStream(fos, BLOCK_SIZE));
   }


   // Convert long to 4 bytes.
   public static byte[] longToBytes(long num) throws ArithmeticException
   {
      BigInteger bigNum = new BigInteger(Long.toString(num));

      byte[] data     = bigNum.toByteArray();
      byte[] numBytes = new byte[4];

      for (int i = 0; i < 4; i++)
      {
         numBytes[i] = 0;
      }

      switch (data.length)
      {
      case 1:
         numBytes[3] = data[0];

         break;

      case 2:
         numBytes[2] = data[0];
         numBytes[3] = data[1];

         break;

      case 3:
         numBytes[1] = data[0];
         numBytes[2] = data[1];
         numBytes[3] = data[2];

         break;

      case 4:
         numBytes[0] = data[0];
         numBytes[1] = data[1];
         numBytes[2] = data[2];
         numBytes[3] = data[3];

         break;

      default:
         throw new ArithmeticException("longToBytes error");
      }

      return(numBytes);
   }


   // Convert 4 bytes to long.
   public static long bytesToLong(byte[] data)
   {
      BigInteger bigNum = new BigInteger(data);

      return(bigNum.longValue());
   }


   static synchronized int getID()
   {
      idGenerator++;

      return(idGenerator);
   }


   // File list element.
   static class FileElem {
      String name;
      byte   type;
      long   size;
      long   modified;
      String md5code;
   }

   // File name comparator.
   static class NameCompare implements Comparator {
      public int compare(Object file1, Object file2)
      {
         FileElem f1 = (FileElem)file1;
         FileElem f2 = (FileElem)file2;

         return(f1.name.compareTo(f2.name));
      }


      public boolean equals(Object o)
      {
         return(false);
      }
   }

   // File modification time comparator.
   static class ModifiedCompare implements Comparator {
      public int compare(Object file1, Object file2)
      {
         FileElem f1 = (FileElem)file1;
         FileElem f2 = (FileElem)file2;

         return((int)(f1.modified - f2.modified));
      }


      public boolean equals(Object o)
      {
         return(false);
      }
   }

   // File size comparator.
   static class SizeCompare implements Comparator {
      public int compare(Object file1, Object file2)
      {
         FileElem f1 = (FileElem)file1;
         FileElem f2 = (FileElem)file2;

         return((int)(f1.size - f2.size));
      }


      public boolean equals(Object o)
      {
         return(false);
      }
   }
}
