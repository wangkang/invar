import invar.InvarContext;
import invar.InvarReadRule;
import invar.InvarWriteAS3;
import invar.InvarWriteJava;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

final public class Invar
{
    static final String ARG_HELP       = "-help";
    static final String ARG_RULE_PATH  = "-rule";
    static final String ARG_JAVA_PATH  = "-java";
    static final String ARG_FLASH_PATH = "-flash";
    static String       rulePath       = "rule/";
    static String       outPathJava    = "code/java/";
    static String       outPathFlash   = "code/flash/";

    static public void main(String[] args) throws Throwable
    {
        HashMap<String,List<String>> mapArgs = parseArguments(args);
        if (mapArgs.get(ARG_HELP) != null)
        {
            showHelp();
            return;
        }
        log("Invar start: " + new Date().toString());
        InvarContext ctx = new InvarContext();
        ctx.addBuildInTypes(InvarReadRule.makeTypeIdMap());
        List<String> rules = mapArgs.get(ARG_RULE_PATH);
        if (rules != null && rules.size() > 0)
            rulePath = rules.get(0);
        InvarReadRule.start(rulePath, ".xml", ctx);
        List<String> outPath = null;
        outPath = mapArgs.get(ARG_JAVA_PATH);
        if (outPath != null)
        {
            if (outPath.size() > 0)
                outPathJava = outPath.get(0);
            new InvarWriteJava(ctx, outPathJava).write(".java");
        }
        outPath = mapArgs.get(ARG_FLASH_PATH);
        if (outPath != null)
        {
            if (outPath.size() > 0)
                outPathFlash = outPath.get(0);
            new InvarWriteAS3(ctx, outPathFlash).write(".as");
        }
        log("Invar end: " + new Date().toString());
    }

    static HashMap<String,List<String>> parseArguments(String[] args)
    {
        HashMap<String,List<String>> mapArgs = new HashMap<String,List<String>>();
        List<String> listCurrent = null;
        for (String arg : args)
        {
            if (arg.charAt(0) == '-')
            {
                listCurrent = new LinkedList<String>();
                mapArgs.put(arg, listCurrent);
            }
            else if (listCurrent != null)
            {
                listCurrent.add(arg);
            }
        }
        return mapArgs;
    }

    static void showHelp()
    {
        StringBuilder s = new StringBuilder();
        s.append("\n");
        s.append("Description: ");
        s.append("\n  ");
        s.append("Invariable data interchange format in your project.");
        s.append("\n\n");
        s.append("Argument List: ");
        s.append("\n  ");
        s.append(ARG_HELP);
        s.append("\n  ");
        s.append(ARG_RULE_PATH);
        s.append("\n  ");
        s.append(ARG_JAVA_PATH);
        s.append("\n  ");
        s.append(ARG_FLASH_PATH);
        log(s);
    }

    static void log(Object txt)
    {
        System.out.println(txt);
    }
}