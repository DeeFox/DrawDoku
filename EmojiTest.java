public class EmojiTest {
  public static void main(String[] args) {
    // Eingabe
    // Unicode-Darstellung des Lach-Weinen-Emojis
    // (http://apps.timwhitlock.info/unicode/inspect/hex/1F601)
    // Kann in Android aus Tastatureingabe stammen
    String emoji = "\uD83D\uDE01";
    System.out.println(emoji.length());
    if(emoji.length() == 2) {
      char[] c = emoji.toCharArray();

      int c1 = (int) c[0];
      int c2 = (int) c[1];

      // Ausgabe
      // 2xint, c1 und c2
      System.out.println("c1: " + c1);
      System.out.println("c2: " + c2);
    }
  }
}
