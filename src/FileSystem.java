package projekt_SO;
import java.io.IOException;
import java.util.*;

//a two-level directory
public class FileSystem {
int currentdir;
public static Hashtable<String,Catalog_Entry> rootdirectory;
public static Vector< Hashtable<String, Catalog_Entry>> directories;
public static INODE[]  fcblist; //list of all i-nodes
public static Disk disk;
public static INODE[] openfiles;//system table of open files
public Vector <Integer> process_openfiles; //process table of open files



	public FileSystem() {
 		rootdirectory = new Hashtable<String,Catalog_Entry>();
 		directories = new Vector<Hashtable<String, Catalog_Entry>> ();
 		directories.add(rootdirectory);
 		currentdir = directories.indexOf(rootdirectory); //starts with rootdirectory
 		fcblist = new INODE[32]; //the maximum file quantity is equal to the number of blocks
 		disk = new Disk();
 		openfiles = new INODE[32];
 		process_openfiles = new Vector<Integer>(); 
 
	}
	
 	private int freefcbindex() {
 		for(int i = 0; i<fcblist.length; i++)
 			if(fcblist[i] == null)
 				return i;
 		return -1;
 	}
 	
	private int openfilesindex() {
 		for(int i = 0; i<openfiles.length; i++)
 			if(openfiles[i] == null)
 				return i;
 		return -1;
 	}
 	 	
	// file management
	
	public boolean createFile(String name) throws FileException {
		
		if(directories.get(currentdir).containsKey(name)) {
			return false;
		}
	
		int pom = 0;
		INODE inode = new INODE();
		inode.size = 0;
		inode.block_index1 = disk.assign_freeblock(); 
		
		if(inode.block_index1 == -1) return false;
		inode.block_index2 = -1;
		for(int i=0;i<3;i++) {
		inode.index_block = -1;}
		
		
		//inode.i_ctime = date.getTime();
		//inode.i_mtime = inode.i_ctime;
		inode.pointer = inode.block_index1*32;
		pom = freefcbindex();
		fcblist[pom] = inode;
		
		Catalog_Entry entry = new Catalog_Entry();
		entry.name = name;
		entry.type = 0;
		entry.fcb_index = pom;
		Date date = new Date();
		entry.i_ctime = date.getTime();
		inode.i_mtime = entry.i_ctime;
		directories.get(currentdir).put(name,entry);
		
		System.out.println("Stworzono plik o nazwie " + name);
		return true;
	
	}
	
	public int open(String name)throws FileException {
		if(directories.get(currentdir).containsKey(name)) {
			for(int i=0; i<process_openfiles.size();i++) {
				if(openfiles[process_openfiles.get(i)] == fcblist[directories.get(currentdir).get(name).fcb_index]) {
					  throw new FileException("Ten plik juz jest otwarty");
			}}
		
			int freeindex = openfilesindex();
			if(freeindex == -1)throw new FileException("Brak wolnej pamieci");
			openfiles[freeindex] = fcblist[directories.get(currentdir).get(name).fcb_index];
			int indeks = freeindex;
			process_openfiles.add(indeks);
			System.out.println("otwarto plik o nazwie " + name);
			return process_openfiles.indexOf(freeindex);
		}
		else throw new FileException("Nie ma pliku o takiej nazwie");
		
	}

