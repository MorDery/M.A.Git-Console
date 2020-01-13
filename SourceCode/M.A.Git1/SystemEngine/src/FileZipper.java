
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import java.io.*;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class FileZipper {

    public static void WriteToZip(String i_Path, String i_NameFile, String i_DestinationPath) {
        ZipOutputStream out = null;
        File f = null;
        byte[] daraByte = new byte[1024];
        InputStream inputstream = null;
        try {
            inputstream = new FileInputStream(i_Path + i_NameFile);
            f = new File(i_DestinationPath + i_NameFile);

            out = new ZipOutputStream(new FileOutputStream(f));

            ZipEntry e = new ZipEntry(i_NameFile);
            out.putNextEntry(e);


            int len = inputstream.read(daraByte, 0, 1024);
            while (len != -1) {
                out.write(daraByte, 0, len);
                len = inputstream.read(daraByte, 0, 1024);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputstream.close();
                out.closeEntry();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public static void ReadFromZip(String i_Path, String i_NameFile, String i_DestinationPath, String i_DestinationNameFile) {
        byte[] daraByte = new byte[1024];
        ZipFile zipFile = null;
        OutputStream outputstream = null;
        try {
            zipFile = new ZipFile(i_Path + i_NameFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            outputstream = new FileOutputStream(i_DestinationPath + i_DestinationNameFile);
            ZipEntry entry = entries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            int len = stream.read(daraByte, 0, 1024);
            while (len != -1) {
                outputstream.write(daraByte, 0, len);
                len = stream.read(daraByte, 0, 1024);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                outputstream.close();
                zipFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public static String unZip(String i_ZipPath) {

        try {
            FileInputStream fis = new FileInputStream(i_ZipPath);
            ZipInputStream zis = new ZipInputStream(fis);
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];

            while (zis.getNextEntry() != null) {
                zis.read(buffer, 0, buffer.length);
                sb.append(new String(buffer));
            }

            return sb.toString();
        }
        catch(IOException e)
        {
            return null ;
        }
    }

    public static void Zip(Path i_Path, String i_Value ) {
        ZipOutputStream out = null;
        File f = null;
        try {
            f = new File(i_Path.toString());
            out = new ZipOutputStream(new FileOutputStream(f));

            ZipEntry e = new ZipEntry(i_Path.getFileName().toString());
            out.putNextEntry(e);
            out.write(i_Value.getBytes(), 0, i_Value.getBytes().length);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.closeEntry();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
}


