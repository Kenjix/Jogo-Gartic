package Gartic;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author dougl
 */
public class Utils {

    public String sortearPalavra() {
        ArrayList<String> palavras = new ArrayList<>();
        String caminhoArquivo = "./temas.txt";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(caminhoArquivo), StandardCharsets.UTF_8));
            String linha;
            while ((linha = br.readLine()) != null) {
                palavras.add(removerAcentuacao(linha));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Random random = new Random();
        int indiceSorteado = random.nextInt(palavras.size());
        return palavras.get(indiceSorteado);
    }

    //remove acentuacao e caracteres especiais
    public String removerAcentuacao(String palavra) {
        //decompoe a string recebida para o codigo unicode no padrao NFD e a transforma em maiuscula
        palavra = Normalizer.normalize(palavra, Normalizer.Form.NFD);

        //remove acentuacoes da string com regex 
        return palavra.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").toUpperCase();
    }
}
