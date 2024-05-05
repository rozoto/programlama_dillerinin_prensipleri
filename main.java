/** 
* 
* @author Nurgül Serin  nurgul.serin@ogr.sakarya.edu.tr
* @since 5.04.2024
* <p> 
*  işlemlerin yapıldığı sınıf
* </p> 
*/ 

package pdp1odev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class main {

    public static void main(String[] args) {
        System.out.println("Github Analiz Uygulaması");

        // Kullanıcıdan GitHub depo URL'sini al
        String repoUrl = promptUserForInput("URL'i giriniz:");

        // Depoyu klonla ve dosyaları getir
        cloneAndRetrieveFiles(repoUrl);

    }

    private static String promptUserForInput(String prompt) {
        System.out.println(prompt);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = null;
        try {
            input = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

    private static void cloneAndRetrieveFiles(String repoUrl) {
        // GitHub depoyu klonla
        try {
            Process process = Runtime.getRuntime().exec("git clone " + repoUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
            reader.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        // Klonlanan depodaki *.java uzantılı dosyaları getir
        String directoryPath = "./" + getRepoNameFromUrl(repoUrl);
        File directory = new File(directoryPath);
        File[] javaFiles = getAllJavaFiles(directory);

        // Dosyaları analiz et
        analizJavaFiles(javaFiles);
    }

    private static String getRepoNameFromUrl(String repoUrl) {
        String[] parts = repoUrl.split("/");
        return parts[parts.length - 1].replace(".git", "");
    }

    private static File[] getAllJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    javaFiles.addAll(Arrays.asList(getAllJavaFiles(file)));
                } else if (file.getName().endsWith(".java")) {
                    javaFiles.add(file);
                }
            }
        }
        return javaFiles.toArray(new File[0]);
    }

    private static void analizJavaFiles(File[] javaFiles) {
        for (File file : javaFiles) {
            // Dosyayı analiz et ve sonuçları ekrana yazdır
            analyzeFile(file);
        }
    }

    private static void analyzeFile(File file) {
        int javadocSatiri = 0; // Javadoc yorum satırı sayısı
        int digerYorumSatirlari = 0; // Diğer yorum satırı sayısı
        int kodSatirSayisi = 0; // Kod satırı sayısı (tüm yorum ve boşluk satırları hariç)
        int toplamSatirSayisi = 0; // Toplam satır sayısı
        int fonksiyonSayisi = 0; // Fonksiyon sayısı

        boolean inCommentBlock = false; // javadoc bloğu içinde mi?
        boolean inOtherCommentBlock = false; //diğer yorum blloğu içinde mi?
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                toplamSatirSayisi++;

                // Satırın başındaki ve sonundaki boşlukları temizle
                line = line.trim();

                // Javadoc yorumlarını ve çoklu satır yorumlarını işle
                if (line.startsWith("/**")) {
                    inCommentBlock = true;
                } else if (line.startsWith("/*")) {
                    if (!line.contains("*/")) {
                        inOtherCommentBlock = true;
                    }
                } else if (inCommentBlock && line.startsWith("*")) { //javadoc mu?
                    if (!line.contains("*/")) {
                    	javadocSatiri++;
                    } else {
                        inCommentBlock = false;
                    }
                } else if (inOtherCommentBlock) {//normal yorum satırı mı?
                    if (!line.contains("*/")) {
                    	digerYorumSatirlari++;
                    } else {
                        inOtherCommentBlock = false;
                    }
                } else if (line.contains("//")) { // "//" içeriyor mu?
                    // Tekli satır yorum
                	if(!line.startsWith("//")) { // "//" satırın başında değilse kod satırına ekle
                		kodSatirSayisi++;
                		digerYorumSatirlari++;
                	}else {
                		digerYorumSatirlari++;
                	}
                } else if (!line.isEmpty()) {
                    // Kod satırı
                	kodSatirSayisi++;
                }

              // Fonksiyon sayısını hesapla
             // Satırda "(" ve ")" işaretlerini kontrol et
                if (line.contains("(") && line.contains(")")) {
                    // Satırda "{" işareti var mı diye kontrol et
                    if (line.contains("{")) {
                        // Eğer "{" işareti bu satırda varsa, fonksiyon sayısını artır
                    	fonksiyonSayisi++;
                    } else {
                        // Satırda "{" işareti yoksa, bir sonraki satırı oku
                        String nextLine = reader.readLine();
                        if (nextLine != null && nextLine.trim().equals("{")) {
                            // Eğer bir sonraki satırda "{" işareti varsa, fonksiyon sayısını artır
                        	fonksiyonSayisi++;
                        }
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Yorum sapma yüzdesini hesapla
        double YG = ((double) (javadocSatiri + digerYorumSatirlari) * 0.8) / fonksiyonSayisi;
        double YH = ((double) kodSatirSayisi / fonksiyonSayisi) * 0.3;
        double sapma = ((100 * YG) / YH) - 100;

        // Analiz sonuçlarını ekrana yazdır
        System.out.println("Dosya adi: " + file.getName());
        System.out.println("Javadoc sayisi: " + javadocSatiri);
        System.out.println("Yorum: " + digerYorumSatirlari);
        System.out.println("Kod satir sayisi: " + kodSatirSayisi);
        System.out.println("LOC: " + toplamSatirSayisi);
        System.out.println("Function Count: " + fonksiyonSayisi);
        System.out.println("Yorum sapması: %" + sapma);
        System.out.println("-----------------------------------");
        
    }


}
