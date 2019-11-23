import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;

public class WaveletHaar implements PlugInFilter {

    ImagePlus reference;        // Reference image
    int level;                  // Wavelet decoposition level

    public int setup(String arg, ImagePlus imp) {
        reference = imp;
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        return DOES_ALL;
    }

    public void run(ImageProcessor img) {

        GenericDialog gd = new GenericDialog("Entre com o número", IJ.getInstance());
        gd.addNumericField("Número de decomposicao de wavelets:", 1, 0);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        level = (int) gd.getNextNumber();

        SaveDialog sd = new SaveDialog("Abra uma pasta...", "pode ser qualquer nome :D", "");
        if (sd.getFileName()==null) return;
        String dir = sd.getDirectory();

        search(dir);
    }

    public void search(String dir) {

        if (!dir.endsWith(File.separator))
            dir += File.separator;

        String[] list = new File(dir).list();  /* lista de arquivos */

        if (list==null) return;

        for (int i=0; i<list.length; i++) {

            IJ.showStatus(i+"/"+list.length+": "+list[i]);   /* mostra na interface */
            IJ.showProgress((double)i / list.length);  /* barra de progresso */
            File f = new File(dir+list[i]);
            if (!f.isDirectory()) {
                ImagePlus image = new Opener().openImage(dir, list[i]); /* abre imagem image */
                if (image != null) {

                    // CODIGO
                    ImageAccess output = DoHaar(new ImageAccess(image.getProcessor()), level);
                    (new ImagePlus("Wavelet",output.createByteProcessor())).show();

                }
            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");
    }

    // ESTÁ É A FUNÇÃO WAVELET HAAR
    static public ImageAccess DoHaar(ImageAccess input, int level){
        // ainda deve ser colocado como um parametro o numero de interações que o usuario deseja

        int nx = input.getWidth();      // quantidade de linhas
        int ny = input.getHeight();     // quantidade de colunas
        int counter = 0;                // contador para interações

        ImageAccess imgParcial = new ImageAccess(nx,ny);    // imagem usada durante as transformações
        ImageAccess imgFinal = new ImageAccess(nx,ny);      // imagem final com as transformações

        ImageAccess imagens[] = new ImageAccess[4];
        int tam = 4 + 3 * (level - 1);
        ImageAccess todasIMG[] = new ImageAccess[tam];

        while(counter < level){
            int halfX = nx/2;   // conta metade da imagem no eixo x para separar a op1 e a op2
            int halfY = ny/2;   // conta metade da imagem no eixo y para separar a op1 e a op2
            // para cada linha
            for (int i = 0; i<nx; i++){
                int auxColumn = 0;  // auxiliar para contar colunas ja que vou rodar a cada duas colunas
                for (int j=0; j<ny; j+=2){
                    double pixel1 = input.getPixel(i,j);    // pega o primeiro pixel (da linha i e coluna j)
                    double pixel2 = input.getPixel(i,j+1);  // pega o primeiro pixel (da linha i e coluna j+1)
                    // Operações
                    double op1 = (pixel1+pixel2)/2; // média da soma
                    double op2 = (pixel1-pixel2)/2; // média da diferença
                    // Imagens resultantes
                    imgParcial.putPixel(i, auxColumn, op1);         // coloca o pixel da soma
                    imgParcial.putPixel(i, auxColumn+halfY, op2);   // coloca o pixel da diferença
                    auxColumn++;    // passo o auxiliar para a proxima coluna
                }
            }

            // para cada coluna
            for (int j=0; j<ny; j++){
                int auxRow = 0; // auxiliar para contar linhas ja que vou rodar a cada duas linhas
                for (int i=0; i<ny; i+=2){
                    double pixel1 = imgParcial.getPixel(i,j);   // pega o primeiro pixel (da coluna j e linha i)
                    double pixel2 = imgParcial.getPixel(i+1,j); // pega o primeiro pixel (da coluna j e linha i+1)
                    // Operações
                    double op1 = (pixel1+pixel2)/2; // média da soma
                    double op2 = (pixel1-pixel2)/2; // média da diferença
                    // Imagens resultantes
                    imgFinal.putPixel(auxRow, j, op1);          // coloca o pixel da soma
                    imgFinal.putPixel(auxRow+halfX, j, op2);    // coloca o pixel da diferença
                    auxRow++;   // passo o auxiliar para a proxima linha
                }
            }
            counter++;
            input = imgFinal;
            nx = imgFinal.getWidth()/2;
            ny = imgFinal.getHeight()/2;

            // for (int i=0; i<level; i++) {

            //     for (int j=0; j<4; j++) {

            //         imagens = Sep(imgFinal);
            //         todasIMG[tam - j] = imagens[3 - j];
            //         imagens = Sep(imagens[0]);
            //     }
            // }

        }

        imagens = Sep(imgFinal, level);
        mostra(imagens);
        // todasIMG = Sep(imgFinal, level);
        // (new ImagePlus("Wavelet",todasIMG[0].createByteProcessor())).show();
        // (new ImagePlus("Wavelet",todasIMG[1].createByteProcessor())).show();
        // (new ImagePlus("Wavelet",todasIMG[2].createByteProcessor())).show();
        // (new ImagePlus("Wavelet",todasIMG[3].createByteProcessor())).show();

        return imgFinal;  //retorno a imagem final
    }


    public static void mostra(ImageAccess[] img) {

        for(int i=0; i < img.length; i++) {
            (new ImagePlus("Wavelet",img[i].createByteProcessor())).show();
        }

    }

    public static double entropy(ImageAccess img, int level) {

        int nx = img.getWidth();      // quantidade de linhas
        int ny = img.getHeight();     // quantidade de colunas
        double entro = 0.0; // vetor

        double pixel;

        for (int i=0; i < nx; i++) {

            for (int j=0; j < ny;j++) {

                pixel = img.getPixel(i,j);
                if (pixel > 0)
                    entro = entro + (double) (Math.log(pixel) * pixel);

            }
        }

        IJ.write(String.valueOf(entro));

        return entro;
    }

    static public ImageAccess[] Sep(ImageAccess input, int interations){
        int nx = input.getWidth();
        int ny = input.getHeight();
        double pixel = 0.0;
        ImageAccess[] minhasIMG = new ImageAccess[4 + 3 * (interations - 1) ];
        minhasIMG[0] = new ImageAccess(nx/2,ny/2);
        minhasIMG[1] = new ImageAccess(nx/2,ny/2);
        minhasIMG[2] = new ImageAccess(nx/2,ny/2);
        minhasIMG[3] = new ImageAccess(nx/2,ny/2);
        switch(interations) {

            case 1:

                for(int x=0; x<nx; x++){
                    for(int y=0; y<ny; y++){

                        if(x<nx/2 && y<ny/2){
                            pixel = input.getPixel(x,y);
                            minhasIMG[0].putPixel(x, y, pixel);
                        }

                        if(x>nx/2 && y<ny/2){
                            pixel = input.getPixel(x,y);
                            minhasIMG[1].putPixel(x-nx/2, y, pixel);
                        }

                        if(x<nx/2 && y>=ny/2){
                            pixel = input.getPixel(x,y);
                            minhasIMG[2].putPixel(x, y-ny/2, pixel);
                        }

                        if(x>=nx/2 && y>=ny/2){
                            pixel = input.getPixel(x,y);
                            minhasIMG[3].putPixel(x-nx/2, y-ny/2, pixel);
                        }
                    }
                }
                break;

            }
            return minhasIMG;
        }

            // case 2:
            //     ImageAccess new1 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new2 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new3 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new4 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new5 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new6 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new7 = new ImageAccess(nx/2,ny/2);

            //     for(int x=0; x<nx; x++){
            //         for(int y=0; y<ny; y++){

            //             if(x<nx/4 && y<ny/4){
            //                 pixel = input.getPixel(x,y);
            //                 new1.putPixel(x, y, pixel);
            //             }

            //             if(x>=nx/4 && y<ny/4 && x<nx/2){
            //                 pixel = input.getPixel(x,y);
            //                 new2.putPixel(x-nx/4, y, pixel);
            //             }

            //             if(x<nx/4 && y>=ny/4 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new3.putPixel(x, y-ny/4, pixel);
            //             }

            //             if(x>=nx/4 && y>=ny/4 && x<nx/2 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new4.putPixel(x-nx/4, y-ny/4, pixel);
            //             }

            //             if(x>=nx/2 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new5.putPixel(x-nx/2, y, pixel);
            //             }

            //             if(x<nx/2 && y>=ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new6.putPixel(x, y-ny/2, pixel);
            //             }

            //             if(x>=nx/2 && y>=ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new7.putPixel(x-nx/2, y-ny/2, pixel);
            //             }
            //         }
            //     }
            // break;

            // case 3:
            //     ImageAccess new1 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new2 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new3 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new4 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new5 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new6 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new7 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new8 = new ImageAccess(nx/2,ny/2);
            //     ImageAccess new9= new ImageAccess(nx/2,ny/2);
            //     ImageAccess new10 = new ImageAccess(nx/2,ny/2);

            //     for(int x=0; x<nx; x++){
            //         for(int y=0; y<ny; y++){

            //             if(x<nx/8 && y<ny/8){
            //                 pixel = input.getPixel(x,y);
            //                 new1.putPixel(x, y, pixel);
            //             }

            //             if(x>nx/8 && x<nx/4 && y<ny/8){
            //                 pixel = input.getPixel(x,y);
            //                 new2.putPixel(x-nx/8, y, pixel);
            //             }

            //             if(x>nx/8 && y<ny/4 && y>ny/8){
            //                 pixel = input.getPixel(x,y);
            //                 new3.putPixel(x, y-ny/8, pixel);
            //             }

            //             if(x>nx/8 && x<nx/4 && y>ny/8 && y<ny/8){
            //                 pixel = input.getPixel(x,y);
            //                 new4.putPixel(x-nx/8, y-ny/8, pixel);
            //             }

            //             if(x>=nx/4 && y<ny/4 && x<nx/2){
            //                 pixel = input.getPixel(x,y);
            //                 new5.putPixel(x-nx/4, y, pixel);
            //             }

            //             if(x<nx/4 && y>=ny/4 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new6.putPixel(x, y-ny/4, pixel);
            //             }

            //             if(x>=nx/4 && y>=ny/4 && x<nx/2 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new7.putPixel(x-nx/4, y-ny/4, pixel);
            //             }

            //             if(x>=nx/2 && y<ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new8.putPixel(x-nx/2, y, pixel);
            //             }

            //             if(x<nx/2 && y>=ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new9.putPixel(x, y-ny/2, pixel);
            //             }

            //             if(x>=nx/2 && y>=ny/2){
            //                 pixel = input.getPixel(x,y);
            //                 new10.putPixel(x-nx/2, y-ny/2, pixel);
            //             }
            //         }
            //     }
            // break;

    public static ImageAccess[] divide4(ImageAccess img){

        int nx = img.getWidth();      // quantidade de linhas
        int ny = img.getHeight();     // quantidade de colunas

        int px = 0; // coordenada em x da primeira interação do pixel
        int py = 0; // coordenada em y da primeira interação do pixel
        int tx = 0; // limite em x da divisão da imagem
        int ty = 0; // limite em y da divisão da imagem

        ImageAccess imagens[] = new ImageAccess[4];

        double pixel;

        for (int i = 0;i<2;i++) {

            for (int j = 0;j<2;j++) {

                px = i * nx/2;
                py = j * ny/2;

                tx = (nx/2 * (px +1));
                ty = (ny/2 * (py +1));
                imagens[i + (2 * j)] = new ImageAccess(nx/2,ny/2);

                for (int u=px; u<px+nx/2;u++) {
                    for (int v=py; v<py+ny/2;v++) {

                        pixel = img.getPixel(u,v);
                        imagens[i+ (2 * j)].putPixel(u-nx,v-ny, pixel);

                    }
                }

            }
        }

        (new ImagePlus("Wavelet",imagens[0].createByteProcessor())).show();
        (new ImagePlus("Wavelet",imagens[1].createByteProcessor())).show();
        (new ImagePlus("Wavelet",imagens[2].createByteProcessor())).show();
        (new ImagePlus("Wavelet",imagens[3].createByteProcessor())).show();

        return imagens;
    }

    // static public ImageAccess[] Sep(ImageAccess input){
    //     int nx = input.getWidth();
    //     int ny = input.getHeight();
    //     double pixel = 0.0;
    //     ImageAccess imagens[] = new ImageAccess[4];

    //     imagens[0] = new ImageAccess(nx/2,ny/2);
    //     imagens[1] = new ImageAccess(nx/2,ny/2);
    //     imagens[2] = new ImageAccess(nx/2,ny/2);
    //     imagens[3] = new ImageAccess(nx/2,ny/2);

    //     for(int x=0; x<nx; x++){
    //         for(int y=0; y<ny; y++){

    //             if(x<nx/2 && y<ny/2){
    //                 pixel = input.getPixel(x,y);
    //                 imagens[0].putPixel(x, y, pixel);
    //             }

    //             if(x>=nx/2 && y<ny/2){
    //                 pixel = input.getPixel(x,y);
    //                 imagens[1].putPixel(x - nx/2, y, pixel);
    //             }

    //             if(x<nx/2 && y>=ny/2){
    //                 pixel = input.getPixel(x,y);
    //                 imagens[2].putPixel(x, y - ny/2, pixel);
    //             }

    //             if(x>=nx/2 && y>=ny/2){
    //                 pixel = input.getPixel(x,y);
    //                 imagens[3].putPixel(x - nx/2, y - ny/2, pixel);
    //             }
    //         }
    //     }

    //     return imagens;
    // }
}
