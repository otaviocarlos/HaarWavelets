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

                image.show();

                    // CODIGO
                }
            }
        }
        IJ.showProgress(1.0);
        IJ.showStatus("");
    }

    // ESTÁ É A FUNÇÃO WAVELET HAAR
    static public ImageAccess ImageAccess(ImageAccess input){
        // ainda deve ser colocado como um parametro o numero de interações que o usuario deseja

        int nx = input.getWidth();      // quantidade de linhas
        int ny = input.getHeight();     // quantidade de colunas
        int counter = 0;                // contador para interações
        
        ImageAccess imgParcial = new ImageAccess(nx,ny);    // imagem usada durante as transformações
        ImageAccess imgFinal = new ImageAccess(nx,ny);      // imagem final com as transformações

        while(counter < 1){
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
            for (int j = 0; j<ny; j++){
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
        }
        return imgFinal;    //retorno a imagem final
    }
}