	public void close(int index) {
		int pom = process_openfiles.get(index);
		openfiles[pom] = null;
		process_openfiles.remove(index);
	
	}

	
	public boolean writetoFile(int index, byte[] data) { 
		
		if(index >= process_openfiles.size()) { //if file is not open, writing data to it is not possible
			return false;
		}
		
		int pom = process_openfiles.get(index);
		clearFile(openfiles[pom]); //file is cleared before writing new data to it
		int i = 0; // stores the number of bytes written
		int pom2 = 0;
		int pom3 = 0;
		
		//writing to the first block
		i = disk.writetoblock(openfiles[pom].block_index1, i, data);
		
		//writing to the second block if necessary 
		if(i<data.length) {
			if(openfiles[pom].block_index2 == -1) {
				pom2 = disk.assign_freeblock();
				for(int n =0; n<32; n++) {
					if(fcblist[n].equals(openfiles[pom])) {
							fcblist[n].block_index2 = pom2;
						pom3 = n;
						break;
					}
				}
				openfiles[pom].block_index2 = pom2;	
			}	
			i = disk.writetoblock(openfiles[pom].block_index2,i,data);
		} 

		//writing to index blocks if necessary 
		while(i< data.length) {
			
			if(openfiles[pom].index_block == -1) {
				openfiles[pom].index_block = disk.assign_freeblock();
				fcblist[pom3].index_block = openfiles[pom].index_block;
			}
			
			pom2 = disk.assign_freeblock();	
			disk.writetoindexblock(openfiles[pom].index_block, pom2);
			i = disk.writetoblock(pom2, i, data);
		}
		
		//inode update
		openfiles[pom].size += i;
		Date date = new Date();
		openfiles[pom].i_mtime = date.getTime(); 
		
		return true;
	}
	

	
	public boolean appendFile(int index, byte[] data) throws FileException {
		
		if(!process_openfiles.contains(index)) {
			return false;
		}
		
		int pom = process_openfiles.get(index);
		int i = 0;
		
		while(i < data.length) {
			
		if(disk.freeblockspace(openfiles[pom].block_index1) != -1 ) {
	
			i = disk.appendblock(openfiles[pom].block_index1, i, data);
		     continue;
		}
		
	if(openfiles[pom].block_index2 == -1) {
			openfiles[pom].block_index2 = disk.assign_freeblock();
	
		}
		
	if(openfiles[pom].block_index2 != -1 && disk.freeblockspace(openfiles[pom].block_index2) != -1) {
			
			i = disk.appendblock(openfiles[pom].block_index2, i, data);
			 continue;
		}
		
		
	if(openfiles[pom].index_block == -1) {
		openfiles[pom].index_block = disk.assign_freeblock();
	}

	if(openfiles[pom].index_block != -1) {
		
		int counter = 0;
		while(i<data.length) {
			disk.writetoindexblock(openfiles[pom].index_block, disk.assign_freeblock());
			byte[] help = disk.readblock(openfiles[pom].index_block, counter+1);
			i = disk.appendblock((int)help[counter], i, data);
			counter++;
			}
		} } 
		
		openfiles[pom].size += i;
		Date date = new Date();
		openfiles[pom].i_mtime = date.getTime(); 

		return true;
	}
	
	

	public byte[] readfromFile(int index) throws FileException{
	
		if(!process_openfiles.contains(index)) {
			throw new FileException("Plik nie jest otwarty");
			
		}
		
		byte[] data; 
		int pom = process_openfiles.get(index);
		int count = openfiles[pom].size; 
	    data = new byte[count];
		int size_control = 0;
		byte[] pom2;
		
		pom2 = disk.readblock(openfiles[pom].block_index1, count);
		System.arraycopy(pom2, 0, data, size_control, pom2.length);
		size_control += pom2.length;
		
		if(pom2.length < count && openfiles[pom].block_index2 != -1) {
				pom2 = disk.readblock(openfiles[pom].block_index2, (count-size_control));
				System.arraycopy(pom2, 0, data, size_control, pom2.length);
				size_control += pom2.length;
		}
		
		if(pom2.length < count && openfiles[pom].index_block != -1) {
			byte[] help = disk.readblock(openfiles[pom].index_block, 32);
			
			for(int c = 0; c < help.length; c++) {
				if(help[c] != (byte)0) {
					pom2 = disk.readblock((int)help[c], (count-size_control));
					System.arraycopy(pom2, 0, data, size_control, pom2.length);
					size_control += pom2.length;}
				}
			}
		return data;
	}
	
	

