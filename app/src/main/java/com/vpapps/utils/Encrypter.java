package com.vpapps.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.facebook.android.crypto.keychain.SharedPrefsBackedKeyChain;
import com.facebook.crypto.Crypto;
import com.facebook.crypto.Entity;
import com.facebook.crypto.exception.CryptoInitializationException;
import com.facebook.crypto.exception.KeyChainException;
import com.facebook.crypto.util.SystemNativeCryptoLibrary;
import com.vpapps.item.ItemSong;
import com.vpapps.EnthronementLifestyle.BuildConfig;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import okio.BufferedSource;

public class Encrypter {
    private DBHelper dbHelper;
    private Context context;
    public Crypto _crypto;
    private Entity _entity;
    private boolean isInited = false;
    private static Encrypter instance = null;
    public static final int ENC_BLOCK_SIZE = 4096;

    private Encrypter() {
        isInited = false;
    }

    public static Encrypter GetInstance() {
        if (instance == null) {
            instance = new Encrypter();
        }
        return instance;
    }

    public void Init(Context context, String saltString) {
        this.context = context;
        _crypto = new Crypto(new SharedPrefsBackedKeyChain(context),
                new SystemNativeCryptoLibrary());
        _entity = new Entity(saltString);
        isInited = true;
        dbHelper = new DBHelper(context);
    }

