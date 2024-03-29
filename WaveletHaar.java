import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import java.io.IOException;

public class WaveletHaar implements PlugInFilter {

    ImagePlus reference;        // Reference image
    int level;                  // Wavelet decoposition level

    // implementação do metodo setup da classe abstrata PlugInFilter
    // ete método recebe uma imagem (que esta aberta no momento) e então a converte para 8 bits em nivel de cinza

    public int setup(String arg, ImagePlus imp) {
        reference = imp;
        ImageConverter ic = new ImageConverter(imp);
        ic.convertToGray8();
        return DOES_ALL;
    }

    // implementação do metodo run da classe abstrata PlugInFilter
    // neste método o usuário abre uma pasta de interesse e é retornado o caminho do diretório
    public void run(ImageProcessor img) {

        // instancia uma janela no imageJ escrita "Entre com o numero"
        GenericDialog gd = new GenericDialog("Entre com o numero", IJ.getInstance());
        // adiciona um campo na janela escrito Numero de decomposicao de wavelets:
        gd.addNumericField("Numero de decomposicao de wavelets:", 1, 0);
        // mostra a janela
        gd.showDialog();

        // retorna a funcao se for cancelada
        if (gd.wasCanceled())
            return;

        // pega o parametro level da janela
        level = (int) gd.getNextNumber();

        // abre uma janela para o ususario escolher um diretorio
        SaveDialog sd = new SaveDialog("Abra uma pasta...", "pode ser qualquer nome :D", "");
        // se o diretorio for NULL retorna
        if (sd.getFileName()==null) return;

        // pega o caminho do diretorio
        String dir = sd.getDirectory();

        // chama search para o caminho de diretorio
        search(dir);
    }

    //  neste método é passado um caminho de diretório e então procura-se por todas as imagems no diretorio e por fim efetuado o wavelets para todas as imagens pertencentes a esta pasta
    public void search(String dir) {

        // separa as palavras do caminho do diretorio
        if (!dir.endsWith(File.separator))
            dir += File.separator;

        // instancia uma lista que ira contem cada palavra do caminho do diretorio
        String[] list = new File(dir).list();  /* lista de arquivos */

        // se o caminho não existir retorna
        if (list==null) return;

        // para cada arquivo no diretorio
        for (int i=0; i<list.length; i++) {

            IJ.showStatus(i+"/"+list.length+": "+list[i]);   /* mostra na interface */
            IJ.showProgress((double)i / list.length);  /* barra de progresso */
            // instancia um novo arquivo da pasta
            File f = new File(dir+list[i]);
            if (!f.isDirectory()) {

                // tenta instanciar uma imagem
                ImagePlus image = new Opener().openImage(dir, list[i]); /* abre imagem image */
                // se for possivel chama o método doHar
                if (image != null) {

                    ImageAccess output = DoHaar(new ImageAccess(image.getProcessor()), level, list);

                }
            }
        }

        IJ.showProgress(1.0);
        IJ.showStatus("");
    }

    // faz a wavelet haar
    static public ImageAccess DoHaar (ImageAccess input, int level, String[] nome){
        // ainda deve ser colocado como um parametro o numero de interações que o usuario deseja

        int nx = input.getWidth();      // quantidade de linhas
        int ny = input.getHeight();     // quantidade de colunas
        int counter = 0;                // contador para interações
        int tam = 4 + 3 * (level - 1);	// quantidade de imagens geradas
        double[] entropias = new double[nome.length * tam];	// vetor para energias
        double[] energias = new double[nome.length * tam];	// vetor para entropias
        ImageAccess imgParcial = new ImageAccess(nx,ny);	// imagem usada durante as transformações
        ImageAccess imgFinal = new ImageAccess(nx,ny);		// imagem final (pos transformações)
        ImageAccess imagens[] = new ImageAccess[tam];		//

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
            counter++;			// aumenta o counter para ver quantas vezes dividir a imagem (counter para level)
            input = imgFinal;	// input vira a imagem final formada
            //divide o tamanho da imagem atual na metade
            nx = halfX;
            ny = halfY;

        }

