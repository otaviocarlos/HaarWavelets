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
}
