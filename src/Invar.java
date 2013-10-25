import invar.InvarContext;
import invar.InvarMainArgs;
import invar.InvarReadRule;
import invar.InvarWriteAS3;
import invar.InvarWriteJava;
import invar.InvarWriteUnity;
import invar.InvarWriteXSD;
import invar.model.InvarType.TypeID;
import java.util.Date;
import java.util.TreeMap;

final public class Invar
{
    static final String ARG_HELP       = "-help";
    static final String ARG_RULE_PATH  = "-rule";
    static final String ARG_JAVA_PATH  = "-java";
    static final String ARG_FLASH_PATH = "-flash";
    static final String ARG_UNITY_PATH = "-csharp";
    static final String ARG_XSD_PATH   = "-xsd";

    static public void main (String[] args)
    {
        InvarMainArgs a = new InvarMainArgs();
        a.addDefault(ARG_RULE_PATH, "rule/");
        a.addDefault(ARG_XSD_PATH, "data/");
        a.addDefault(ARG_JAVA_PATH, "code/java/");
        a.addDefault(ARG_FLASH_PATH, "code/flash/");
        a.addDefault(ARG_UNITY_PATH, "code/csharp/");
        a.parseArguments(args);

        if (a.has(ARG_HELP))
        {
            showHelp();
            return;
        }

        TreeMap<TypeID,String> basics = InvarReadRule.makeTypeIdMap();
        try
        {
            log("Invar start: " + new Date().toString());

            InvarContext ctx = new InvarContext();
            ctx.addBuildInTypes(basics);

            if (a.has(ARG_UNITY_PATH))
            {
                ctx.ghostAdd("UnityEngine", "MonoBehaviour", "");
            }

            InvarReadRule.start(a.get(ARG_RULE_PATH), ".xml", ctx);

            if (a.has(ARG_JAVA_PATH))
                new InvarWriteJava(ctx, a.get(ARG_JAVA_PATH)).write(".java");
            if (a.has(ARG_FLASH_PATH))
                new InvarWriteAS3(ctx, a.get(ARG_FLASH_PATH)).write(".as");
            if (a.has(ARG_XSD_PATH))
                new InvarWriteXSD().write(ctx, basics, a.get(ARG_XSD_PATH));
            if (a.has(ARG_UNITY_PATH))
            {
                new InvarWriteUnity(ctx, a.get(ARG_UNITY_PATH)).write(".cs");
            }
            log("Invar end: " + new Date().toString());
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    static void showHelp ()
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

    static void log (Object txt)
    {
        System.out.println(txt);
    }
}
