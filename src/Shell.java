package projekt_SO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;



public class Shell
{
	Scanner scan = new Scanner(System.in);

	private void Read()
	{
	     scan.nextLine();
	}
	
	
	private static ArrayList<String> commands = new ArrayList<String>();
	private static HashMap<String, Integer> argumentsNumber = new HashMap<String, Integer>();


	public FileSystem files = new FileSystem();
	public int x;
	private String directory = new String("");


	private static void clear()
	{
		try
		{
			if (System.getProperty("os.name").contains("Windows"))
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			else
				Runtime.getRuntime().exec("clear");
		}
		catch (IOException | InterruptedException ex) {}
	}

	static
	{

		commands.add("rm");
		argumentsNumber.put("rm",1);
		
		
		commands.add("rmdir");
		argumentsNumber.put("rmdir",1);
		

		commands.add("ls");
		argumentsNumber.put("ls",0);
		

		commands.add("cd");
		argumentsNumber.put("cd",1);
		
		
		commands.add("more");
		argumentsNumber.put("more",1);
	
		commands.add("moreb");
		argumentsNumber.put("moreb",2);
		

		commands.add("touch");
		argumentsNumber.put("touch",1);
		
		
		commands.add("mkdir");
		argumentsNumber.put("mkdir",1);

		commands.add("clear");
		argumentsNumber.put("clear",0);
		
		commands.add("shutdown");
		argumentsNumber.put("shutdown",0);

		commands.add("echo");
		argumentsNumber.put("echo",100);

		commands.add("cp");
		argumentsNumber.put("cp",2);

			commands.add("move");
		argumentsNumber.put("move",2);

		commands.add("allce");
		argumentsNumber.put("allce",0);
			
		commands.add("df");
		argumentsNumber.put("df",0);
		
		commands.add("copy");
		argumentsNumber.put("copy",2);
	
		commands.add("info");
		argumentsNumber.put("info",1);
	
		commands.add("dinfo");
		argumentsNumber.put("dinfo",1);
	
		commands.add("iinfo");
		argumentsNumber.put("iinfo",1);
		
		commands.add("allinfo");
		argumentsNumber.put("allinfo",0);

	}

	
	private boolean recognizeCommand(String command)
	{
		for(int i=0; i<commands.size(); i++)
			if(command.equals(commands.get(i))) return true;
		return false;
	}


