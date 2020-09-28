package projekt_SO;

public class Disk {
public static byte[] DISK;
public static final int blocksize = 32;
public static int [] bitmap;

public Disk () {

	DISK = new byte[1024];// 1 block has 32 bytes
	for(int i=0; i< DISK.length; i++)
		DISK[i] = (byte)0;
	
	bitmap = new int[32]; // is even to amount of blocks in the system
	for(int i=0; i< bitmap.length; i++)
		bitmap[i] = 1;
} 
public int writetoblock(int n, int a, byte[] data) {
	
	for(int i = n*blocksize; i < (n+1)*blocksize; i++, a++) {
		DISK[i] = data[a];
		if(a+1 >= data.length) {
		return a+1;}
	}
	
	return a;
}
public void writetoindexblock (int n, int a) {
	for(int i = n*blocksize; i < (n+1)*blocksize; i++) {
		if(DISK[i] == (byte)0) {
			DISK[i] = (byte)a;
			break;
			}
			
			
	}
	
}


public int appendblock(int n, int pom, byte[] data) {
	
	if(n >= 0) {
	for(int i = n*blocksize; i < (n+1)*blocksize; i++) {
		if(DISK[i] == (byte)0) {
			DISK[i] = data[pom];
			pom++;}
			if(pom+1 > data.length) return pom;
			//System.out.println(pom);
			//System.out.println(data.length);
	}}
	return pom;
}


public byte[] readblock(int n, int count) {
	byte[] data;
	int pom = 0;
	if(freeblockspace(n)==-1 && count >= 32){ 
		data = new byte[blocksize];}
	else if(freeblockspace(n) - (n*blocksize) <= count && freeblockspace(n) - (n*blocksize) >= 0) {
	
	data = new byte[freeblockspace(n)-(n*blocksize) ];}
	else {
		data = new byte[count];}
	for(int i = n*blocksize; pom < data.length; i++, pom++) {
		data[pom] = DISK[i];

		if(pom+1 >= data.length) return data;
	}
	return data;
}

public byte[] readpartofblock(int n, int count, int pointer) {
	byte[] data;
	int pom = 0;
	int pom2 = 0;
	byte [] data2; 
	
	if(count >= ((n+1)*32)-pointer && (((n+1)*32)-pointer)>=0) {
		 data2 = new byte[((n+1)*32)-pointer];
	}
	else if(count > freeblockspace(n)- pointer && freeblockspace(n) != -1) {
		data2 = new byte[freeblockspace(n)- pointer];
	}
	else if(count > freeblockspace(n)- pointer && freeblockspace(n) == -1) {
		data2 = new byte[((n+1)*32)- pointer];
	}
	else data2 = new byte[count];
	
	for(int i = 0; i < data2.length; i++) {
		data2[i] = DISK[pointer+i];
	}

	return data2;
}
public void clearblock(int n) {
	for(int i = n*32; i<(n+1)*32;i++) {
		DISK[i] = (byte)0;
	}
	bitmap[n] = 1;
}
public void clearfirstblock(int n) {
	for(int i = n*32; i<(n+1)*32;i++) {
		DISK[i] = (byte)0;
	}
}
	public int assign_freeblock() {
		for(int i=0; i<bitmap.length; i++)
			if(bitmap[i] == 1) {
				bitmap[i] = 0;
				return i;}
		return -1;
	}
	public int freeblockspace(int n) { //returns first blank byte in the block
		for(int i = n*blocksize; i < (n+1)*blocksize; i++) {
			if(DISK[i] == ((byte)0)) return i;
		}
		return -1;
	}
	public int readbyte(int n) {
		return (int)DISK[n];
	}
	public void stan_dysku() {
		int check = 0;
		for(int i=0; i < 32; i++) {
			if(bitmap[i] == 1) {
				check++;
			}}
			System.out.println("Ilosc wolnych blokow dysku: " + check);
			System.out.print("Wolne bloki dysku: ");
			for(int i=0; i < 32; i++) {
				if(bitmap[i] == 1) {
					System.out.print(i + " ");
				}}
			System.out.println("");
		}
	}