        // separando imagens
        imagens = Sep(imgFinal, level);

            for (int j=0;j<tam;j++ ) {
	            entropias[j] = entropy(imagens[j]);	// vetor com as entropias
	            energias[j] = energy(imagens[j]);	// vetor com as energias

            }
            // normalizacao
            entropias = Normalization(entropias);
            energias = Normalization(energias);

        try{
        	// crio um arquivo do tipo txt chamado arq chamado Haar (Energia e Entropia)
        	FileWriter arq = new FileWriter("Haar (Energia e Entropia).txt");
        	// printa as representacoes formatas dos objetos para um output de texto
            PrintWriter gravarArq = new PrintWriter(arq);

            for (int i=0; i<nome.length; i++) {
                    gravarArq.printf(nome[i] + "\n");				// nome do arquivo utilizado
                    gravarArq.printf("Energia e Entropia [  ");		// escreve "Energia e Entropia ["

                for (int j=0; j<tam ;j++ ) {
                	// ecreve os valores de enrgia e entropia para cada vetor de imagens
                    gravarArq.printf(entropias[i * j] + " " + energias[i * j] + " ");
                }
                	// escreve "]"
                    gravarArq.printf("]\n\n");

            }
            // fecha o arquivo da imagem atual e salva o mesmo
            arq.close();
        } catch(IOException e) {
        	System.out.println("erro: " + e);
        }

