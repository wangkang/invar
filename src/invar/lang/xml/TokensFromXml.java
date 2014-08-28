package invar.lang.xml;

import invar.InvarContext;
import invar.lang.TokenNode;
import invar.lang.TokenParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

public final class TokensFromXml
{
    static private final String suffix = ".xml";

    static public void start (InvarContext ctx) throws Exception
    {
        File path = new File(ctx.getRuleDir());
        //log("Path: " + path.toURI().toASCIIString());
        if (!path.exists())
            return;
        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept (File dir, String name)
            {
                File f = new File(dir, name);
                if (f.getName().startsWith("."))
                    return false;
                if (f.getName().startsWith("_"))
                    return false;
                if (f.isDirectory())
                    return true;
                if (name.endsWith(TokensFromXml.suffix))
                    return true;
                return false;
            }
        };

        List<File> files = new ArrayList<File>();
        recursiveReadFile(files, path, filter);
        TokenNode root = StAX(ctx, files);
        root.freeze();

        TokenParser syntax = new TokenParser();
        syntax.init(root, ctx);
        syntax.parse(ctx);
    }

    static private void recursiveReadFile (List<File> all, File file, FilenameFilter filter)
    {
        if (all.size() > 1024)
            return;
        if (file.isFile())
            all.add(file);
        else if (file.isDirectory())
        {
            File[] files = file.listFiles(filter);
            for (int i = 0; i < files.length; i++)
                recursiveReadFile(all, files[i], filter);
        }
        else
        {
        }
    }

    static TokenNode StAX (InvarContext ctx, List<File> files) throws Exception
    {
        final TokenNode root = new TokenNode();
        final Stack<TokenNode> stack = new Stack<TokenNode>();
        final XMLInputFactory fac = XMLInputFactory.newFactory();
        fac.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        fac.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        fac.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        fac.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        fac.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        fac.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
        for (File f : files)
        {
            //convert file to appropriate URI, f.toURI().toASCIIString()
            //converts the URI to string as per rule specified in
            //RFC 2396,
            String systemId = f.toURI().toASCIIString();
            final InputStream inp = new FileInputStream(f);
            final XMLStreamReader reader = fac.createXMLStreamReader(inp, InvarContext.encoding);
            while (reader.hasNext())
            {
                int event = reader.getEventType();
                switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    StAXStartElement(reader, stack, systemId);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    StAXEndElement(reader, stack, root);
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    String hint = "read  <- " + systemId;
                    if (!reader.getVersion().equals("1.0"))
                        throw new Exception("XML version should be 1.0\n" + event + "\n" + hint);
                    log(hint);
                    break;
                default:
                    break;
                }
                reader.next();
            }
            reader.close();
        }
        return root;
    }

    private static void StAXStartElement (XMLStreamReader r, Stack<TokenNode> stack, String filePath)
    {
        String name = r.getLocalName();
        TokenNode n = new TokenNode(name);
        n.setFilePath(filePath);
        stack.push(n);
        int len = r.getAttributeCount();
        for (int i = 0; i < len; i++)
        {
            String k = r.getAttributeName(i).getLocalPart();
            String v = r.getAttributeValue(i);
            n.putAttr(k, v);
        }
    }

    private static void StAXEndElement (XMLStreamReader r, Stack<TokenNode> stack, TokenNode root) throws Exception
    {
        String name = r.getLocalName();
        if (stack.empty())
        {
            throw new Exception("No start, but end: " + name);
        }
        TokenNode n = stack.peek();
        if (n.getName().equals(name))
        {
            n = stack.pop();
            TokenNode p = stack.empty() ? root : stack.peek();
            p.addChild(n);
        }
        else
        {
            throw new Exception("Redundant end element: " + name);
        }
    }

    static private void log (Object txt)
    {
        System.out.println(txt);
    }

}