	private boolean checkIfCorrect(ArrayList<String> command)
	{
		if("echo".equals(command.get(0)))
		{
			if(command.size()>=4 && (">>".equals(command.get(command.size()-2)) || ">".equals(command.get(command.size()-2))))
				return true;
			else if (command.size()>1) return true;
		}
		if("cd".equals(command.get(0)) && command.size()==1) return true;
		if(command.size()-1 == argumentsNumber.get(command.get(0)))
			return true;
		return false;
	}

	
	private void executeCommand(String command) throws IOException, FileException {
		ArrayList<String> commandParts = new ArrayList<String>();
		Matcher part = Pattern.compile("([!#-}0-9]|[>-])+").matcher(command);
		while (part.find())
			commandParts.add(part.group());
		int size = commandParts.size();
		if(commandParts.size() != 0 && recognizeCommand(commandParts.get(0)))
		{
			if(checkIfCorrect(commandParts))
			{
				boolean check = true;
				ArrayList<String> text = new ArrayList<String>();
				ArrayList<ArrayList<String>> doubleText = new ArrayList<ArrayList<String>>();
				String singleText = new String();

				switch(commandParts.get(0))
				{
					
					case "ls":
						files.listDir();
						break;
					
					case "touch":
						check = files.createFile(commandParts.get(1)); //check
						if(check==false) System.out.printf("Could not create file '%s': File/Directory already exists\n", commandParts.get(1));
						break;
						
					case "mkdir":
						check = files.createDirectory(commandParts.get(1)); //check
						if(check==false) System.out.printf("Could not create directory '%s': File/Directory already exists\n", commandParts.get(1));
						break;
						
					case "cd":
						check = files.changeDir(commandParts.get(1));
						if(check==false) System.out.printf("Could not change directory to '%s': It does not exist\n", commandParts.get(1));
						break;
					
					case "rm":
						check = files.deleteFile(commandParts.get(1));
						if(check==false) System.out.printf("'%s' cannot be deleted : There is no such file\n", commandParts.get(1));
						break;
					case "rmdir":
						check = files.deleteDir(commandParts.get(1));
						if(check==false) System.out.printf("'%s' cannot be deleted: There is no such directory\n", commandParts.get(1));
						break;
					
					case "more":
						x = files.open(commandParts.get(1));
						singleText = files.bytetoString(files.readfromFile(x));
						System.out.println(singleText);
						if(x==-1) System.out.printf("cannot open '%s' for reading\n",commandParts.get(1));
						files.close(x);
						break;
					case "moreb":
						x = files.open(commandParts.get(1));
						
						int bytes = Integer.parseInt(commandParts.get(2));	
						singleText = files.bytetoString(files.readingfromFile(x, bytes));
						System.out.println(singleText);
						if(x==-1) System.out.printf("cannot open '%s' for reading\n",commandParts.get(1));
						files.close(x);
						break;
					
					case "clear":
						clear();
						break;
					case "copy" :
						check = files.createFile(commandParts.get(2)); 
						if(check==false) System.out.printf("Could not create file '%s': File/Directory already exists\n", commandParts.get(2));
						x = files.open(commandParts.get(1));
						byte [] help = files.readfromFile(x);
						if(x==-1) System.out.printf("cannot open '%s' for reading\n",commandParts.get(1));
						files.close(x);
						int b = files.open(commandParts.get(2));
						check = files.writetoFile(b, help);
						if(check==false) System.out.printf("writing to '%s' has failed\n", commandParts.get(commandParts.size()-1));
						files.close(b);
						
						break;
						
					case "move" : 
						x = files.open(commandParts.get(1));
					
						byte [] help2 = files.readfromFile(x);
						if(x==-1) System.out.printf("cannot open '%s' for reading\n",commandParts.get(1));
						files.close(x);
						check = files.deleteFile(commandParts.get(1));
						if(check==false) System.out.printf("'%s' cannot be deleted : There is no such file\n", commandParts.get(1));
						
						check = files.createFile(commandParts.get(2)); 
						if(check==false) System.out.printf("Could not create file '%s': File/Directory already exists\n", commandParts.get(2));
						int a = files.open(commandParts.get(2));
						check = files.writetoFile(a, help2); 
						if(check==false) System.out.printf("writing to '%s' has failed\n", commandParts.get(commandParts.size()-1));
						files.close(a);
						break;
					
					case "shutdown":
						System.exit(0);
						break;
					
					case "echo":
						if(">>".equals(commandParts.get(commandParts.size()-2)))
						{
							String textAdd = "";
							for(int i=1; i<commandParts.size()-2; i++) {
								textAdd += commandParts.get(i);
								if(i+1 != commandParts.size()-2) {
									textAdd += " ";}}
							check = files.appendFile(files.open(commandParts.get(commandParts.size()-1)), files.Stringtobyte(textAdd));
							if(check==false) System.out.printf("writing to '%s' has failed\n", commandParts.get(commandParts.size()-1));
							files.close(x);
						}
						else if(">".equals(commandParts.get(commandParts.size()-2)))
						{

							String textWrite = "";
							for(int i=1; i<commandParts.size()-2; i++) {
								textWrite += commandParts.get(i);
							  if(i+1 != commandParts.size()-2) {
								textWrite += " ";
							}}
							check = files.writetoFile(files.open(commandParts.get(commandParts.size()-1)), files.Stringtobyte(textWrite)); //**dodac nazwe obiektu klasy FileSystem
							if(check==false) System.out.printf("writing to '%s' has failed\n", commandParts.get(commandParts.size()-1));
							files.close(x);
						}
						else
						{
							for(int i=1; i<commandParts.size(); i++)
								System.out.printf("%s ",commandParts.get(i));
							System.out.print("\n");
						}
						break;
						
					case "allinfo":
						files.allinodesinfo();
						break;
						
					case "info":
						files.info(commandParts.get(1));
						break;
						
					case "dinfo":
						files.dirinfo(commandParts.get(1));
						break;
						
					case "iinfo":
						files.indexinfo(commandParts.get(1));
						break;
						
					case "allce" :
						files.allce();
						break;
						
					case "cp" :
						files.changepointer(commandParts.get(1), Integer.parseInt(commandParts.get(2)));
						break;
					
			
					case "df":
						files.disk.stan_dysku();
						break;
				}
			}
			else System.out.println("bash: wrong number of arguments");
		}
		else
		{
			if(commandParts.size() == 0)
			{}
			else
			{
				System.out.printf("bash: %s: command not found\n",commandParts.get(0));
			}
		}
	}

	public void boot () throws IOException, FileException {
		while(true)
		{
			Scanner input = new Scanner(System.in);
			System.out.printf("root@Linux:~%s$ ", directory);
			String command = input.nextLine();

			executeCommand(command);
		}
	}
	
}