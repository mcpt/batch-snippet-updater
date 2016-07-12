import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.swing.border.*;

public class SnippetUpdater {
  
  public static final String DELIMITER = "<title>(.*?)</title>"; //used to check for <title> tag when parsing
  public static final String USER_DIR = "/Users/Atharva/Documents/test walker";//specifies directory to update files in (and all sub-directories within)
  public static final String SIGNIFIER = "<!--MCPT-->";//signifies whether file should be updated
  public static List <String> HEAD_HTML_CONTENTS, TAIL_HTML_CONTENTS;
  public static final String HEAD_HTML = "head.html", TAIL_HTML = "tail.html";
  
  public static boolean DEBUG = false;//set to true when debugging
  
  public static final Pattern TITLE_FINDER = Pattern.compile (DELIMITER);//used to check for <title> tag when parsing
  
  /* Replaces everything between the start of the file to the <main> tag with the contents of 'HEAD.html', and
   * replaces everything between </main> and the TAIL of the file with the contents of 'TAIL.html'.
   *
   * (Only happens if first line of file is <!--MCPT--> AND the file is not 'HEAD.html' or 'TAIL.html')
   */
  
  public static void update (Path p) {
    if (!p.getFileName ().equals (HEAD_HTML) && !p.getFileName ().equals (TAIL_HTML)) {
      try (BufferedReader in = new BufferedReader (new FileReader (p.toFile ()))) {
        String firstLine = in.readLine ();
        
        if (firstLine != null && firstLine.equals (SIGNIFIER)) {//first line of file signifies whether file should be updated
          in.close ();
          List <String> text = readAllLines (p);
          
          String titleField = null;
          Matcher matcher;
          
          for (int i = 0; i < text.size (); i++) {
             matcher = TITLE_FINDER.matcher (text.get (i));
              
             if (matcher.find ()) {
               titleField = matcher.group (1);
               break;
            }
          }
          
          PrintWriter out = new PrintWriter (new FileWriter (p.toFile ()));
          
          //Prints signifier, and contents of HEAD.html as well as <main> tag at the TAIL
          out.println (SIGNIFIER);
          
          if (DEBUG) {
            for (String i : HEAD_HTML_CONTENTS) {
              System.out.println (i);
            }
          }
          
          for (int i = 0; i < HEAD_HTML_CONTENTS.size (); i++) {
            
            matcher = TITLE_FINDER.matcher (HEAD_HTML_CONTENTS.get (i));
            
            if (matcher.find ()) {
              String newLine = HEAD_HTML_CONTENTS.get (i).replaceAll (DELIMITER, "<title>" + titleField + "</title>");
              HEAD_HTML_CONTENTS.set (i, newLine);
              
              if (DEBUG) {
                System.out.println ("KEEP ORIGINAL <TITLE> [new <title> found in head.html]: " + newLine);
              }
            }
            
            out.print (HEAD_HTML_CONTENTS.get (i));
            
            //is this right? should it not be on a new line
            if (i != HEAD_HTML_CONTENTS.size () - 1) {
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
          
          for (int i = 0; i < TAIL_HTML_CONTENTS.size (); i++) {
            
            matcher = TITLE_FINDER.matcher (TAIL_HTML_CONTENTS.get (i));
            
            if (matcher.find ()) {
              String newLine = TAIL_HTML_CONTENTS.get (i).replaceAll (DELIMITER, "<title>" + titleField + "</title>");
              TAIL_HTML_CONTENTS.set (i, newLine);
              
              if (DEBUG) {
                System.out.println ("KEEP ORIGINAL <TITLE> [new <title> found in tail.html]: " + newLine);
              }
            }
            
            out.println (TAIL_HTML_CONTENTS.get (i));
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
              
              if (p.getFileName ().toString ().equals (HEAD_HTML)) {
                HEAD_HTML_CONTENTS = readAllLines (p);
              }
              else if (p.getFileName ().toString ().equals (TAIL_HTML)) {
                TAIL_HTML_CONTENTS = readAllLines (p);
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
      System.out.println (HEAD_HTML_CONTENTS);
      System.out.println (TAIL_HTML_CONTENTS);
    }
    
    for (Path p : paths) {
      if (DEBUG) {
        System.out.println (p);
      }
      
      update (p);
    }
  }
}