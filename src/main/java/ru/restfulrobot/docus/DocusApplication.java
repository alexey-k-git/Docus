package ru.restfulrobot.docus;

import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.xml.bind.DatatypeConverter;
import com.mongodb.*;

public class DocusApplication {

    // точка входа в программу создает первого пользователя с именем admin и даёт ему исключтильные права
    public static void main(String[] args) throws UnknownHostException {
        Random random = new Random();
        MongoClient client = new MongoClient();
        ServerAddress address = client.getAddress();
        System.out.println("Info: ");
        System.out.println("Host: " + address.getHost());
        System.out.println("Port: " + address.getPort());
        System.out.println("SocketAddress: " + address.getSocketAddress());
        DB db = client.getDB("documents");
        DBCollection users = db.getCollection("users");
        DBObject admin = users.findOne((new BasicDBObject("_id", "admin")));
        if (admin == null) {
            admin = new BasicDBObject("_id", "admin")
                    .append("password", encodePassword("admin", "DocusForever"))
                    .append("chief", true).append("department", "general");
            users.insert(admin);
            System.out.println("Пользователь admin. Пароль: 'admin' создан");
        }
        else {
            System.out.println("Пользователь Admin уже существует");
        }
    }

    // Метод генерирует пароль для админа
    public static String encodePassword(String password, String salt) {
        try {
            String saltedAndHashed = password + "," + salt;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(saltedAndHashed.getBytes());
            byte hashedBytes[] = (new String(digest.digest(), "UTF-8"))
                    .getBytes();
            return DatatypeConverter.printBase64Binary(hashedBytes) + ","
                    + salt;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is not available", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 unavailable?  Not a chance", e);
        }
    }
}
