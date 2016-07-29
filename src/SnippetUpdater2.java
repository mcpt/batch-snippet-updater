import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

public class SnippetUpdater2 {
  
  public static final String USER_DIR = "/Users/Atharva/Documents/test";//specifies directory to update files in (and all sub-directories within)
  public static final String SIGNIFIER = "<!--MCPT-->";//signifies whether file should be updated
  
  public static final String MAIN_START_DELIMITER = "(.*?)<main>(.*?)"; //used to check for <main> when parsing
  public static final String MAIN_END_DELIMITER = "(.*?)</main>(.*?)"; //used to check for </main> when parsing
  
  public static final String CONTAINER_START_DELIMITER = "(.*?)<div class=\"container\">(.*?)";
  public static final String DIV_CLASS_END_DELIMITER = "(.*?)</div>(.*?)";
  public static final String DIV_CLASS_START_DELIMITER = "(.*?)<div class=\"(.*?)\">(.*?)";
  
  public static final Pattern MAIN_START_FINDER = Pattern.compile (MAIN_START_DELIMITER);
  public static final Pattern MAIN_END_FINDER = Pattern.compile (MAIN_END_DELIMITER);
  public static final Pattern CONTAINER_START_FINDER = Pattern.compile (CONTAINER_START_DELIMITER);
  public static final Pattern DIV_CLASS_START_FINDER = Pattern.compile (DIV_CLASS_START_DELIMITER);
  public static final Pattern DIV_CLASS_END_FINDER = Pattern.compile (DIV_CLASS_END_DELIMITER);
  
  public static final boolean DEBUG = false;
  
  public static int [] rangeMain (List <String> text, Path p) {
    int [] range = {-1, -1};
    
    for (int i = 0; i < text.size (); i++) {
      if (range [0] == -1) {//first hasn't been found
        if (MAIN_START_FINDER.matcher (text.get (i)).find ()) {
          range [0] = i;
        }
      }
      else if (range [1] == -1) {//second hasn't been found
        if (MAIN_END_FINDER.matcher (text.get (i)).find ()) {
          range [1] = i;
        }
      }
      else {
        break;
      }
    }
    
    if (range [1] == -1) {//main tag unmatched
      throw new IllegalArgumentException ("<main> not completed in " + p);
    }
    
    System.out.println ("MAIN");
    System.out.println (range [0] + " " + range [1]);
    
    return range;
  }
  
  public static List <int []> rangeContainer (List <String> text, Path p) throws IOException {
    
    List <int []> ranges = new ArrayList <int []> ();
    ranges.add (new int [] {-1, -1});
    int last_index = 0;
    int stack = 1;
    
    for (int i = 0; i < text.size (); i++) {
      if (ranges.get (last_index) [0] == -1) {
        if (CONTAINER_START_FINDER.matcher (text.get (i)).find ()) {
          ranges.get (last_index) [0] = i;
          stack = 1;
        }
      }
      else if (ranges.get (ranges.size () - 1) [1] == -1) {
        if (DIV_CLASS_START_FINDER.matcher (text.get (i)).find ()) {
          stack++;
        }
        else if (DIV_CLASS_END_FINDER.matcher (text.get (i)).find ()) {
          stack--;
        }
        
        if (stack == 0) {
          ranges.get (last_index) [1] = i;
          ranges.add (new int [] {-1, -1});
          last_index++;
        }
      }
    }
    
    if (ranges.get (last_index) [0] == -1) {
      ranges.remove (last_index);
    }
    else if (ranges.get (last_index) [1] == -1) {//problem, tag is not completed...
      throw new IllegalArgumentException ("<div class=\"container\" not completed in " + p);
    }
    
    System.out.println ("CONTAINER");
    for (int [] r : ranges) {
      System.out.println (r [0] + " " + r [1]);
    }
    
    return ranges;
  }
  
  public static void update (Path p) throws IOException {
    try (BufferedReader in = new BufferedReader (new FileReader (p.toFile ()))) {
      String firstLine = in.readLine ();
      
      if (firstLine != null && firstLine.equals (SIGNIFIER)) {
        in.close ();
        
        List <String> text = readAllLines (p);
        List <int []> ranges = rangeContainer (text, p);
        ranges.add (rangeMain (text, p));
        
        System.out.println ("Everything");
        for (int [] r : ranges) {
          System.out.println (r [0] + " " + r [1]);
        }
        
        Collections.sort (ranges, new Comparator <int []> () {
          public int compare (int [] a, int [] b) {
            return Integer.compare (a [0], b [0]);
          }
        });
        
        if (ranges.get (0) [0] == -1 || ranges.get (0) [1] == -1) {
          ranges.remove (0);
        }
        
        System.out.println ("final");
        for (int [] r : ranges) {
          System.out.println (r [0] + " " + r [1]);
        }
        
        PrintWriter out = new PrintWriter (new FileWriter (p.toFile ()));
        out.println (SIGNIFIER);//idk if needed
        
        boolean in_tag = false;
        int pos = 0;
        
        for (int i = 1; i < text.size (); i++) {
          //System.out.println (pos + " " + i + " " + in_tag + " " + ranges.get (pos) [0] + " " + ranges.get (pos) [1]);
          
          if (pos < ranges.size () && !in_tag && ranges.get (pos) [0] == i) {
            in_tag = true;
          }
          
          if (in_tag) {
            out.println (text.get (i));
          }
          
          if (pos < ranges.size () && in_tag && ranges.get (pos) [1] == i) {
            in_tag = false;
            pos++;
            
            while (pos < ranges.size () && ranges.get (pos) [0] < i) {
              pos++;
            }
          }
        }
        
        out.close ();
      }
    }
  }
  
  public static List <Path> getSourcePaths (Path dir) {
    List <Path> paths = new ArrayList <Path> ();
    
    Queue <Path> queue = new ArrayDeque <Path> ();
    queue.offer (dir);
    
    Path curr;
    
    while (!queue.isEmpty ()) {
      curr = queue.poll ();
      
      try (DirectoryStream <Path> stream = Files.newDirectoryStream (curr)) {
        for (Path p : stream) {
          if (Files.isDirectory (p) || p.getFileName ().toString ().endsWith (".html")) {
            if (!Files.isDirectory (p)) {
              paths.add (p);
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
  
  public static List <String> readAllLines (Path p) throws IOException {
    BufferedReader in = new BufferedReader (new FileReader (p.toFile ()));
    List <String> text = new ArrayList <String> ();
    String ln;
    
    while ((ln = in.readLine ()) != null) {
      text.add (ln);
    }
    
    return text;
  }
  
  public static void main (String [] t) throws IOException {
    List <Path> paths = getSourcePaths (Paths.get (USER_DIR));
    
    if (DEBUG) {
      System.out.println ("files to update: " + paths);
    }
    
    for (Path p : paths) {
      update (p);
    }
  }
}