import java.nio.file.*;
import java.util.*;
import java.io.*;

public class FileUpdater {
  
  public static final String USER_DIR = "test/";//specifies directory to update files in (and all sub-directories within)
  //public static final String FILE_FILTER = "*.{html}";//add additional file types with commas, e.g. '*.{html,css,java}'
  public static final String SIGNIFIER = "<!--MCPT-->";//signifies whether file should be updated
  public static List <String> BEGIN_HTML_CONTENTS, END_HTML_CONTENTS;
  
  /* Replaces everything between the start of the file to the <main> tag with the contents of 'begin.html', and
   * replaces everything between </main> and the end of the file with the contents of 'end.html'.
   * 
   * (Only happens if first line of file is <!--MCPT--> AND the file is not 'begin.html' or 'end.html')
   */
  
  public static void update (Path p) {
    if (!p.getFileName ().equals ("begin.html") && !p.getFileName ().equals ("end.html")) {
      try (BufferedReader in = new BufferedReader (new FileReader (p.toFile ()))) {
        if (in.readLine ().equals (SIGNIFIER)) {//first line of file signifies whether file should be updated
          in.close ();
          List <String> text = readAllLines (p);
          
          PrintWriter out = new PrintWriter (new FileWriter (p.toFile ()));
          
          //Prints signifier, and contents of begin.html as well as <main> tag at the end
          out.println (SIGNIFIER);
          out.println (BEGIN_HTML_CONTENTS);
          out.println ("<main>");
          
          //Prints out all normal contents of the file between <main> and </main>
          int pos = text.indexOf ("<main>");
          
          for (int i = pos + 1; i < text.size () && !text.get (i).equals ("</main>"); i++) {
            out.println (text.get (i));
          }
          
          //Prints </main> as well as contents of end.html
          out.println ("</main>");
          out.println (END_HTML_CONTENTS);
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
              
              if (p.getFileName ().toString ().equals ("begin.html")) {
                BEGIN_HTML_CONTENTS = readAllLines (p);
              }
              else if (p.getFileName ().toString ().equals ("end.html")) {
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
    String ln = in.readLine ();
    List <String> text = new ArrayList <String> ();
    
    while (ln != null) {
      text.add (ln.trim ());
      ln = in.readLine ();
    }
    
    return text;
  }
  
  /* Starts process
   * 
   */
  
  public static void main (String [] args) {
    List <Path> paths = getSourcePaths (Paths.get (USER_DIR));
    
    //System.out.println (BEGIN_HTML_CONTENTS);
    //System.out.println (END_HTML_CONTENTS);
    
    for (Path p : paths) {
      //System.out.println (p);
      update (p);
    }
  }
}