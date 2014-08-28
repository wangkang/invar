package invar.lang.xml;

import invar.InvarContext;
import invar.lang.TokenNode;
import invar.lang.TokenParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public final class TokensFromXml
{
    static private final String suffix = ".xml";

    static public void start (InvarContext ctx) throws Exception
    {
        File path = new File(ctx.getRuleDir());
        log("Rule Path: " + path.getAbsolutePath());
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
        final XMLInputFactory inpFac = XMLInputFactory.newInstance();
        inpFac.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inpFac.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        for (File f : files)
        {
            //convert file to appropriate URI, f.toURI().toASCIIString()
            //converts the URI to string as per rule specified in
            //RFC 2396,
            String systemId = f.toURI().toASCIIString();
            //log("read  <- " + systemId);
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

            final XMLEventReader reader = factory.createXMLEventReader(new FileInputStream(f), InvarContext.encoding);
            XMLEvent event = null;
            while (reader.hasNext())
            {
                event = reader.peek();
                switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StAXStartElement(event.asStartElement(), stack, systemId);
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    StAXEndElement(event.asEndElement(), stack, root);
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                    String hint = "read  <- " + systemId;
                    StartDocument s = (StartDocument)event;
                    if (!s.getVersion().equals("1.0"))
                    {
                        throw new Exception("XML version should be 1.0\n" + event + "\n" + hint);
                    }
                    log(hint);
                    break;
                default:
                    break;
                }
                reader.nextEvent();
            }
            reader.close();
        }
        return root;
    }

    private static void StAXStartElement (StartElement e, Stack<TokenNode> stack, String filePath)
    {
        String name = e.getName().getLocalPart();
        TokenNode n = new TokenNode(name);
        n.setFilePath(filePath);
        stack.push(n);
        @SuppressWarnings("unchecked") Iterator<Attribute> iter = e.getAttributes();
        if (iter == null)
        {
            return;
        }
        while (iter.hasNext())
        {
            Attribute attr = iter.next();
            String k = attr.getName().getLocalPart();
            String v = attr.getValue();
            n.putAttr(k, v);
        }
        //System.out.println(stack.size() + " +++ " + qName + "  " + attributes.getValue("name"));
    }

    private static void StAXEndElement (EndElement e, Stack<TokenNode> stack, TokenNode root) throws Exception
    {
        String name = e.getName().getLocalPart();
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
            //System.out.println(stack.size() + " --- " + qName + "  " + n.getAttr("name"));
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
