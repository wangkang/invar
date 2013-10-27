import invar.InvarContext;
import invar.InvarMainArgs;
import invar.InvarReadRule;
import invar.InvarWriteAS3;
import invar.InvarWriteCode;
import invar.InvarWriteXSD;
import invar.model.InvarType.TypeID;
import java.io.File;
import java.util.Date;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

final public class Invar
{
    static final String ARG_HELP        = "help";
    static final String ARG_RULE_PATH   = "rule";
    static final String ARG_JAVA_PATH   = "java";
    static final String ARG_FLASH_PATH  = "flash";
    static final String ARG_CSHARP_PATH = "csharp";
    static final String ARG_XSD_PATH    = "xsd";

    static public void main (String[] args)
    {
        InvarMainArgs a = new InvarMainArgs();
        a.addDefault(ARG_RULE_PATH, "rule/");
        a.addDefault(ARG_XSD_PATH, "data/");
        a.addDefault(ARG_JAVA_PATH, "code/java/");
        a.addDefault(ARG_FLASH_PATH, "code/flash/");
        a.addDefault(ARG_CSHARP_PATH, "code/csharp/");
        a.parseArguments(args);

        if (a.has(ARG_HELP))
        {
            showHelp();
            return;
        }

        TreeMap<TypeID,String> basics = InvarReadRule.makeTypeIdMap();
        try
        {
            long startMS = new Date().getTime();
            log("Invar start: " + new Date().toString());
            InvarContext ctx = new InvarContext();
            ctx.addBuildInTypes(basics);
            ctx.setRuleDir(a.get(ARG_RULE_PATH));

            log("");
            InvarReadRule.start(ctx, ".xml");

            if (a.has(ARG_CSHARP_PATH))
            {
                log("");
                new InvarWriteCode(ctx, getTemplateDoc(ARG_CSHARP_PATH, ctx), a.get(ARG_CSHARP_PATH)).write(".cs");
            }
            if (a.has(ARG_JAVA_PATH))
            {
                log("");
                //new InvarWriteJava(ctx, a.get(ARG_JAVA_PATH)).write(".java");
                new InvarWriteCode(ctx, getTemplateDoc(ARG_JAVA_PATH, ctx), a.get(ARG_JAVA_PATH)).write(".java");
            }
            if (a.has(ARG_FLASH_PATH))
            {
                log("");
                new InvarWriteAS3(ctx, a.get(ARG_FLASH_PATH)).write(".as");
            }
            if (a.has(ARG_XSD_PATH))
            {
                log("");
                new InvarWriteXSD().write(ctx, basics, a.get(ARG_XSD_PATH));
            }

            log("\nInvar end: " + (new Date().getTime() - startMS) + "ms");
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    private static Document getTemplateDoc (String langName, InvarContext ctx) throws Exception
    {
        String templateFileName = "_" + langName + ".xml";
        File file = new File(ctx.getRuleDir() + "/" + templateFileName);
        if (file.exists())
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            if (!doc.hasChildNodes())
                return null;
            log("Read <- " + file.getAbsolutePath());
            return doc;
        }
        return null;
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
