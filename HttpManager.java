// Any questions should be directed to Peter Darveau, P.Eng. - Hexagon Technology Inc.
// This file is intended for Proof of Concept only and should not be used in production environment
// Tested on Linux 4.14.79


import java.util.*;


class HttpManager
{
  HashMap<String, String> names;
  JsonManager jm;

  public HttpManager() {
    names = new HashMap<String, String>();
    jm = new JsonManager();
  }

  // PUT api
  public void put(String uri, String content) throws Exception {
    StringTokenizer st = new StringTokenizer(uri, "/");
    String name = null;
    String url = null;

    if(st.countTokens() == 2 && st.nextToken().equals("names")) {
      name = st.nextToken();
    }

    if(!isWord(name)) {
      throw new Exception();
    }

    url = jm.getInfo(content, "url");

    names.put(name, url);
  }

  // GET api
  public String get(String uri, String content) throws Exception {
    StringTokenizer st = new StringTokenizer(uri, "/");
    String name = null;

    if(st.countTokens() == 2 && st.nextToken().equals("names")) {
      name = st.nextToken();
    }

    String url = names.get(name);

    if(url == null) {
      throw new Exception();
    }

    return jm.createJson(name, url);
  }


  // DELETE api
  public void delete(String uri) {
    if(uri.equals("/names")) {
      names.clear();
    }
  }


  // POST api
  public String post(String uri, String content) throws Exception {
    if(uri.equals("/annotate")) {
      return annotate(content);
    }
    throw new Exception("uri is " + uri + " instead of /annotate");
  }


  // TODO Deal with skipping comments
  private String annotate(String text) {
    StringBuilder sb = new StringBuilder(text);

    // The first word is considered the tag name
    // Any following words are attributes that are matched IN ORDER
    char[] link = {'a', ' ', 'h', 'r', 'e', 'f'};

    int i = 0;
    boolean lookingAtWord = false;
    int start = 0;

    // We don't validate bounds inside the loop because open braces are expected
    // to be followed up by close braces. Opened tags are also expected to be
    // closed.
    // Our assumption is that the provided HTML is valid.
    while(i < sb.length()) {

      // Analyse what's inside the tag - O(n)
      if(sb.charAt(i) == '<' && !lookingAtWord) {
        int j = 0;
        boolean matchingTagName = true;

        // Move to the next character
        i++;

        // Looking for 'a href' in the tag
        while(j < link.length && sb.charAt(i) != '>') {
          // We have a character match!
          if(link[j] == sb.charAt(i)) {
            if(matchingTagName && sb.charAt(i) == ' ') {
              // We're looking for tag attributes now
              matchingTagName = false;
            }
            i++;
            j++;
            continue;
          }

          // Trimming any unwanted space
          if((j == 0 || link[j-1] == ' ') && sb.charAt(i) == ' ') {
            i++;
            // We are looking for a whole word at this point
            continue;
          }

          // If not a tag
          // Implication is that we don't need to trim and
          // character doesn't match
          if(matchingTagName) {
            // TODO Make sure we don't accidentally stop on a non-terminating >
            // If it's in a comment, ignore that stuff
            // If it's escaped, don't fall for it

            // If it's in a quote, ignore that stuff too
            // Look for the next legit >
            boolean inQuotes = false;
            while(sb.charAt(i) != '>' || inQuotes) {
              if(sb.charAt(i) == '\'' || sb.charAt(i) == '"') {
                inQuotes = !inQuotes;
              }

              i++;
            }
          }

          // Character doesn't match what we are looking for
          // Look at the same word again because there was a mismatch (ROLLBACK)
          while(j > 0 && link[j] != ' ') {
            j--;
          }
          j++;

          // Look forward to the next word because there was a mismatch
          while(sb.charAt(i) != ' ' && sb.charAt(i) != '>') {
            i++;
          }
        }

        // We are in an 'a' tag with an href attribute
        if(j == link.length) {
          // So we want to skip over this entire tag's content
          // because we consider it already hyperlinked and href set
          int closeBraces = 2;
          int openBraces = 1;

          while(!(closeBraces == 0 && openBraces == 0)) {
            if(sb.charAt(i) == '>') {
              closeBraces--;
            } else if(sb.charAt(i) == '<' && sb.charAt(i+1) == '/') {
              openBraces--;
            } else if(sb.charAt(i) == '<') {
              openBraces++;
            }
            i++;
          }
        }
        // If we are at a close tag and it wasn't a perfect match,
        // we didn't find an 'a' tag from earlier
        else if(sb.charAt(i) == '>') {
          // So we just ignore what we saw in that tag but not its upcoming content
          i++;
        }
        continue;
      }

      // Is the character in our acceptable range of [A-za-z0-9]?
      if(isAlphaNumeric(sb.charAt(i))) {
        // If we weren't looking at a word, now we are so we mark that
        if(!lookingAtWord) {
          lookingAtWord = true;
          start = i;
        }
      } else {
        // Have we arrived at the end of an alphanumeric sequence?
        if(lookingAtWord) {
          int end = i;
          String word = sb.substring(start, end);

          // Do we have to annotate this word?
          if(names.containsKey(word)) {
            // Remove the old word and add the annotation

            sb.delete(start, end);
            String annotation = annotateIt(word);
            sb.insert(start, annotation);
            i = start + annotation.length() - 1;
          }

          if(sb.charAt(i) == '<') {
            i--;
          }

          lookingAtWord = false;
        }
      }

      i++;
    }

    // Check the final word if we are looking at one
    if(lookingAtWord) {
      int end = sb.length();
      String word = sb.substring(start, end);

      if(names.containsKey(word)) {
        sb.delete(start, end);
        String annotation = annotateIt(word);
        sb.insert(start, annotation);
      }
    }

    return sb.toString();
  }


  // Is the given character an alphabet or numeric?
  private boolean isAlphaNumeric(char c) {
    return (c -'0' >= 0 && c - '0' < 10)
        || (c - 'a' >= 0 && c - 'a' < 26)
        || (c - 'A' >= 0 && c - 'A' < 26);
  }


  // Does the given string contain only alphanumerics?
  private boolean isWord(String word) {
    if(word == null) {
      return false;
    }

    for(int i = 0; i < word.length(); i++) {
      if(!isAlphaNumeric(word.charAt(i))) {
        return false;
      }
    }
    return true;
  }


  // Annotate a given word with its link
  private String annotateIt(String word) {
    return "<a href=" + names.get(word) + ">" + word + "</a>";
  }
}