	public byte[] readingfromFile(int index, int count) throws FileException{
		
				if(!process_openfiles.contains(index)) {
					throw new FileException("Plik nie jest otwarty");
				}
				byte[] data; 
				int pom = process_openfiles.get(index);
				
				if(count > openfiles[pom].size) {
					data = new byte[openfiles[pom].size];
				} else { 
					data = new byte[count]; }
				int size_control = 0;
				byte[] pom2;
				
				if(openfiles[pom].pointer >= openfiles[pom].block_index1*32 && openfiles[pom].pointer < ((openfiles[pom].block_index1 + 1)*32)  ) {
				pom2 = disk.readpartofblock(openfiles[pom].block_index1, count,  openfiles[pom].pointer);
					
				
				for(int i = 0; i < pom2.length && i < data.length; i++) {
					data[i] = pom2[i];
					size_control++;
					openfiles[pom].pointer++;
				} 
				
			  if(openfiles[pom].pointer == ((openfiles[pom].block_index1 + 1)*32) && openfiles[pom].block_index2 != -1) {
				  openfiles[pom].pointer = openfiles[pom].block_index2*32;
			  }
				}
			
				if(openfiles[pom].block_index2 != -1 && openfiles[pom].pointer >= openfiles[pom].block_index2*32 && openfiles[pom].pointer < ((openfiles[pom].block_index2 + 1)*32)) {
				

						pom2 = disk.readpartofblock(openfiles[pom].block_index2, (count-size_control),  openfiles[pom].pointer);
							
						for(int i = 0; i < pom2.length && i< data.length &&  size_control < data.length; i++) {
							data[size_control] = pom2[i];
							size_control++;
							openfiles[pom].pointer++;
						}
				  if(openfiles[pom].pointer == ((openfiles[pom].block_index2 + 1)*32) && openfiles[pom].index_block != -1) {
					  openfiles[pom].pointer = openfiles[pom].index_block*32;
				  }
				  
				}
				if(openfiles[pom].index_block != -1) {
						byte[] help = disk.readblock(openfiles[pom].index_block, 32);
						int control = 0;
						if(openfiles[pom].pointer == openfiles[pom].index_block*32) openfiles[pom].pointer = (int)help[0]*32;
						
						while(control != 1 && size_control < count) {
							for(int c = 0; c < help.length; c++) {
								if(help[c] != (byte)0) {	
									
								if (openfiles[pom].pointer >=  (int)help[c]*32 && openfiles[pom].pointer <  ((int)help[c]+1)*32) {
								
								}

								else if 
									(openfiles[pom].pointer ==  ((int)help[c]+1)*32) {
									openfiles[pom].pointer = (int)help[c+1]*32;
									}	
								else continue;
							
								pom2 = disk.readpartofblock((int)help[c], count-size_control, openfiles[pom].pointer);
								for(int i = 0; i < pom2.length; i++) {
									if(pom2[i] == (byte)0 || size_control >= data.length) { control = 1; break;}
									data[size_control] = pom2[i];
									size_control++;
									openfiles[pom].pointer++;
									
									if(openfiles[pom].pointer == endoffile(openfiles[pom])) {
										control = 1;
										
									}
										
									
									if(size_control >= data.length) {control = 1; break;}
									} 	
								} 
							}   
						}}
		
				if(openfiles[pom].pointer >= endoffile(openfiles[pom])) {
					openfiles[pom].pointer = openfiles[pom].block_index1 *32;
				}
			for(int i = 0; i < data.length; i++) {
				if(data[i] == (byte)0) {
				
					byte[] help = new byte[i];
					
					for(int a = 0; a < help.length; a++) {
					
						help[a] = data[a];
					
					}
					return help; }
			}
	return data;
			}
	
	
public boolean deleteFile(String name) throws FileException {
		
		if(directories.get(currentdir).containsKey(name)) {
			for(int i=0; i<process_openfiles.size();i++) {
				if(openfiles[process_openfiles.get(i)] == fcblist[directories.get(currentdir).get(name).fcb_index]) {
					openfiles[process_openfiles.get(i)] = null;
					process_openfiles.remove(i);
			}}
			
			for(int i=0; i<32;i++) {
				if(openfiles[i] == null) {
					
				
			} else if (openfiles[i] == fcblist[directories.get(currentdir).get(name).fcb_index]) {
				return false;
				}
			
			}
			if(directories.get(currentdir).get(name).type == 1) {
				return false;
			}
			clearwholeFile(fcblist[directories.get(currentdir).get(name).fcb_index]);
			fcblist[directories.get(currentdir).get(name).fcb_index].size =0;
			fcblist[directories.get(currentdir).get(name).fcb_index] = null;
		
			directories.get(currentdir).remove(name);
			return true;
			}
		else return false;
	} 



//directory management
	
