import invar.InvarContext;
import invar.InvarMainArgs;
import invar.InvarReadRule;
import invar.InvarWriteCode;
import invar.InvarWriteXSD;
import invar.model.InvarType.TypeID;
import java.util.Date;
import java.util.TreeMap;

final public class Invar
{
    static final String ARG_HELP        = "help";
    static final String ARG_RULE_PATH   = "rule";
    static final String ARG_XSD_PATH    = "xsd";
    static final String ARG_JAVA_PATH   = "java";
    static final String ARG_FLASH_PATH  = "flash";
    static final String ARG_CSHARP_PATH = "csharp";
    static final String ARG_CPP_PATH    = "cpp";

    static public void main (String[] args)
    {
        InvarMainArgs a = new InvarMainArgs();
        a.addDefault(ARG_RULE_PATH, "rule/");
        a.addDefault(ARG_XSD_PATH, "code/xsd/");
        a.addDefault(ARG_JAVA_PATH, "code/java/");
        a.addDefault(ARG_FLASH_PATH, "code/flash/");
        a.addDefault(ARG_CSHARP_PATH, "code/csharp/");
        a.addDefault(ARG_CPP_PATH, "code/cpp/");
        a.parseArguments(args);

        if (a.has(ARG_HELP))
        {
            showHelp();
            return;
        }

        TreeMap<TypeID,String> basics = InvarReadRule.makeTypeIdMap();
        try
        {
            long startMS = System.currentTimeMillis();
            log("Invar start: " + new Date().toString());
            InvarContext ctx = new InvarContext();
            ctx.addBuildInTypes(basics);
            ctx.setRuleDir(a.get(ARG_RULE_PATH));

            log("");
            InvarReadRule.start(ctx, ".xml");

            if (a.has(ARG_XSD_PATH))
            {
                log("");
                new InvarWriteXSD().write(ctx, basics, a.get(ARG_XSD_PATH));
            }
            if (a.has(ARG_FLASH_PATH))
            {
                log("");
                new InvarWriteCode(ctx, ARG_FLASH_PATH, a.get(ARG_FLASH_PATH)).write(".as");
            }
            if (a.has(ARG_CSHARP_PATH))
            {
                log("");
                new InvarWriteCode(ctx, ARG_CSHARP_PATH, a.get(ARG_CSHARP_PATH)).write(".cs");
            }
            if (a.has(ARG_JAVA_PATH))
            {
                log("");
                new InvarWriteCode(ctx, ARG_JAVA_PATH, a.get(ARG_JAVA_PATH)).write(".java");
            }
            if (a.has(ARG_CPP_PATH))
            {
                log("");
                new InvarWriteCode(ctx, ARG_CPP_PATH, a.get(ARG_CPP_PATH), "snippet.h.xml").write(".h");
                new InvarWriteCode(ctx, ARG_CPP_PATH, a.get(ARG_CPP_PATH), "snippet.cc.xml").write(".cc", true);
            }
            log("\nInvar end: " + (System.currentTimeMillis() - startMS) + "ms");
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    static void showHelp ()
    {
        StringBuilder s = new StringBuilder(256);
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
