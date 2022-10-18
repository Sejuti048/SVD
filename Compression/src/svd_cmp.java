import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Scanner;

public class svd_cmp {

	public svd_cmp() {
		// TODO Auto-generated constructor stub
	}
	public static byte[] readByteBlock(InputStream in, int offset, int noBytes) throws IOException {
            byte[] result = new byte[noBytes];
            in.read(result, offset, noBytes);
            return result;
        }
	public static float toFloat( int hbits )
	{
	    int mant = hbits & 0x03ff;            // 10 bits mantissa
	    int exp =  hbits & 0x7c00;            // 5 bits exponent
	    if( exp == 0x7c00 )                   // NaN/Inf
	        exp = 0x3fc00;                    // -> NaN/Inf
	    else if( exp != 0 )                   // normalized value
	    {
	        exp += 0x1c000;                   // exp - 15 + 127
	        if( mant == 0 && exp > 0x1c400 )  // smooth transition
	            return Float.intBitsToFloat( ( hbits & 0x8000 ) << 16
	                                            | exp << 13 | 0x3ff );
	    }
	    else if( mant != 0 )                  // && exp==0 -> subnormal
	    {
	        exp = 0x1c400;                    // make it normal
	        do {
	            mant <<= 1;                   // mantissa * 2
	            exp -= 0x400;                 // decrease exp by 1
	        } while( ( mant & 0x400 ) == 0 ); // while not normal
	        mant &= 0x3ff;                    // discard subnormal bit
	    }                                     // else +/-0 -> +/-0
	    return Float.intBitsToFloat(          // combine all parts
	        ( hbits & 0x8000 ) << 16          // sign  << ( 31 - 15 )
	        | ( exp | mant ) << 13 );         // value << ( 23 - 10 )
	}

	// returns all higher 16 bits as 0 for all results
	public static int fromFloat( float fval )
	{
	    int fbits = Float.floatToIntBits( fval );
	    int sign = fbits >>> 16 & 0x8000;          // sign only
	    int val = ( fbits & 0x7fffffff ) + 0x1000; // rounded value

	    if( val >= 0x47800000 )               // might be or become NaN/Inf
	    {                                     // avoid Inf due to rounding
	        if( ( fbits & 0x7fffffff ) >= 0x47800000 )
	        {                                 // is or must become NaN/Inf
	            if( val < 0x7f800000 )        // was value but too large
	                return sign | 0x7c00;     // make it +/-Inf
	            return sign | 0x7c00 |        // remains +/-Inf or NaN
	                ( fbits & 0x007fffff ) >>> 13; // keep NaN (and Inf) bits
	        }
	        return sign | 0x7bff;             // unrounded not quite Inf
	    }
	    if( val >= 0x38800000 )               // remains normalized value
	        return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
	    if( val < 0x33000000 )                // too small for subnormal
	        return sign;                      // becomes +/-0
	    val = ( fbits & 0x7fffffff ) >>> 23;  // tmp exp for subnormal calc
	    return sign | ( ( fbits & 0x7fffff | 0x800000 ) // add subnormal bit
	         + ( 0x800000 >>> val - 102 )     // round depending on cut off
	      >>> 126 - val );   // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
	}