    public void encryptSong(File file) throws IOException {
        File dir = Environment.getExternalStorageDirectory();

        String infoFileName = ".mp3Info";
        Set<String> filesEncrypted = GetAllEncryptedFilePaths(dir.getAbsolutePath() + File.separator + "Online MP3/" + infoFileName);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(dir.getAbsoluteFile() + File.separator + "Online MP3/" + infoFileName, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (filesEncrypted == null || !filesEncrypted.contains(file.getAbsolutePath())) {
            try {
                Encrypt(file);
                bw.write(file.getAbsolutePath());
                bw.newLine();
                if (filesEncrypted == null) {
                    filesEncrypted = new HashSet<String>();
                }
                filesEncrypted.add(file.getAbsolutePath());
                //file.delete();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (KeyChainException e) {
                e.printStackTrace();
            } catch (CryptoInitializationException e) {
                e.printStackTrace();
            }
        }
        if (bw != null) {
            bw.close();
        }
    }

    public Set<String> GetAllEncryptedFilePaths(String pathToInfoFile) throws IOException {
        File f = new File(pathToInfoFile);
        if (!f.exists()) {
            return null;
        }
        Set<String> filesEncrypted = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line;
        while ((line = br.readLine()) != null) {
            filesEncrypted.add(line);
        }
        return filesEncrypted;
    }

    public String GetEditedFileName(File file, String token) {
        String path = file.getAbsolutePath();
        String extension;
        String fname;
        int i = path.lastIndexOf('.');
        if (i > 0) {
            extension = path.substring(i);
            fname = path.substring(0, i) + token;
        } else {
            fname = path + token;
        }
        return fname;
    }

    private void Encrypt(File file) throws IOException, KeyChainException, CryptoInitializationException {
        String encBlockFileName = GetEditedFileName(file, "_0");
        String plainBlockFileName = GetEditedFileName(file, "_1");
        InputStream fis = new FileInputStream(file);
        FileOutputStream encfileStream = new FileOutputStream(encBlockFileName);
        FileOutputStream plainfileStream = new FileOutputStream(plainBlockFileName);

// Creates an output stream which encrypts the data as
// it is written to it and writes it out to the file.
        OutputStream cryptoCipherOutputStream = _crypto.getCipherOutputStream(
                encfileStream,
                _entity);
        byte[] buff = new byte[ENC_BLOCK_SIZE];
        fis.read(buff);
        String s = new String(buff);
        Log.i("Encrypter", "#decrypt=" + s);
        cryptoCipherOutputStream.write(buff);
        byte[] buffer = new byte[1024];
        int lenth;
        while ((lenth = fis.read(buffer)) != -1) {
            plainfileStream.write(buffer, 0, lenth);
        }
        fis.close();
        cryptoCipherOutputStream.close();
        plainfileStream.close();
        file.delete();

        RandomAccessFile raf = new RandomAccessFile(plainBlockFileName, "rw");
        byte[] buf = new byte[65536];
        long pos = 0;
        int len;
        Random random = new Random(34);
        while ((len = raf.read(buf)) != -1) {
            for (int i = 0; i < len; i++) {
                buf[i] ^= random.nextInt();
            }
            raf.seek(pos);
            raf.write(buf);
            pos = raf.getFilePointer();
        }
        raf.close();
    }

    public boolean IsInitialized() {
        return isInited;
    }

    public void BreakAndEncrypt(String fileLoc, String fileName, int encBlockSize) throws IOException, KeyChainException, CryptoInitializationException {
        String encBlockFileName = "0_" + fileName;
        String plainBlockFileName = "1_" + fileName;
        File src = new File(fileLoc + File.separator + fileName);
        InputStream fis = new FileInputStream(src);
        FileOutputStream encfileStream = new FileOutputStream(fileLoc + File.separator + encBlockFileName);
        FileOutputStream plainfileStream = new FileOutputStream(fileLoc + File.separator + plainBlockFileName);

// Creates an output stream which encrypts the data as
// it is written to it and writes it out to the file.
        OutputStream cryptoCipherOutputStream = _crypto.getCipherOutputStream(
                encfileStream,
                _entity);
        byte[] buf = new byte[encBlockSize];
        fis.read(buf);
        String s = new String(buf);
        Log.i("Encrypter", "#decrypt=" + s);
        cryptoCipherOutputStream.write(buf);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = fis.read(buffer)) != -1) {
            plainfileStream.write(buffer, 0, len);
        }
        fis.close();
        cryptoCipherOutputStream.close();
        plainfileStream.close();
        //src.delete();
    }

    public InputStream GetDecryptedBlockData(String filePath) throws IOException, KeyChainException, CryptoInitializationException {
        long a = System.currentTimeMillis();
        FileInputStream fileInputStream = new FileInputStream(filePath);
        InputStream inputStream = _crypto.getCipherInputStream(
                fileInputStream,
                _entity);

        return inputStream;
    }

    public InputStream decrypt(String filePath) throws KeyChainException, CryptoInitializationException, IOException {

        InputStream fis = new FileInputStream(filePath);
        InputStream cryptoStream = _crypto.getCipherInputStream(fis,  new Entity(BuildConfig.DOWNLOAD_ENC_KEY));

//        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/abc song");
//        File mypath = new File(directory, "bb.mp3");

//        OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(mypath));

//        int read = 0;
//        byte[] buffer = new byte[1024];
//        while ((read = cryptoStream.read(buffer)) != -1) {
//            fileStream.write(buffer, 0, read);
//        }
//        cryptoStream.close();
//        fis.close();
//        fileStream.close();

        return cryptoStream;
    }

    public void encrypt(String fileName, BufferedSource bufferedSource, final ItemSong itemSong) {
        try {
            final long a = System.currentTimeMillis();

            File file_encypt = new File(GetEditedFileName(new File(fileName),""));
            final String fileSavedName = file_encypt.getName();
            itemSong.setTempName(fileSavedName);

            if (!_crypto.isAvailable()) {
                return;
            }

            OutputStream fileStream = new BufferedOutputStream(new FileOutputStream(file_encypt));
            OutputStream outputStream = _crypto.getCipherOutputStream(
                    fileStream, new Entity(BuildConfig.DOWNLOAD_ENC_KEY));


            InputStream fis = bufferedSource.inputStream();
            int len;
            byte[] buffer = new byte[2048];
            while ((len =  fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            fis.close();
            outputStream.close();
            bufferedSource.close();

            new AsyncTask<String, String, String>() {
                String imageName;

                @Override
                protected String doInBackground(String... strings) {
                    imageName = getBitmapFromURL(itemSong.getImageBig(), fileSavedName);
                    if(!imageName.equals("0")) {
                        return "1";
                    } else {
                        return "0";
                    }
                }

                @Override
                protected void onPostExecute(String s) {
                    super.onPostExecute(s);
                    if(s.equals("1")) {
//                        itemSong.setImageBitmap(bitmap);
                        itemSong.setImageBig(imageName);
                        itemSong.setImageSmall(imageName);
                        itemSong.setTempName(fileSavedName);
                    } else {
                        imageName = "null";
                        itemSong.setImageBig(imageName);
                        itemSong.setImageSmall(imageName);
                        itemSong.setTempName(fileSavedName);
                    }
                    dbHelper.addToDownloads(itemSong);
                }
            }.execute();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        file.delete();
    }

    public String getBitmapFromURL(String src, String name) {
        try {
            if(src != null && src.equals("")) {
                src = "null";
            }
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes);

            File f_root = new File(context.getExternalCacheDir() + File.separator + "/tempim/");
            if(!f_root.exists()) {
                f_root.mkdirs();
            }

            File f = new File(f_root, name);

            f.createNewFile();
//write the bytes in file
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());

// remember close de FileOutput
            fo.close();

            return f.getAbsolutePath();
        } catch (IOException e) {
            // Log exception
            return "0";
        }
    }
}
