package invar.io;

import invar.InvarContext;
import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeProtocol;
import invar.model.TypeStruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReadInputXml
{
    static private final String SPLIT_PACK_TYPE  = "::";
    static private final String SPLIT_VECTOR     = "-";

    static private final String ATTR_PACK_NAME   = "name";
    static private final String ATTR_STRUCT_NAME = "name";
    static private final String ATTR_ENUM_VAL    = "value";
    static private final String ATTR_COMMENT     = "doc";
    static private final String ATTR_FIELD_NAME  = "name";
    static private final String ATTR_FIELD_DEFT  = "value";
    static private final String ATTR_FIELD_ENC   = "encode";
    static private final String ATTR_FIELD_DEC   = "decode";
    static private final String XML_NODE_CLIENT  = "client";
    static private final String XML_NODE_SERVER  = "server";

    // Build in types
    static private final String BI_INT8          = "Int8";
    static private final String BI_INT16         = "Int16";
    static private final String BI_INT32         = "Int32";
    static private final String BI_INT64         = "Int64";
    static private final String BI_UINT8         = "Uint8";
    static private final String BI_UINT16        = "Uint16";
    static private final String BI_UINT32        = "Uint32";
    static private final String BI_UINT64        = "Uint64";
    static private final String BI_FLOAT         = "Float";
    static private final String BI_DOUBLE        = "Double";
    static private final String BI_BOOL          = "Bool";
    static private final String BI_STRING        = "String";
    static private final String BI_MAP           = "Map";
    static private final String BI_VECTOR        = "Vec";

    // User custom types, will be write to code file.
    static private final String BI_ENUM          = "Enum";
    static private final String BI_STRUCT        = "Struct";
    static private final String BI_PROTOCOL      = "Protoc";

    static public TreeMap<TypeID,String> makeTypeIdMap()
    {
        TreeMap<TypeID,String> map = new TreeMap<TypeID,String>();
        map.put(TypeID.INT8, BI_INT8);
        map.put(TypeID.INT16, BI_INT16);
        map.put(TypeID.INT32, BI_INT32);
        map.put(TypeID.INT64, BI_INT64);
        map.put(TypeID.UINT8, BI_UINT8);
        map.put(TypeID.UINT16, BI_UINT16);
        map.put(TypeID.UINT32, BI_UINT32);
        map.put(TypeID.UINT64, BI_UINT64);
        map.put(TypeID.FLOAT, BI_FLOAT);
        map.put(TypeID.DOUBLE, BI_DOUBLE);
        map.put(TypeID.BOOL, BI_BOOL);
        map.put(TypeID.STRING, BI_STRING);
        map.put(TypeID.MAP, BI_MAP);
        map.put(TypeID.LIST, BI_VECTOR);
        return map;
    }

    private final InvarContext context;
    private final String       pathXml;
    private final InputStream  input;
    private InvarPackage       pack;
    private List<Node>         typeNodes;

    public ReadInputXml(InvarContext ctx, InputStream input, String pathXml) throws Throwable
    {
        this.context = ctx;
        this.input = input;
        this.pathXml = pathXml;
    }

    public void parseTypes() throws Throwable
    {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(input);
        if (!doc.hasChildNodes())
            return;

        Node nPack = doc.getFirstChild();
        String packName = getAttr(nPack, ATTR_PACK_NAME);

        pack = context.findOrCreatePack(packName);
        typeNodes = new ArrayList<Node>();

        NodeList nodes = nPack.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
            {
                continue;
            }
            String nameNode = n.getNodeName().toLowerCase();
            String name = getAttr(n, ATTR_STRUCT_NAME);
            String comment = getAttrOptional(n, ATTR_COMMENT);
            InvarType t = null;
            if (nameNode.equals(BI_STRUCT.toLowerCase()))
            {
                t = new TypeStruct(name, pack, comment);
            }
            else if (nameNode.equals(BI_ENUM.toLowerCase()))
            {
                t = new TypeEnum(name, pack, comment);
            }
            else if (nameNode.equals(BI_PROTOCOL.toLowerCase()))
            {
                t = new TypeProtocol(name, pack, comment);
            }
            else
            {
                onError("Invalid xml node: " + n.getNodeName());
                continue;
            }
            pack.add(t);
            typeNodes.add(n);
        }
    }

    public void parse() throws Throwable
    {
        if (pack == null)
            return;

        for (Node n : typeNodes)
        {
            String nameNode = n.getNodeName().toLowerCase();
            String nameType = getAttr(n, ATTR_STRUCT_NAME);
            if (nameNode.equals(BI_STRUCT.toLowerCase()))
            {
                decStruct(n, pack.<TypeStruct> findType(nameType));
            }
            else if (nameNode.equals(BI_ENUM.toLowerCase()))
            {
                decEnum(n, pack.<TypeEnum> findType(nameType));
            }
            else if (nameNode.equals(BI_PROTOCOL.toLowerCase()))
            {
                decProtocol(n, pack.<TypeProtocol> findType(nameType), nameType);
            }
            else
            {
            }
        }
    }

    private void decEnum(Node node, TypeEnum type) throws NumberFormatException, DOMException, Exception
    {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            String value = getAttr(n, ATTR_ENUM_VAL);
            String comment = getAttrOptional(n, ATTR_COMMENT);
            type.addOption(n.getNodeName(), Integer.decode(value), comment);
        }
    }

    private void decStruct(Node node, TypeStruct type) throws Throwable
    {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            decStructField(n, type);
        }
    }

    private void decStructField(Node n, TypeStruct type) throws Throwable
    {
        String nodeName = n.getNodeName();
        String[] nameTypes = nodeName.split(SPLIT_VECTOR);
        if (nameTypes.length > 3)
        {
            onError("Type format is invalid: " + nodeName);
        }

        InvarType typeBasic = findType(nameTypes[0]);

        if (typeBasic.getId() == TypeID.PROTOCOL)
        {
            onError("Invalid element '" + n.getNodeName() + "' in struct '"
                    + type.getName() + "'.");
        }

        String key = getAttr(n, ATTR_FIELD_NAME);
        String comment = getAttrOptional(n, ATTR_COMMENT);

        InvarField<?> field = new InvarField<InvarType>(typeBasic, key, comment);

        switch (typeBasic.getId()){
        case ENUM:
            field = new InvarField<TypeEnum>((TypeEnum)typeBasic, key, comment);
            break;
        case STRUCT:
            field = new InvarField<TypeStruct>((TypeStruct)typeBasic, key, comment);
            break;

        case LIST:
            if (nameTypes.length != 2)
            {
                onError("Vector format is invalid: " + nodeName);
            }
            field.getGenerics().add(findType(nameTypes[1]));
            break;

        case MAP:
            InvarType typeKey = null;
            InvarType typeValue = null;
            if (nameTypes.length < 2)
            {
                onError("HashMap format is invalid: " + nodeName);
            }
            else if (nameTypes.length == 2)
            {
                typeKey = findType(BI_STRING);
                typeValue = findType(nameTypes[1]);
            }
            else
            {
                typeKey = findType(nameTypes[1]);
                typeValue = findType(nameTypes[2]);
            }
            field.getGenerics().add(typeKey);
            field.getGenerics().add(typeValue);
            break;

        default:
            field = new InvarField<InvarType>(typeBasic, key, comment);
        }

        setFieldCommonAttrs(n, field);
        type.addField(field);
    }

    private InvarType findType(String name) throws Throwable
    {
        String[] names = name.split(SPLIT_PACK_TYPE);
        String typeName = null;
        InvarPackage typePack = null;
        if (names.length == 1)
        {
            typePack = pack;
            typeName = names[0];
        }
        else if (names.length == 2)
        {
            typePack = context.findOrCreatePack(names[0]);
            typeName = names[1];
        }
        else
        {
            onError("Invalid type name: " + name);
        }
        InvarType fieldType = null;
        fieldType = context.findType(typeName, typePack);
        if (fieldType == null)
        {
            // find type in all packages
            fieldType = context.findType(typeName);
        }
        if (fieldType == null)
        {
            onError("Undefined type: " + name);
        }
        return fieldType;
    }

    private void onError(String hint) throws Exception
    {
        throw new Exception("\nFile parse error: " + pathXml + "\n" + hint);
    }

    private void setFieldCommonAttrs(Node node, InvarField<? extends InvarType> field)
    {
        String str = "";
        str = getAttrOptional(node, ATTR_FIELD_DEFT);
        if (!str.equals(""))
            field.setDefault(str);
        str = getAttrOptional(node, ATTR_FIELD_ENC);
        if (!str.equals(""))
            field.setEncode(Boolean.parseBoolean(str));
        str = getAttrOptional(node, ATTR_FIELD_DEC);
        if (!str.equals(""))
            field.setDecode(Boolean.parseBoolean(str));

    }

    private void decProtocol(Node node, TypeProtocol type, String typeName) throws Throwable
    {
        NodeList nodes = node.getChildNodes();
        Node nClient = null;
        Node nServer = null;
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
                    onError("Repeated element '" + n.getNodeName()
                            + "' in protocol '" + typeName + "'");
            }
            else if (n.getNodeName().toLowerCase().equals(XML_NODE_SERVER))
            {
                if (nServer == null)
                    nServer = n;
                else
                    onError("Repeated element '" + n.getNodeName()
                            + "' in protocol '" + typeName + "'");
            }
            else
            {
                onError("Invalid element '" + n.getNodeName()
                        + "' in protocol '" + typeName + "'");
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

    private String getAttrOptional(Node node, String name)
    {
        Node n = node.getAttributes().getNamedItem(name);
        String v = "";
        if (n != null)
            v = n.getNodeValue();
        return v;
    }

    private String getAttr(Node node, String name) throws Exception
    {
        String v = getAttrOptional(node, name);
        if (v.equals(""))
        {
            onError(formatXmlNode(node) + "\nAttribute '" + name
                    + "' is required.");
        }
        return v;
    }

    protected void log(String txt)
    {
        System.out.println(txt);
    }

    static public String makeTestXmlString(String prefix)
    {
        StringBuilder code = new StringBuilder();
        TreeMap<TypeID,String> map = makeTypeIdMap();
        Iterator<TypeID> i;
        i = map.keySet().iterator();
        while (i.hasNext())
        {
            TypeID key = i.next();
            if (TypeID.LIST == key)
                continue;
            if (TypeID.MAP == key)
                continue;
            String name = map.get(key);
            String nkey = "test" + prefix + name;
            if (prefix != "")
                name = prefix + "-" + name;
            code.append("<" + name + " ");
            code.append(ATTR_FIELD_NAME);
            code.append("=\"" + nkey + "\" ");
            code.append(ATTR_FIELD_DEFT);
            code.append("=\"" + "" + "\" ");
            code.append(ATTR_COMMENT);
            code.append("=\"" + "" + "\" ");
            code.append("/>");
            code.append("\n");
        }
        return code.toString();
    }

    private String formatXmlNode(Node node)
    {
        NamedNodeMap attrs = node.getAttributes();
        StringBuilder code = new StringBuilder();
        code.append("<" + node.getNodeName());
        int len = attrs.getLength();
        for (int i = 0; i < len; i++)
        {
            Node n = attrs.item(i);
            code.append(" " + n.toString());
        }
        code.append(" />");
        return code.toString();
    }

}