	public boolean createDirectory (String name) throws FileException {
		if(directories.get(currentdir).containsKey(name)) {
			//throw new FileException("Katalog o danej nazwie juz istnieje w danym katalogu");
			return false;
		}
		
		
		Catalog_Entry entry = new Catalog_Entry();
		entry.name = name;
		entry.type = 1;
		entry.fcb_index = -1;
		Date date = new Date();
		entry.i_ctime = date.getTime();
		directories.get(currentdir).put(name,entry);
		Hashtable<String, Catalog_Entry> catalog = new Hashtable<String, Catalog_Entry>();
		directories.add(catalog);
	
		return true;
	} 

	
	
public boolean changeDir(String name) {
	
	int filecontrol = 0;
	if(currentdir == 0) {
		
	Collection<Catalog_Entry> pom = directories.get(currentdir).values();
	Catalog_Entry pom2[] = pom.toArray(new Catalog_Entry[directories.get(currentdir).size()]);
	sortbytime(pom2);
	for(int i=0; i<directories.get(currentdir).size(); i++) {
		if(pom2[i].type == 0) { 
			filecontrol++;
		}
		
		else if((!name.contentEquals(pom2[i].name)) && (pom2[i].type == 1)) {
		
		}
		
		else if((name.contentEquals(pom2[i].name)) && (pom2[i].type == 1)) { 		
			currentdir = (i-filecontrol+1);
			return true;
		}
	}
	
		return false;
	}
	else currentdir = 0;
	return true;
	
	}

public boolean deleteDir(String name)throws FileException {
	currentdir = 0;
	if(directories.get(currentdir).containsKey(name) && directories.get(currentdir).get(name).type == 1) {
		changeDir(name);
		Collection<Catalog_Entry> pom = directories.get(currentdir).values();
		Catalog_Entry[] pom2 = pom.toArray(new Catalog_Entry[directories.get(currentdir).size()]);
		for(int i=0; i<directories.get(currentdir).size();i++) {
				deleteFile(pom2[i].name);
			}
			directories.remove(currentdir);
			currentdir = 0;
			directories.get(currentdir).remove(name);
		return true;
	}
	return false;
} 


private Catalog_Entry[] sortbytime(Catalog_Entry lista[]) {
	int  n =  lista.length-1;
	for(int i =0; i<n; i++) {
		for(int j=0; j < n; j++) {
			if(lista[j].i_ctime > lista[j+1].i_ctime) {
				Catalog_Entry help = lista[j];
				lista[j] = lista[j+1];
				lista[j+1] = help;
			}
		}
	}
	return lista;
}


// information output

public void listDir() { 
	
	 Collection<Catalog_Entry> pom = directories.get(currentdir).values();
System.out.println(currentdir);
		if(pom.size() != 0) {	
			System.out.println("Pliki:");
			pom.forEach (entry -> {if(entry.type == 0) System.out.println(entry.name); });
			System.out.println("Katalogi:");
			pom.forEach (entry -> {if(entry.type == 1) System.out.println(entry.name); });
		}
		else {
			System.out.println("Katalog nie zawiera �adnych plik�w");
		}
	}


	public void allce() {
		Collection<Catalog_Entry> pom = directories.get(currentdir).values();
		
		if(pom.size() != 0) {	
			pom.forEach (entry -> {if(entry.type == 0) {System.out.println("Nazwa pliku: " + entry.name);
			System.out.println("Indeks pliku w tablicy i-wezlow: " + entry.fcb_index);
			System.out.println(""); }});
		}
	}
	
	public void allinodesinfo() {
		Collection<Catalog_Entry> pom = directories.get(currentdir).values();
		
		if(pom.size() != 0) {	
			pom.forEach (entry -> {if(entry.type == 0) {System.out.println("Nazwa pliku: " + entry.name);
			System.out.println("Rozmiar: " + fcblist[entry.fcb_index].size);
			//System.out.println("Czas stworzenia: " + fcblist[entry.fcb_index].i_ctime);
			System.out.println("Czas stworzenia: " + entry.i_ctime);
			System.out.println("Czas ostatniej modyfikacji:" + fcblist[entry.fcb_index].i_mtime);
			System.out.println("Blok 1 :" + fcblist[entry.fcb_index].block_index1);
			
			System.out.println("Blok 2: " + fcblist[entry.fcb_index].block_index2);
			
			System.out.println("Blok posredni: " + fcblist[entry.fcb_index].index_block);
			System.out.println(""); }});
		}
	}
 