	public static void main(String[] args) throws IOException {

		int k=107;
		File f= new File("header.txt");
		Scanner sc = new Scanner(f);
		int width, height, grayscale;
		width=sc.nextInt();
		height=sc.nextInt();
		grayscale=sc.nextInt();
		System.out.println("w: "+width+" h: "+height+" c: "+grayscale);
		sc.close();

		File mf=new File("image_b.pgm.SVD");
		if (!mf.exists()) {
            try {
				mf.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        FileOutputStream fos = new FileOutputStream("image_k.compression.txt");
        OutputStreamWriter osw=new OutputStreamWriter(fos,"UTF-8");
        BufferedWriter outb=new BufferedWriter(osw);

	OutputStream opStream = null;
        opStream = new FileOutputStream(mf);
        byte[] byteContent = "P2".getBytes();
        opStream.write(byteContent);
        String lineSeparator = System.getProperty("line.separator");
        opStream.write(lineSeparator.getBytes());
        outb.write("P2");
        //outb.write(lineSeparator);

        String strContent = String.valueOf(width);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(' ');
        outb.write(strContent);
        //outb.write(' ');

        strContent = String.valueOf(height);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(' ');
        outb.write(strContent);
        //outb.write(lineSeparator);

        strContent = String.valueOf(grayscale);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(lineSeparator.getBytes());
        outb.write(strContent);
        //(lineSeparator);

        strContent = String.valueOf(k);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(lineSeparator.getBytes());
        outb.write(strContent);
        //outb.write(lineSeparator);

	f=new File("SVD.txt");
		/*Scanner in=new Scanner(f);

		int words=0;
		while(in.hasNext())
        {
            in.next();
            words++;
        }

		System.out.println("no of floats= "+words);
		PrintStream o = new PrintStream(new File("A.txt"));

	     // Store current System.out before assigning a new value


	     // Assign o to output stream
	     System.setOut(o); */
	     sc=new Scanner(f);
	     //int count=0;
	     //String s;
		//System.out.println(sc.nextFloat());

		short[][] arrU= new short[height][height]; //in
                //float[][] arU= new float[height][height];
		for (int i=0;i<height;i++) {
                    for(int j=0;j<height;j++) {
				//s=sc.next();

			float aij_f=sc.nextFloat(); //in
		//	arU
				//System.out.println("i "+i+" j "+j+" float "+aij_f);
			int aij_int=fromFloat(aij_f); //in
			arrU[i][j]=(short)aij_int; //in
				//arrU[i][j]=sc.nextFloat();
				//count++;
						/*strContent = String.valueOf(aij_int);
				        byteContent=strContent.getBytes();
				        opStream.write(byteContent);
				        opStream.write(lineSeparator.getBytes());*/
                            }

			}//opStream.write(lineSeparator.getBytes());

                    short[][] arrS= new short[height][width]; //in
		//float[][] arrS= new float[height][width];
                    for (int i=0;i<height;i++) {
			for(int j=0;j<width;j++) {
				//s=sc.next();
                            float aij_f=sc.nextFloat(); //in

                            int aij_int=fromFloat(aij_f); //in
                            arrS[i][j]=(short)aij_int; //in
				//arrS[i][j]=sc.nextFloat();
				//count++;
				//System.out.println("i "+i+" j "+j+" float "+aij_f+" c: "+count);
						/*strContent = String.valueOf(aij_int);
				        byteContent=strContent.getBytes();
				        opStream.write(byteContent);
				        opStream.write(lineSeparator.getBytes());*/
                            }

                        }
		//float [][] arrV= new float[width][width];
		//float [][] arrVT= new float[width][width];
                    short[][] arrV= new short[width][width]; //in
		//int [][] arrVT=new int[width][width]; //in
                    for (int i=0;i<width;i++) {
			for(int j=0;j<width;j++) {
				//s=sc.next();
                            float aij_f=sc.nextFloat();; //in
				//System.out.println("i "+i+" j "+j+" float "+aij_f);
                            int aij_int=fromFloat(aij_f); //in
                            arrV[i][j]=(short)aij_int; //in
				//arrV[i][j]=sc.nextFloat();

				//count++;
						/*strContent = String.valueOf(aij_int);
				        byteContent=strContent.getBytes();
				        opStream.write(byteContent);
				        opStream.write(lineSeparator.getBytes());*/
                        }

                    }
		sc.close();
		//PrintStream Console=System.out;
		//System.setOut(Console);
		/*for (int i=0;i<width;i++) {
			for(int j=0;j<width;j++) {
				arrVT[i][j]=arrV[j][i];

			}
		}
		*/
		//ByteBuffer buf = ByteBuffer.allocate(10);
		/*PrintStream o1=new PrintStream(new File("C:\\Users\\sb343\\Pictures\\imm.txt"));
        System.setOut(o1);
        System.out.println("P2");
        System.out.println(width+" "+height);
        System.out.println(grayscale);
        System.out.println(k);*/

		for(int i=0;i<height;i++) {
                    for(int j=0;j<k;j++) {//k
				//System.out.print(arrU[i][j]+" ");
			strContent = String.valueOf(arrU[i][j]);
		        byteContent=strContent.getBytes();
		        opStream.write(byteContent);
		        opStream.write(' ');
                        outb.write(strContent);
		        //opStream.write(lineSeparator.getBytes());
			}
                    }
                    for(int i=0;i<k;i++) {//k
			for(int j=0;j<k;j++) {//k
				//System.out.print(arrS[i][j]+" ");
                            if(i==j){
                            strContent = String.valueOf(arrS[i][j]);
                            byteContent=strContent.getBytes();
                            opStream.write(byteContent);
                            opStream.write(' ');}
                            if(i==j) outb.write(strContent);
		        //opStream.write(lineSeparator.getBytes());
			}
                    }

                    for(int i=0;i<k;i++) {//k
			for(int j=0;j<width;j++) {
				//System.out.print(arrV[i][j]+" ");
                            strContent = String.valueOf(arrV[i][j]);
                            byteContent=strContent.getBytes();
                            opStream.write(byteContent);
                            opStream.write(' ');
                            outb.write(strContent);
		        //opStream.write(lineSeparator.getBytes());
			}
                    }
                opStream.flush();
                opStream.close();
                outb.flush();
                outb.close();
		//o1.flush();
		//o1.close();

                Scanner sc1=new Scanner(mf);
                String mn=sc1.next();
                System.out.println("mn: "+mn);

                width=sc1.nextInt();
                System.out.println("w: "+width);
                height=sc1.nextInt();
                System.out.println("h: "+height);
                grayscale=sc1.nextInt();
                System.out.println("gs: "+grayscale);
                k=sc1.nextInt();
                System.out.println("k: "+k);
		/*
		File mf1=new File("C:\\Users\\sb343\\Pictures\\img.txt");
		opStream = new FileOutputStream(mf1);
        byteContent = mn.getBytes();
        opStream.write(byteContent);
        lineSeparator = System.getProperty("line.separator");
        opStream.write(lineSeparator.getBytes());

        strContent = String.valueOf(width);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(' ');

        strContent = String.valueOf(height);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(lineSeparator.getBytes());

        strContent = String.valueOf(grayscale);
        byteContent=strContent.getBytes();
        opStream.write(byteContent);
        opStream.write(lineSeparator.getBytes());

        ByteBuffer buf = ByteBuffer.allocate(2048);

        FileInputStream in = new FileInputStream(mf);
        int len = in.getChannel().read(buf);
        RandomAccessFile aFile = new RandomAccessFile
                ("C:\\Users\\sb343\\Pictures\\imm.txt", "r");
        FileChannel inChannel = aFile.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while(inChannel.read(buffer) > 0)
        {
            buffer.flip();
            for (int i = 0; i < buffer.limit(); i++)
            {
                System.out.print((char) buffer.get());
            }
            buffer.clear(); // do something with the data and clear/compact it.
        }*/
        //byte b=(byte) sc1.nextByte(10);
        //int ff= b;
        //System.out.println("b "+b+" ff "+ff);

		float[][] arr_fU=new float[height][k]; //change 2nd ht to k
		float[][] arr_fS=new float[k][k]; //change both to k
		float[][] arr_fVT=new float[k][width]; //change 1st wi to k
		int ff=0;
		for(int i=0;i<height;i++) {
                    for(int j=0;j<k;j++){ //k
				//arr_fU[i][j]=sc1.nextFloat();
                        ff=(int)sc1.nextInt(); //in
                        arr_fU[i][j]=toFloat(ff); //in
		        //opStream.write(lineSeparator.getBytes());
                    }
                }
		for(int i=0;i<k;i++) {//k
                    for(int j=0;j<k;j++) {//k
				//arr_fS[i][j]=sc1.nextFloat();
                        if(i==j){
                        ff=(int)sc1.nextInt(); }
                        else ff=0;//in
                        arr_fS[i][j]=toFloat(ff); //in
		        //opStream.write(lineSeparator.getBytes());
                    }
		}

		for(int i=0;i<k;i++) {//k
                    for(int j=0;j<width;j++) {
				//arr_fVT[i][j]=sc1.nextFloat();
			ff=(int)sc1.nextInt(); //in
			arr_fVT[i][j]=toFloat(ff); //in
		        //opStream.write(lineSeparator.getBytes());
                    }
		}
		float[][] product = new float[k][width]; //width na k
		for(int i = 0; i < k; i++) {
                    for (int j = 0; j < width; j++) {//k
                        product[i][j]=(float) 0.0;
                    }
		}
                for(int i = 0; i < k; i++) {
                    for (int j = 0; j < width; j++) {//k thik koro
                        for (int p = 0; p < k; p++) {//k thik koro
                            product[i][j] += arr_fS[i][p] * arr_fVT[p][j];
                        }
                    }
                }

                float[][] A = new float[height][width];
                for(int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        A[i][j]=(float) 0.0;
                    }
                }

                for(int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        for(int p = 0; p < k; p++) {//k thik korte hbe
                            A[i][j] += arr_fU[i][p] * product[p][j];
                        }
                    }
                }
                PrintStream o=new PrintStream(new File("image_k.PGM"));
                System.setOut(o);
                System.out.println(mn);
                System.out.println(width+" "+height);
                System.out.println(grayscale);
                for(int i = 0; i < height; i++) {
                    for(int j = 0; j < width; j++){
                        if(A[i][j]-(int)A[i][j]<0.1) {
        		    System.out.print((int)A[i][j]+" ");
        		}
        		else {
                            System.out.print((int)Math.ceil(A[i][j])+" ");
        		}
        		/*strContent = String.valueOf(A[i][j]);
		        byteContent=strContent.getBytes();
		        opStream.write(byteContent);
		        opStream.write(' ');*/
                    }
                    System.out.println();
                }
                /*
                Scanner sc2=new Scanner(new File("C:\\Users\\user\\Pictures\\Mytest.txt"));
                byte b=sc2.next().getBytes();
                System.out.println("b[0] "+b);
                //b[1]=(byte)sc2.nextByte();
                //String jk=b.toString();
                /*int w=sc2.nextInt();
                int h=sc2.nextInt();
                int gs=sc2.nextInt();
                int k2=sc2.nextInt();

                float[][] arr_fU1=new float[h][k2]; //change 2nd ht to k
		float[][] arr_fS1=new float[k2][k2]; //change both to k
		float[][] arr_fVT1=new float[k2][w]; //change 1st wi to k
		int ff3=0;
		for(int i=0;i<h;i++) {
                    for(int j=0;j<k2;j++){ //k
				//arr_fU[i][j]=sc1.nextFloat();
                        byte[][] bin=new byte[1][2];
                        bin[0][0]=(byte)sc2.nextByte();
                        bin[0][1]=(byte)sc2.nextByte();
                        String sgs=String.valueOf(bin[0][0]);
                        String cat=sgs.concat(String.valueOf(bin[0][1]));
                        ff3=Integer.parseInt(cat);
                        //ff3=(int)sc1.nextInt(); //in
                        arr_fU1[i][j]=toFloat(ff3); //in
		        //opStream.write(lineSeparator.getBytes());
                    }
                }
		for(int i=0;i<k2;i++) {//k
                    for(int j=0;j<k2;j++) {//k
				//arr_fS[i][j]=sc1.nextFloat();
                        if(i==j){
                            byte[][] bin=new byte[1][2];
                        bin[0][0]=sc2.nextByte();
                        bin[0][1]=sc2.nextByte();
                        String sgs=String.valueOf(bin[0][0]);
                        String cat = sgs.concat(String.valueOf(bin[0][1]));
                        ff3=Integer.parseInt(cat);
                        //ff3=(int)sc1.nextInt(); //in
                        arr_fS1[i][j]=toFloat(ff3); //in

                        }
                        else{ //in
                        arr_fS1[i][j]=toFloat(0);
                        }//in
		        //opStream.write(lineSeparator.getBytes());
                    }
		}
		for(int i=0;i<k2;i++) {//k
                    for(int j=0;j<w;j++) {
                        byte[][] bin=new byte[1][2];
                        bin[0][0]=sc2.nextByte();
                        bin[0][1]=sc2.nextByte();
                        String sgs=String.valueOf(bin[0][0]);
                        String cat = sgs.concat(String.valueOf(bin[0][1]));
                        ff3=Integer.parseInt(cat);
                        //ff3=(int)sc1.nextInt(); //in
                        arr_fVT1[i][j]=toFloat(ff3); //in

                        }
				//arr_fVT[i][j]=sc1.nextFloat();
			//ff=(int)sc1.nextInt(); //in
			//arr_fVT1[i][j]=toFloat(ff); //in
		        //opStream.write(lineSeparator.getBytes());
                    //}
		}
		float[][] product1 = new float[k2][w]; //width na k
		for(int i = 0; i < k2; i++) {
                    for (int j = 0; j < w; j++) {//k
                        product[i][j]=(float) 0.0;
                    }
		}
                for(int i = 0; i < k2; i++) {
                    for (int j = 0; j < w; j++) {//k thik koro
                        for (int p = 0; p < k2; p++) {//k thik koro
                            product1[i][j] += arr_fS1[i][p] * arr_fVT1[p][j];
                        }
                    }
                }

                float[][] A1 = new float[h][w];
                for(int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        A1[i][j]=(float) 0.0;
                    }
                }

                for(int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        for(int p = 0; p < k2; p++) {//k thik korte hbe
                            A[i][j] += arr_fU1[i][p] * product1[p][j];
                        }
                    }
                }
                PrintStream o1=new PrintStream(new File("C:\\Users\\user\\Pictures\\img4.PGM"));
                System.setOut(o1);
                System.out.println(jk);
                System.out.println(w+" "+h);
                System.out.println(gs);
                for(int i = 0; i < h; i++) {
                    for(int j = 0; j < w; j++){
                        if(A[i][j]-(int)A[i][j]<0.1) {
        		    System.out.print((int)A1[i][j]+" ");
        		}
        		else {
                            System.out.print((int)Math.ceil(A1[i][j])+" ");
        		}
        		/*strContent = String.valueOf(A[i][j]);
		        byteContent=strContent.getBytes();
		        opStream.write(byteContent);
		        opStream.write(' ');*/
                   // }
                  //  System.out.println();
                //}


                //opStream.flush();
		//opStream.close();
		//float a =(float)1.22;
		//int kj=fromFloat(a);
		//System.out.println("int: "+kj+" f: "+a);

		//System.out.println(toFloat(kj));
		//a=a*(float)44.444;
		//kj=fromFloat(a);
		//System.out.println(toFloat(kj));

    }
}
