package util.filemanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Filemanager {

	static double progress = 1;

	public static double getProgress(){
		return progress;
	}

	/**
	 * Row 1 is the first line
	 */
	public static String getRow(String FileName, int row) {
		String string = Filemanager.getFileContent(FileName);

		String[] lines = string.split(System.getProperty("line.separator"));

		return lines[row - 1];
	}

	public static int getRowCount(String FileName) {
		String string = Filemanager.getFileContent(FileName);

		String[] lines = string.split(System.getProperty("line.separator"));

		return lines.length;
	}

	public static void saveFile(String Name, ArrayList<String> FileContent) {
		ArrayList<String> fc = FileContent;

		for (int i = 0; i < fc.size(); i++) {
			if (!(fc.get(i) instanceof String)) {
				fc.set(i, fc.get(i).toString());
			}
		}
		String string = String.join("\r\n", fc);

		Filemanager.saveFile(Name, string);
	}

	public static void saveFile(String Name, String FileContent) {
		FileOutputStream fop = null;
		File file;
		String content = FileContent;

		try {

			file = new File(Name);
			fop = new FileOutputStream(file);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// get the content in bytes
			byte[] contentInBytes = content.getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();

			// System.out.println("Done");

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Creates a JFileChooser to select the file.
	 */
	public static void saveFile(ArrayList<String> FileContent) {
		ArrayList<String> fc = FileContent;

		for (int i = 0; i < fc.size(); i++) {
			if (!(fc.get(i) instanceof String)) {
				fc.set(i, fc.get(i).toString());
			}
		}
		String string = String.join("\r\n", fc);

		Filemanager.saveFile(string);
	}

	/**
	 * Creates a JFileChooser to select the file.
	 */
	public static void saveFile(String content){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			var f = new JFileChooser();
			int res = f.showOpenDialog(null);
			if(res == JFileChooser.APPROVE_OPTION){
				saveFile(f.getSelectedFile().getAbsolutePath(), content);
			}
		}catch(java.awt.HeadlessException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	public static void addLine(String Name, int i) {
		String l = "" + i;
		Filemanager.addLine(Name, l);
	}

	public static void addLine(String Name, String line) {
		String content = getFileContent(Name);
		content += "\r\n" + line;
		saveFile(Name, content);
	}

	public static String getFileContent(String Name) {
		return getFileContent(new File(Name));
	}

	public static String getFileContent(File file) {
		InputStreamReader fis = null;
		String Fcontent = "";
		progress = 0;
		try {
			Path path = Paths.get(file.getAbsolutePath());
			long size = Files.size(path);	
			long status = 0;

			fis = new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8"));

			// System.out.println("Total file size to read (in bytes) : "+ fis.available());

			int content;
			while ((content = fis.read()) != -1) {
				// convert to char and display it
				// System.out.print((char) content);
				Fcontent += (char) content;
				status += 2;
				progress = (double)((double)status/(double)size);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return Fcontent;
	}

	public static File chooseFile(){
		return chooseFile(new File(""));
	}

	public static File chooseFile(File startPath){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			var f = new JFileChooser();
			f.setCurrentDirectory(startPath);
			int res = f.showOpenDialog(null);
			if(res == JFileChooser.APPROVE_OPTION)
				return f.getSelectedFile();
		}catch(java.awt.HeadlessException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		return new File("");
	}

	public static ArrayList<String> readFileInArrayList(File f){
		ArrayList<String> list = new ArrayList<String>();
		BufferedReader reader;
		progress = 0;

        try {
			Path path = Paths.get(f.getAbsolutePath());
			long size = Files.size(path);	
			long status = 0;

            reader = new BufferedReader(new FileReader(
                    f.getAbsolutePath()));
            String line = "-1";
            while (line != null) {
                line = reader.readLine();
				if(line != null)
					status += line.getBytes().length;				
				progress = (double)status/(double)size;
                list.add((line!=null)?line:"");
            }
			progress = 1;
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
	}

	public static String[] getFileContentArray(String Name) {
		// String string = Filemanager.getFileContent(Name);
		// String lines[] = string.split("\\r?\\n");
		// return lines;
		return getFileContentArray(new File(Name));
	}

	public static String[] getFileContentArray(File file) {
		String string = Filemanager.getFileContent(file);
		String lines[] = string.split(System.lineSeparator());
		return lines;
	}

	public static ArrayList<String> getFileContentArrayList(File f) {
		// String str = getFileContent(f);
		// ArrayList<String> res = new ArrayList<String>(Arrays.asList(str.split(System.lineSeparator())));
		// return res;
		return readFileInArrayList(f);
	}

	public static ArrayList<String> getFileContentArrayList(String s) {
		return getFileContentArrayList(new File(s));
	}


}
