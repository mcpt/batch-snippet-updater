import java.nio.file.*;
import java.util.*;
import java.io.*;

public class SnippetUpdater {
  
  public static final String USER_DIR = "INSERT DIRECTORY HERE";//specifies directory to update files in (and all sub-directories within)
  //public static final String FILE_FILTER = "*.{html}";//add additional file types with commas, e.g. '*.{html,css,java}'
  public static final String SIGNIFIER = "<!--MCPT-->";//signifies whether file should be updated
  public static List <String> BEGIN_HTML_CONTENTS, END_HTML_CONTENTS;
  
  public static boolean DEBUG = false;//set to true when debugging
  
  /* Replaces everything between the start of the file to the <main> tag with the contents of 'begin.html', and
   * replaces everything between </main> and the end of the file with the contents of 'end.html'.
   *
   * (Only happens if first line of file is <!--MCPT--> AND the file is not 'begin.html' or 'end.html')
   */
  
  public static void update (Path p) {
    if (!p.getFileName ().equals ("head.html") && !p.getFileName ().equals ("tail.html")) {
      try (BufferedReader in = new BufferedReader (new FileReader (p.toFile ()))) {
        String firstLine = in.readLine ();
        
        if (firstLine != null && firstLine.equals (SIGNIFIER)) {//first line of file signifies whether file should be updated
          in.close ();
          List <String> text = readAllLines (p);
          
          PrintWriter out = new PrintWriter (new FileWriter (p.toFile ()));
          
          //Prints signifier, and contents of begin.html as well as <main> tag at the end
          out.println (SIGNIFIER);
          
          if (DEBUG) {
            for (String i : BEGIN_HTML_CONTENTS) {
              System.out.println (i);
            }
          }
          
          for (int i = 0; i < BEGIN_HTML_CONTENTS.size (); i++) {
            out.print (BEGIN_HTML_CONTENTS.get (i));
            
            if (i != BEGIN_HTML_CONTENTS.size () - 1) {
              out.println ();
            }
          }
          
          //finds position of <main> tag
          int pos = text.size ();
          
          for (int i = 0; i < text.size (); i++) {
            if (text.get (i).trim ().equals ("<main>")) {
              pos = i;
              break;
            }
          }
          
          //prints out all contents between <main> and </main> (including the two tags)
          for (int i = pos; i < text.size (); i++) {
            out.println (text.get (i));
            
            if (text.get (i).trim ().equals ("</main>")) {
              break;
            }
          }
          
          for (String i : END_HTML_CONTENTS) {
            out.println (i);
          }
          
          out.close ();
        }
      }
      catch (IOException e) {
        System.out.println (e);
      }
    }
  }
  
  /* Gets all file paths specified by the filter in a given directory (including all sub-directories)
   *
   */
  
  public static List <Path> getSourcePaths (Path dir) {
    List <Path> paths = new ArrayList <Path> ();
    
    Queue <Path> queue = new ArrayDeque <Path> ();
    queue.offer (dir);
    
    Path curr;
    
    while (!queue.isEmpty ()) {
      curr = queue.poll ();
      
      try (DirectoryStream <Path> stream = Files.newDirectoryStream (curr /*, FILE_FILTER*/)) {
        for (Path p : stream) {
          if (Files.isDirectory (p) || p.getFileName ().toString ().endsWith (".html")) {
            if (!Files.isDirectory (p)) {
              paths.add (p);
              
              if (p.getFileName ().toString ().equals ("head.html")) {
                BEGIN_HTML_CONTENTS = readAllLines (p);
              }
              else if (p.getFileName ().toString ().equals ("tail.html")) {
                END_HTML_CONTENTS = readAllLines (p);
              }
            }
            else {
              queue.offer (p);
            }
          }
        }
      }
      catch (DirectoryIteratorException | IOException e) {
        System.out.println (e);
      }
    }
    
    return paths;
  }
  
  /* Read file from path and get all contents as a List of Strings.
   *
   */
  
  public static List <String> readAllLines (Path p) throws IOException {
    BufferedReader in = new BufferedReader (new FileReader (p.toFile ()));
    List <String> text = new ArrayList <String> ();
    String ln;
    
    while ((ln = in.readLine ()) != null) {
      text.add (ln);
    }
    
    return text;
  }
  
  /* Starts process
   *
   */
  
  public static void main (String [] args) {
    List <Path> paths = getSourcePaths (Paths.get (USER_DIR));
    
    if (DEBUG) {
      System.out.println (BEGIN_HTML_CONTENTS);
      System.out.println (END_HTML_CONTENTS);
    }
    
    for (Path p : paths) {
      if (DEBUG) {
        System.out.println (p);
      }
      
      update (p);
    }
  }
}
