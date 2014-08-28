package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeProtocol;
import invar.model.TypeStruct;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final public class InvarReadRule
{
    static private String suffix;

    static public void start (InvarContext ctx, String suffix) throws Throwable
    {
        InvarReadRule.suffix = suffix;
        File file = new File(ctx.getRuleDir());
        log("Rule Path: " + file.getAbsolutePath());
        if (!file.exists())
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
                if (name.endsWith(InvarReadRule.suffix))
                    return true;
                return false;
            }
        };
        List<File> files = new ArrayList<File>();
        recursiveReadFile(files, file, filter);
        List<InvarReadRule> xmls = new ArrayList<InvarReadRule>();
        for (File f : files)
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
            if (!doc.hasChildNodes())
                return;
            log("read  <- " + f.getAbsolutePath());
            InvarReadRule read = new InvarReadRule(ctx, f.getAbsolutePath());
            read.build(doc);
            xmls.add(read);
        }
        for (InvarReadRule x : xmls)
        {
            x.parse();
        }
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

    static private final String SPLIT_PACK_TYPE      = "::";
    static private final String SPLIT_GNERICS        = "-";
    static private final String ATTR_COMMENT         = "doc";
    static private final String ATTR_PACK_NAME       = "name";
    static private final String ATTR_STRUCT_NAME     = "name";
    static private final String ATTR_STRUCT_CHARSET  = "charset";
    static private final String ATTR_STRUCT_ALIAS    = "alias";
    static private final String ATTR_STRUCT_SUPER    = "super";
    static private final String ATTR_STRUCT_SHORT    = "short";

    static private final String ATTR_FIELD_NAME      = "name";
    static private final String ATTR_FIELD_TYPE      = "type";
    static private final String ATTR_FIELD_DEFT      = "value";
    static private final String ATTR_FIELD_NO_SETTER = "nosetter";
    static private final String ATTR_FIELD_USE_REF   = "useref";
    static private final String ATTR_FIELD_USE_PTR   = "useptr";

    static private final String ATTR_ENUM_VAL        = "value";
    static private final String XML_NODE_CLIENT      = "client";
    static private final String XML_NODE_SERVER      = "server";

    //User custom types, will be write to code file.
    static private final String EXT_ENUM             = "Enum";
    static private final String EXT_STRUCT           = "Struct";
    static private final String EXT_PROTOCOL         = "Protoc";

    static private void log (Object txt)
    {
        System.out.println(txt);
    }

    private final InvarContext                     context;
    private final String                           pathXml;
    private final LinkedHashMap<TypeEnum,Node>     enumNodes;
    private final LinkedHashMap<TypeStruct,Node>   structNodes;
    private final LinkedHashMap<TypeProtocol,Node> protocNodes;
    private InvarPackage                           pack;

    public InvarReadRule(InvarContext ctx, String pathXml)
    {
        this.context = ctx;
        this.pathXml = pathXml;
        enumNodes = new LinkedHashMap<TypeEnum,Node>();
        structNodes = new LinkedHashMap<TypeStruct,Node>();
        protocNodes = new LinkedHashMap<TypeProtocol,Node>();
    }

    private void build (Document doc) throws Exception
    {
        if (!doc.hasChildNodes())
            return;
        Node nPack = doc.getFirstChild();
        String packName = getAttr(nPack, ATTR_PACK_NAME);
        pack = context.findOrCreatePack(packName);
        NodeList nodes = nPack.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;

            String nameNode = n.getNodeName().toLowerCase();
            String name = getAttr(n, ATTR_STRUCT_NAME);
            String comment = getAttrOptional(n, ATTR_COMMENT);
            String alias = getAttrOptional(n, ATTR_STRUCT_ALIAS);
            String superName = getAttrOptional(n, ATTR_STRUCT_SUPER);
            String shortFd = getAttrOptional(n, ATTR_STRUCT_SHORT);

            if (!alias.equals("") && context.aliasGet(alias) != null)
                onError(n, "Repeated alias: " + alias);
            if (nameNode.equals(EXT_STRUCT.toLowerCase()))
            {
                TypeStruct t = new TypeStruct(name, pack, comment);
                addToPack(t, n);
                structNodes.put(t, n);
                if (!alias.equals(""))
                {
                    t.setAlias(alias);
                    context.aliasAdd(t);
                }
                if (!shortFd.equals(""))
                {
                    t.setShortField(shortFd);
                }
                t.setCharset(getAttrOptional(n, ATTR_STRUCT_CHARSET));
                if (!superName.equals(""))
                {
                    t.setSuperType(searchType(superName, n));
                }
            }
            else if (nameNode.equals(EXT_ENUM.toLowerCase()))
            {
                TypeEnum t = new TypeEnum(name, pack, comment);
                addToPack(t, n);
                enumNodes.put(t, n);
                if (alias.equals(""))
                {
                    t.setAlias(name);
                    context.aliasAdd(t);
                }
            }
            else if (nameNode.equals(EXT_PROTOCOL.toLowerCase()))
            {
                TypeProtocol t = new TypeProtocol(name, pack, comment);
                addToPack(t, n);
                protocNodes.put(t, n);
            }
            else
            {
                log("Invalid xml node: " + formatXmlNode(n));
                continue;
            }
        }
    }

    private void parse () throws Exception
    {
        if (pack == null)
            return;
        Iterator<TypeEnum> ie = enumNodes.keySet().iterator();
        while (ie.hasNext())
        {
            TypeEnum t = ie.next();
            Node n = enumNodes.get(t);
            decEnum(n, t);
        }
        Iterator<TypeStruct> is = structNodes.keySet().iterator();
        while (is.hasNext())
        {
            TypeStruct t = is.next();
            Node n = structNodes.get(t);
            decStruct(n, t);
        }
        Iterator<TypeProtocol> ip = protocNodes.keySet().iterator();
        while (ip.hasNext())
        {
            TypeProtocol t = ip.next();
            Node n = protocNodes.get(t);
            decProtocol(n, t);
        }
    }

    private void decEnum (Node node, TypeEnum type) throws Exception
    {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            String name = getAttr(n, ATTR_STRUCT_NAME);
            String value = getAttr(n, ATTR_ENUM_VAL);
            String comment = getAttrOptional(n, ATTR_COMMENT);
            try
            {
                Integer v = Integer.decode(value);
                type.addOption(name, v, comment);
            }
            catch (NumberFormatException e)
            {
                onError(n, "Not an inteager: " + value);
            }
        }
    }

    private void decStruct (Node node, TypeStruct type) throws Exception
    {
        NodeList nodes = node.getChildNodes();
        int index = 0;
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            decStructField(n, type, index++);
        }
    }

    private void decProtocol (Node node, TypeProtocol type) throws Exception
    {
        NodeList nodes = node.getChildNodes();
        Node nClient = null;
        Node nServer = null;
        String pName = type.getName();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            if (n.getNodeName().toLowerCase().equals(XML_NODE_CLIENT))
            {
                if (nClient == null)
                    nClient = n;
                else
                    onError(node, "Repeated client in protocol '" + pName + "'");
            }
            else if (n.getNodeName().toLowerCase().equals(XML_NODE_SERVER))
            {
                if (nServer == null)
                    nServer = n;
                else
                    onError(node, "Repeated server in protocol '" + pName + "'");
            }
            else
            {
                onError(node, "Invalid element in protocol '" + pName + "'");
            }
        }
        if (nClient != null)
        {
            decStruct(nClient, type.getClient());
            type.setNoClient(false);
        }
        if (nClient != null)
        {
            decStruct(nServer, type.getServer());
            type.setNoServer(false);
        }
    }

    private void decStructField (Node n, TypeStruct type, int i) throws Exception
    {
        String nodeName = getAttr(n, ATTR_FIELD_TYPE);
        String[] nameTypes = nodeName.split(SPLIT_GNERICS);
        if (nameTypes.length > 8)
        {
            onError(n, "Invalid node in struct '" + type.getName() + "': Max type count is 8.");
        }
        //First type of the field
        InvarType typeBasic = searchType(nameTypes[0], n);
        if (typeBasic.getId() == TypeID.PROTOCOL)
        {
            onError(n, "Invalid node in struct '" + type.getName() + "': Protocol type can not be here.");
        }
        String key = getAttr(n, ATTR_FIELD_NAME);
        String comment = getAttrOptional(n, ATTR_COMMENT);

        boolean isStructSelf = (typeBasic == type);
        boolean useRef = typeBasic.getId().getUseRefer();
        boolean usePtr = false;
        boolean disableSetter = false;
        switch (typeBasic.getId()) {
        case VEC:
        case MAP:
            disableSetter = true;
            break;
        default:
            break;
        }
        Node attr = n.getAttributes().getNamedItem(ATTR_FIELD_NO_SETTER);
        if (attr != null)
        {
            disableSetter = Boolean.parseBoolean(attr.getNodeValue());
        }
        attr = n.getAttributes().getNamedItem(ATTR_FIELD_USE_PTR);
        if (attr != null)
        {
            usePtr = Boolean.parseBoolean(attr.getNodeValue());
        }
        attr = n.getAttributes().getNamedItem(ATTR_FIELD_USE_REF);
        if (attr != null)
        {
            useRef = Boolean.parseBoolean(attr.getNodeValue());
        }

        InvarField field = null;

        switch (typeBasic.getId()) {
        case ENUM:
            field = new InvarField(i, typeBasic, key, comment, false);
            break;
        case STRUCT:
            field = new InvarField(i, typeBasic, key, comment, disableSetter);
            break;
        default:
            field = new InvarField(i, typeBasic, key, comment, disableSetter);
        }

        field.setUseReference(useRef);
        field.setUsePointer(isStructSelf || typeBasic.getId().getNullable() && usePtr);

        List<String> names = fixNameTypes(nameTypes, n);
        parseGenerics(field.getGenerics(), names, 1, n);
        setFieldCommonAttrs(n, field);
        type.addField(field);
    }

    private void parseGenerics (List<InvarType> generics, List<String> nameTypes, int i, Node n) throws Exception
    {
        int len = nameTypes.size();
        if (i >= len)
            return;
        String nameType = nameTypes.get(i);
        InvarType type = searchType(nameType, n);
        generics.add(type);
        parseGenerics(generics, nameTypes, ++i, n);
    }

    private void setFieldCommonAttrs (Node node, InvarField field)
    {
        String str = "";
        str = getAttrOptional(node, ATTR_FIELD_DEFT);
        field.setDefault(str != null ? str : "");
    }

    private String getAttrOptional (Node node, String name)
    {
        Node n = node.getAttributes().getNamedItem(name);
        String v = "";
        if (n != null)
            v = n.getNodeValue();
        return v;
    }

    private String getAttr (Node node, String name) throws Exception
    {
        String v = getAttrOptional(node, name);
        if (v.equals(""))
        {
            onError(node, "Attribute '" + name + "' is required.");
        }
        return v;
    }

    private List<String> fixNameTypes (final String[] nameTypes, Node n) throws Exception
    {
        List<String> names = new LinkedList<String>();
        int len = nameTypes.length;
        for (int i = 0; i < len; i++)
        {
            String name = nameTypes[i];
            names.add(name);
            InvarType type = searchType(name, n);
            if (TypeID.VEC == type.getId())
            {
                if (i == len - 1)
                    names.add(TypeID.INT32.getName());
            }
            else if (TypeID.MAP == type.getId())
            {
                if (i == len - 1)
                {
                    names.add(TypeID.STRING.getName());
                    names.add(TypeID.INT32.getName());
                }
                else if (i == len - 2)
                {
                    names.add(TypeID.STRING.getName());
                }
            }
            else
            {
            }
        }
        return names;
    }

    private InvarType searchType (final String name, final Node n) throws Exception
    {
        String[] names = name.split(SPLIT_PACK_TYPE);
        String typeName = null;
        InvarPackage typePack = null;
        InvarType fieldType = null;
        if (names.length == 1)
        {
            typeName = names[0];
        }
        else if (names.length == 2)
        {
            typePack = context.findOrCreatePack(names[0]);
            typeName = names[1];
        }
        else
        {
            onError(n, "Invalid type name: " + name);
        }
        fieldType = context.findBuildInType(typeName);
        if (fieldType != null)
            return fieldType;
        if (typePack != null)
        {
            fieldType = typePack.getType(typeName);
            if (fieldType != null)
                return fieldType;
        }
        else
        {
            List<InvarType> types = context.findTypes(typeName);
            if (types.size() == 1)
            {
                fieldType = types.get(0);
                return fieldType;
            }
            else if (types.size() > 1)
            {
                StringBuilder s = new StringBuilder();
                for (InvarType t : types)
                    s.append("\n" + t.fullName(SPLIT_PACK_TYPE));
                onError(n, "Find " + types.size() + " types. You should select one." + s.toString());
            }
            else
            {
            }
        }
        if (fieldType == null)
        {
            onError(n, "Undefined type: " + name);
        }
        return fieldType;
    }

    private void addToPack (InvarType t, Node n) throws Exception
    {
        if (pack.getType(t.getName()) != null)
        {
            onError(n, "Repeated type name: " + t.getName());
        }
        pack.add(t);
    }

    private void onError (Node n, String hint) throws Exception
    {
        throw new Exception(hint + "\n" + formatXmlNode(n) + "\n" + pathXml);
    }

    private String formatXmlNode (Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        StringBuilder code = new StringBuilder();
        code.append("<" + n.getNodeName());
        int len = attrs.getLength();
        for (int i = 0; i < len; i++)
        {
            Node a = attrs.item(i);
            code.append(" " + a.toString());
        }
        code.append(" />");
        return code.toString();
    }
}
