package Gartic;

import java.util.ArrayList;

/**
 *
 * @author douglaskihara
 */
public class Desenho {

    private int x1, y1, x2, y2, r, g, b;
    private ArrayList<Desenho> listaPixeis = new ArrayList<>();

    public Desenho(int x1, int y1, int x2, int y2, int r, int g, int b) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public int getB() {
        return b;
    }

    public ArrayList<Desenho> adicionaPixelLista(Desenho pixel) {
        boolean sobreposto = false;
        for (Desenho pixelDaLista : listaPixeis) {
            //se já tiver um pixel na mesma posição dentro da lista, só troca o rgb dele
            if (pixelDaLista.x1 == pixel.x1 && pixelDaLista.y1 == pixel.y1
                    && pixelDaLista.x2 == pixel.x2 && pixelDaLista.y2 == pixel.y2) {
                pixelDaLista.r = pixel.r;
                pixelDaLista.g = pixel.g;
                pixelDaLista.b = pixel.b;
                sobreposto = true;
                break;
            }
        }
        //se não foi encontrado nenhum pixel na posição (x,y), então coloca na lista
        if (!sobreposto) {
            listaPixeis.add(pixel);
        }
        return listaPixeis;
    }

    @Override
    public String toString() {
        return x1 + ";" + y1 + ";" + x2 + ";" + y2 + "@" + r + ";" + g + ";" + b;
    }
}