        return imgFinal;  //retorno a imagem final
    }

    // normaliza o vetor passado como parametro
    public static double[] Normalization(double[] vector){
        double[] output = new double[vector.length]; 	// vetor do mesmo tamanho do vetor passado
        output = vector;								// variavel recebe o vetor passado como parametro
        double aux, max, min;							// variaveis auxiliares
        aux=0;												// variavel para contar o tamanho do vetor
        max=0;												// variavel para achar o maximo
        min=0;												// variavel para achar o minimo
        for(int i=0; i<vector.length; i++) {
            aux = output[i];
            if(max<aux)
                max = aux;
            if(min>aux)
                min = aux;
        }
        for(int i=0; i<vector.length; i++){
            output[i] = (output[i]-min)/(max-min);		// normaliza os resultados
        }
        return output;
    }

    public static double[] GenerateVector(double[] vectorEnergy, double[] vectorEntropy, int vectorLength){
        vectorLength = vectorLength*2;
        double[] output = new double[vectorLength];
        for(int i=0; i<vectorLength; i+=2){
            output[i] = vectorEnergy[i];
            output[i+1] = vectorEntropy[i];
        }
        return output;
    }

    // apresenta a imagem passada como parametro
    public static void mostra(ImageAccess[] img) {

        for(int i=0; i < img.length; i++) {
            (new ImagePlus("Wavelet",img[i].createByteProcessor())).show();
        }

    }

    // calcula a entropia para uma imagem passada como parametro
    public static double entropy(ImageAccess img) {

        int nx = img.getWidth();      	// quantidade de linhas
        int ny = img.getHeight();     	// quantidade de colunas
        double entro = 0.0;				// variavel para calcular a entropia
        double pixel;					// variavel para calcular o pixel

        for (int i=0; i < nx; i++) {
            for (int j=0; j < ny;j++) {
                pixel = img.getPixel(i,j);
                if (pixel > 0)	// pois sera feita a operacao log
                    entro = entro + (double) (Math.log(pixel) * pixel);
            }
        }
        entro = entro*-1;		// a somatoria eh negativa
        return entro;
    }

    // calcula a energia para uma imagem passada como parametro
    public static double energy(ImageAccess input) {

        int nx = input.getWidth();      // quantidade de linhas
        int ny = input.getHeight();     // quantidade de colunas
        double energy = 0.0;			// variavel para calcular a energia
        double pixel;					// variavel para calcular o pixel

        for (int i=0; i < nx; i++) {
            for (int j=0; j < ny;j++) {

                pixel = input.getPixel(i,j);
                energy = energy + Math.pow(pixel,2);
            }
        }

        return energy;
    }

    // separa a imagem passada como parametro dependendo do numero de interacoes
    static public ImageAccess[] Sep(ImageAccess input, int interations){
        int nx = input.getWidth();	// largura
        int ny = input.getHeight();	// altura
        double pixel = 0.0;			// variavel para capturar o valor do pixel
        ImageAccess[] minhasIMG = new ImageAccess[4 + 3 * (interations - 1) ];	// vetor de imagens para armazenar as imagens separadas

        switch(interations) {

            case 1:

               	minhasIMG[0] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[1] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[2] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[3] = new ImageAccess(nx/2,ny/2);

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

            case 2:
			    minhasIMG[0] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[1] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[2] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[3] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[4] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[5] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[6] = new ImageAccess(nx/2,ny/2);

		        for(int x=0; x<nx; x++){
		            for(int y=0; y<ny; y++){

		                if(x<nx/4 && y<ny/4){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[0].putPixel(x, y, pixel);
		                }

		                if(x>=nx/4 && y<ny/4 && x<nx/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[1].putPixel(x-nx/4, y, pixel);
		                }

		                if(x<nx/4 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[2].putPixel(x, y-ny/4, pixel);
		                }

		                if(x>=nx/4 && y>=ny/4 && x<nx/2 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[3].putPixel(x-nx/4, y-ny/4, pixel);
		                }

		                if(x>=nx/2 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[4].putPixel(x-nx/2, y, pixel);
		                }

		                if(x<nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[5].putPixel(x, y-ny/2, pixel);
		                }

		                if(x>=nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[6].putPixel(x-nx/2, y-ny/2, pixel);
		                }
		            }
		        }
		    	break;

		    case 3:
			    minhasIMG[0] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[1] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[2] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[3] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[4] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[5] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[6] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[7] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[8] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[9] = new ImageAccess(nx/2,ny/2);

		        for(int x=0; x<nx; x++){
		            for(int y=0; y<ny; y++){

		                if(x<nx/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[0].putPixel(x, y, pixel);
		                }

		                if(x>nx/8 && x<nx/4 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[1].putPixel(x-nx/8, y, pixel);
		                }

		                if(x<nx/8 && y<ny/4 && y>ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[2].putPixel(x, y-ny/8, pixel);
		                }

		                if(x>nx/8 && x<nx/4 && y>ny/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[3].putPixel(x-nx/8, y-ny/8, pixel);
		                }

		                if(x>=nx/4 && y<ny/4 && x<nx/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[4].putPixel(x-nx/4, y, pixel);
		                }

		                if(x<nx/4 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[5].putPixel(x, y-ny/4, pixel);
		                }

		                if(x>=nx/4 && x<nx/2 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[6].putPixel(x-nx/4, y-ny/4, pixel);
		                }

		                if(x>=nx/2 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[7].putPixel(x-nx/2, y, pixel);
		                }

		                if(x<nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[8].putPixel(x, y-ny/2, pixel);
		                }

		                if(x>=nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[9].putPixel(x-nx/2, y-ny/2, pixel);
		                }
		            }
		        }
		    	break;

		    	case 4:
			    minhasIMG[0] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[1] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[2] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[3] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[4] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[5] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[6] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[7] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[8] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[9] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[10] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[11] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[12] = new ImageAccess(nx/2,ny/2);

		        for(int x=0; x<nx; x++){
		            for(int y=0; y<ny; y++){

		            	if(x<nx/16 && y<ny/16){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[0].putPixel(x, y, pixel);
		                }

		                if(x>nx/16 && x<nx/8 && y<ny/16){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[1].putPixel(x, y, pixel);
		                }

		                if(x<nx/16 && y>ny/16 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[2].putPixel(x-nx/8, y, pixel);
		                }

		                if(x>nx/16 && y>ny/16 && x<nx/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[3].putPixel(x, y-ny/8, pixel);
		                }

		                 if(x>nx/8 && x<nx/4 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[4].putPixel(x-nx/8, y, pixel);
		                }

		                if(x<nx/8 && y<ny/4 && y>ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[5].putPixel(x, y-ny/8, pixel);
		                }

		                if(x>nx/8 && x<nx/4 && y>ny/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[6].putPixel(x-nx/8, y-ny/8, pixel);
		                }

		                if(x>=nx/4 && y<ny/4 && x<nx/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[7].putPixel(x-nx/4, y, pixel);
		                }

		                if(x<nx/4 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[8].putPixel(x, y-ny/4, pixel);
		                }

		                if(x>=nx/4 && x<nx/2 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[9].putPixel(x-nx/4, y-ny/4, pixel);
		                }

		                if(x>=nx/2 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[10].putPixel(x-nx/2, y, pixel);
		                }

		                if(x<nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[11].putPixel(x, y-ny/2, pixel);
		                }

		                if(x>=nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[12].putPixel(x-nx/2, y-ny/2, pixel);
		                }
		            }
		        }
		    	break;

		    	case 5:
		    	minhasIMG[0] = new ImageAccess(nx/32,ny/32);
		    	minhasIMG[1] = new ImageAccess(nx/32,ny/32);
		    	minhasIMG[2] = new ImageAccess(nx/32,ny/32);
			    minhasIMG[3] = new ImageAccess(nx/32,ny/32);
        		minhasIMG[4] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[5] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[6] = new ImageAccess(nx/16,ny/16);
        		minhasIMG[7] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[8] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[9] = new ImageAccess(nx/8,ny/8);
        		minhasIMG[10] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[11] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[12] = new ImageAccess(nx/4,ny/4);
        		minhasIMG[13] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[14] = new ImageAccess(nx/2,ny/2);
        		minhasIMG[15] = new ImageAccess(nx/2,ny/2);

		        for(int x=0; x<nx; x++){
		            for(int y=0; y<ny; y++){

		            	if(x<nx/32 && y<ny/32){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[0].putPixel(x, y, pixel);
		                }

		                if(x<nx/16 && y<ny/32 && x>nx/32){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[1].putPixel(x, y, pixel);
		                }

		                if(y>ny/32 && x<nx/16 && y<ny/32){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[2].putPixel(x, y, pixel);
		                }

		                if(x>nx/32 && x<nx/16 && y>ny/32 && y<ny/16){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[3].putPixel(x-nx/8, y, pixel);
		                }

		                if(x>nx/16 && x<nx/8 && y<ny/16){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[4].putPixel(x, y, pixel);
		                }

		                if(x<nx/16 && y>ny/16 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[5].putPixel(x-nx/8, y, pixel);
		                }

		                if(x>nx/16 && y>ny/16 && x<nx/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[6].putPixel(x, y-ny/8, pixel);
		                }

		                 if(x>nx/8 && x<nx/4 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[7].putPixel(x-nx/8, y, pixel);
		                }

		                if(x<nx/8 && y<ny/4 && y>ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[8].putPixel(x, y-ny/8, pixel);
		                }

		                if(x>nx/8 && x<nx/4 && y>ny/8 && y<ny/8){
		                	pixel = input.getPixel(x,y);
		                    minhasIMG[9].putPixel(x-nx/8, y-ny/8, pixel);
		                }

		                if(x>=nx/4 && y<ny/4 && x<nx/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[10].putPixel(x-nx/4, y, pixel);
		                }

		                if(x<nx/4 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[11].putPixel(x, y-ny/4, pixel);
		                }

		                if(x>=nx/4 && x<nx/2 && y>=ny/4 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[12].putPixel(x-nx/4, y-ny/4, pixel);
		                }

		                if(x>=nx/2 && y<ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[13].putPixel(x-nx/2, y, pixel);
		                }

		                if(x<nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[14].putPixel(x, y-ny/2, pixel);
		                }

		                if(x>=nx/2 && y>=ny/2){
		                    pixel = input.getPixel(x,y);
		                    minhasIMG[15].putPixel(x-nx/2, y-ny/2, pixel);
		                }
		            }
		        }
		    	break;



            }
            return minhasIMG;
        }

}
