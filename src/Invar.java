import invar.InvarContext;
import invar.InvarWriteAS3;
import invar.InvarWriteJava;
import invar.io.ReadInputXml;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author wkang
 */
final public class Invar
{
    static private String outPathJava  = "temp/java/";
    static private String outPathFlash = "temp/flash/";

    static public void main(String[] args) throws Throwable
    {
        System.out.println("Start: " + new Date().toString());

        HashMap<String,List<String>> mapArgs = parseArguments(args);
        if (mapArgs.get("-help") != null)
        {
            showHelp();
            return;
        }

        InvarContext ctx = new InvarContext();
        ctx.addBuildInTypes(ReadInputXml.makeTypeIdMap());
        handleXmlInputs(ctx, mapArgs.get("-xml"));

        List<String> outPath = null;

        outPath = mapArgs.get("-java");
        if (outPath != null)
        {
            if (outPath.size() > 0)
                outPathJava = outPath.get(0);
            new InvarWriteJava().setDirRoot(outPathJava).setContext(ctx)
                    .write(".java");
        }

        outPath = mapArgs.get("-flash");
        if (outPath != null)
        {
            if (outPath.size() > 0)
                outPathFlash = outPath.get(0);
            new InvarWriteAS3().setDirRoot(outPathFlash).setContext(ctx)
                    .write(".as3");
        }

        System.out.println("End: " + new Date().toString());

        //System.out.println(ReadInputXml.makeTestXmlString(""));
        //System.out.println(ReadInputXml.makeTestXmlString("Vec"));
        //System.out.println(ReadInputXml.makeTestXmlString("Map"));
    }

    private static void handleXmlInputs(InvarContext ctx, List<String> pathXmls) throws Throwable
    {
        if (pathXmls == null || pathXmls.size() == 0)
        {
            showHelp();
            return;
        }
        else
        {
            List<ReadInputXml> xmls = new ArrayList<ReadInputXml>();
            for (String pathXml : pathXmls)
            {
                InputStream input = Invar.class.getResourceAsStream(pathXml);
                if (input == null)
                    throw new IOException("Invalid file path " + pathXml);
                xmls.add(new ReadInputXml(ctx, input, pathXml));
            }
            for (ReadInputXml x : xmls)
            {
                x.parseTypes();
            }
            // System.out.println(ctx.dumpTypeAll());
            for (ReadInputXml x : xmls)
            {
                x.parse();
            }
        }
    }
    private static HashMap<String,List<String>> parseArguments(String[] args)
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

    private static void showHelp()
    {
        StringBuilder s = new StringBuilder();
        s.append("flaspect [ -help | -xml | -java | -flash ]");
        s.append("\n");
        s.append("-help");
        s.append("\n");
        s.append("-xml");
        s.append("\n");
        s.append("-java");
        s.append("\n");
        s.append("-flash");
        s.append("\n");
        System.out.println(s);
    }

}