	public void info(String name) {
		if(directories.get(currentdir).containsKey(name) && directories.get(currentdir).get(name).type == 0) {
			System.out.println("Size: " + fcblist[directories.get(currentdir).get(name).fcb_index].size);
			//System.out.println("Time of creation: " + fcblist[directories.get(currentdir).get(name).fcb_index].i_ctime);
			System.out.println("Time of creation: " + directories.get(currentdir).get(name).i_ctime);
			System.out.println("Time of the last modification:" + fcblist[directories.get(currentdir).get(name).fcb_index].i_mtime);
			System.out.println("Blok 1 :" + fcblist[directories.get(currentdir).get(name).fcb_index].block_index1);
			
			System.out.println("Blok 2: " + fcblist[directories.get(currentdir).get(name).fcb_index].block_index2);
			
			System.out.println("Blok posredni: " + fcblist[directories.get(currentdir).get(name).fcb_index].index_block);
				
			
		}
	}
	
	
	
	
	public void indexinfo(String name) {
		byte [] help = new byte [1] ;
	
		if(directories.get(currentdir).containsKey(name)) {
			if(fcblist[directories.get(currentdir).get(name).fcb_index].index_block != -1){
			help = disk.readblock(fcblist[directories.get(currentdir).get(name).fcb_index].index_block, 32);
			System.out.print("[");
			for(int i = 0; i < help.length; i++) {
				if(help[i] != (byte)0) {{
					System.out.print((int)help[i]);
					if( i+1 != help.length) System.out.print(",");}}
				else if(help[i] == (byte)0) break;
			}
			
			System.out.println("]");
			
		}else  System.out.println("Dany plik nie ma przydzielonego bloku indeksowego");}
		}
	
	
	public void dirinfo (String name) {
		int dir = currentdir;
		if(currentdir == 0) {
		changeDir(name);}
		else {
			changeDir("");
			changeDir(name);
		} 
		Collection<Catalog_Entry> pom = directories.get(currentdir).values();
		Catalog_Entry[] pom2 = pom.toArray(new Catalog_Entry[directories.get(currentdir).size()]);
		
		if(currentdir == 0) {
		System.out.println("Pliki:");
		for(int i=0; i<directories.get(currentdir).size(); i++) {
		if(pom2[i].type == 0) System.out.println("Name: " + pom2[i].name);
		info(pom2[i].name);
		}
		System.out.println("Podkatalogi:");
		for(int i=0; i<directories.get(currentdir).size(); i++) {
		if(pom2[i].type == 1) System.out.println(pom2[i].name);
		
		}}
		else {
			if(directories.get(currentdir).size() > 0){
			System.out.println("Pliki:");
			for(int i=0; i<directories.get(currentdir).size(); i++) {
			if(pom2[i].type == 0) System.out.println(pom2[i].name);
			info(pom2[i].name);
			}}else System.out.println("Katalog pusty");
		}
		currentdir = dir;
	}

	
	
	// auxiliary functions
	
	
	public void changepointer (String name, int n) {
		if(directories.get(currentdir).containsKey(name)) {
			if(n == 1) {
				fcblist[directories.get(currentdir).get(name).fcb_index].pointer = fcblist[directories.get(currentdir).get(name).fcb_index].block_index1*32;
			}
			if(n == 2 && fcblist[directories.get(currentdir).get(name).fcb_index].block_index2 != -1) {
				fcblist[directories.get(currentdir).get(name).fcb_index].pointer = fcblist[directories.get(currentdir).get(name).fcb_index].block_index2*32;
			}
			if(n == 3 && fcblist[directories.get(currentdir).get(name).fcb_index].index_block != -1) {
				fcblist[directories.get(currentdir).get(name).fcb_index].pointer = fcblist[directories.get(currentdir).get(name).fcb_index].index_block*32;
			}
		}		
	} 
	
	
	public String bytetoString (byte[] data) {
		String text = new String(data);
		return text;
	}
	public byte[] Stringtobyte(String text) {
		return text.getBytes();
	}
	
	public int endoffile(INODE file) {
		if(file.index_block != -1) {
			byte[] pom = disk.readblock(file.index_block, 32);
			
			if(disk.freeblockspace(pom[pom.length-1]) != -1)
			return disk.freeblockspace(pom[pom.length-1]);
			else return (pom[pom.length-1]+1)*32-1;
	 }
		else if(file.block_index2 != -1) {
			if(disk.freeblockspace(file.block_index2) != -1) {
				return disk.freeblockspace(file.block_index2);
			}else return ((file.block_index2+1)*32)-1;}
		else if(file.block_index1 != -1) {
			if(disk.freeblockspace(file.block_index1) != -1) {
				return disk.freeblockspace(file.block_index1);
			}else return ((file.block_index1+1)*32)-1;}
		
return 0;
	}
	private void clearFile(INODE file) {
		if(file.index_block != -1) {
			byte[] pom = disk.readblock(file.index_block, 32);
			for(int i=0; i< pom.length; i++)
			disk.clearblock(pom[i]);
			file.index_block = -1;
			
		}
		if(file.block_index2 != -1) {
			disk.clearblock(file.block_index2);
		file.block_index2 = -1;}
		if(file.block_index1 != -1) 
			disk.clearfirstblock(file.block_index1);
		file.size = 0;
	}
	
	private void clearwholeFile(INODE file) {
		if(file.index_block != -1) {
			byte[] pom = disk.readblock(file.index_block, 32);
			for(int i=0; i< pom.length; i++)
			disk.clearblock(pom[i]);
		}
		if(file.block_index2 != -1)
			disk.clearblock(file.block_index2);
		if(file.block_index1 != -1)
			disk.clearblock(file.block_index1);
	}
	
	
	
	
}
