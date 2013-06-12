package esg.common;

public class CPrint {
    
    static boolean hasColorSupport = false;
    static {
        try { hasColorSupport = System.getenv("TERM").matches(".*color.*"); }catch(Exception e) { }
    }
    
    private static final String[] atCode = new String[] { "00", "01", "04", "06", "07" };
    private static final String[] fgCode = new String[] { "00", "30", "31", "32", "33", "34", "35", "36", "37" };
    private static final String[] bgCode = new String[] { "00", "40", "41", "42", "43", "44", "45", "46", "47" };

    private static final int NONE = 0;

    private static final int BOLD = 1;
    private static final int UNDERSCORE = 2;
    private static final int BLINK = 3;
    private static final int REVERSE = 4;

    private static final int BLACK = 1;
    private static final int RED = 2;
    private static final int GREEN = 3;
    private static final int YELLOW = 4;
    private static final int BLUE = 5;
    private static final int MAGENTA = 6;
    private static final int CYAN = 7;
    private static final int WHITE = 8;
    

    public static String colorPrint(int fgIdx, String msg) { return colorPrint(fgIdx, 0, 0, msg); }
    public static String colorPrint(int fgIdx, int atIdx, String msg) { return colorPrint(fgIdx, atIdx, 0, msg); }
    public static String colorPrint(int fgIdx, int atIdx, int bgIdx, String msg) {
        if(!hasColorSupport) return msg;
        StringBuilder code = new StringBuilder("\033["+atCode[atIdx]+";"+fgCode[fgIdx]);
        if(bgIdx > 0) code.append(";"+bgCode[bgIdx]);
        code.append("m");
        code.append(msg);
        code.append("\033[0m");
        return code.toString();
    }
    
    public static String black(String msg){ return colorPrint(CPrint.BLACK,0,0,msg); }
    public static String red(String msg){ return colorPrint(CPrint.RED,0,0,msg); }
    public static String green(String msg){ return colorPrint(CPrint.GREEN,0,0,msg); }
    public static String yellow(String msg){ return colorPrint(CPrint.YELLOW,0,0,msg); }
    public static String blue(String msg){ return colorPrint(CPrint.BLUE,0,0,msg); }
    public static String magenta(String msg){ return colorPrint(CPrint.MAGENTA,0,0,msg); }
    public static String cyan(String msg){ return colorPrint(CPrint.CYAN,0,0,msg); }
    public static String white(String msg){ return colorPrint(CPrint.WHITE,0,0,msg); }

    public static String black_b(String msg){ return colorPrint(CPrint.BLACK,CPrint.BOLD,0,msg); }
    public static String red_b(String msg){ return colorPrint(CPrint.RED,CPrint.BOLD,0,msg); }
    public static String green_b(String msg){ return colorPrint(CPrint.GREEN,CPrint.BOLD,0,msg); }
    public static String yellow_b(String msg){ return colorPrint(CPrint.YELLOW,CPrint.BOLD,0,msg); }
    public static String blue_b(String msg){ return colorPrint(CPrint.BLUE,CPrint.BOLD,0,msg); }
    public static String magenta_b(String msg){ return colorPrint(CPrint.MAGENTA,CPrint.BOLD,0,msg); }
    public static String cyan_b(String msg){ return colorPrint(CPrint.CYAN,CPrint.BOLD,0,msg); }
    public static String white_b(String msg){ return colorPrint(CPrint.WHITE,CPrint.BOLD,0,msg); }

    public static String OK = CPrint.green_b("[OK]");
    public static String FAIL = CPrint.red_b("[FAIL]");

    //for testing...
    public static void main(String[] args) {
        System.out.println(" and this is "+CPrint.black("BLACK")+", right?");
        System.out.println(" and this is "+CPrint.red("RED")+", right?");
        System.out.println(" and this is "+CPrint.green("GREEN")+", right?");
        System.out.println(" and this is "+CPrint.yellow("YELLOW")+", right?");
        System.out.println(" and this is "+CPrint.blue("BLUE")+", right?");
        System.out.println(" and this is "+CPrint.magenta("MAGENTA")+", right?");
        System.out.println(" and this is "+CPrint.cyan("CYAN")+", right?");
        System.out.println(" and this is "+CPrint.white("WHITE")+", right?");

        System.out.println(" and this is "+CPrint.black_b("BLACK")+", right?");
        System.out.println(" and this is "+CPrint.red_b("RED")+", right?");
        System.out.println(" and this is "+CPrint.green_b("GREEN")+", right?");
        System.out.println(" and this is "+CPrint.yellow_b("YELLOW")+", right?");
        System.out.println(" and this is "+CPrint.blue_b("BLUE")+", right?");
        System.out.println(" and this is "+CPrint.magenta_b("MAGENTA")+", right?");
        System.out.println(" and this is "+CPrint.cyan_b("CYAN")+", right?");
        System.out.println(" and this is "+CPrint.white_b("WHITE")+", right?");

        System.out.println(" and this is "+CPrint.colorPrint(RED,BOLD,GREEN,"Something nice")+", right?");

        System.out.println(OK);
        System.out.println(FAIL+" failure");

    }
}
