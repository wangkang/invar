package invar;

import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final public class InvarSnippet
{
    final static String                  empty           = "";
    final static String                  whiteSpace      = " ";
    final static String                  br              = "\n";
    final static String                  indent          = whiteSpace + whiteSpace + whiteSpace + whiteSpace;

    final private Document               snippetDoc;
    final private HashMap<String,String> snippetMap;
    final private String                 snippetPath;
    final private InvarContext           context;
    final private InvarWrite             writer;

    //For C++ template "> >" issue in GCC
    private Boolean                      genericOverride = false;

    public InvarSnippet(InvarContext ctx, String path, InvarWrite writer) throws Exception
    {
        this.context = ctx;
        this.writer = writer;
        this.snippetPath = "/res/" + path;
        this.snippetDoc = getSnippetDoc(this.snippetPath, ctx);
        this.snippetMap = new LinkedHashMap<String,String>();
    }

    public void buildSnippetMap (InvarContext c)
    {
        if (!snippetDoc.hasChildNodes())
            return;
        Node root = snippetDoc.getFirstChild();
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            String nameNode = n.getNodeName().toLowerCase();
            if (nameNode.equals("redefine"))
            {
                genericOverride = Boolean.parseBoolean(getAttrOptional(n, "genericOverride"));
                buildTypeRedefine(n.getChildNodes(), c);
            }
            else if (nameNode.equals("template"))
            {
                buildTemplates(n);
            }
            else if (nameNode.equals("export"))
            {
                buildExport(n);
            }
            else
            {
                // ignore
            }
        }
    }

    public String tryGet (String key, String deft)
    {
        if (!snippetMap.containsKey(key))
        {
            return deft;
        }
        return snippetMap.get(key);
    }

    public String getSnippetPath ()
    {
        return snippetPath;
    }

    public Boolean getGenericOverride ()
    {
        return genericOverride;
    }

    static private String getAttrOptional (Node node, String name)
    {
        Node n = node.getAttributes().getNamedItem(name);
        String v = empty;
        if (n != null)
        {
            v = n.getNodeValue();
        }
        return v;
    }

    private void buildTemplates (Node node)
    {
        String key = getAttrOptional(node, "key");
        key = key.replaceAll("\\s", empty);
        if (key.equals(empty))
            return;
        NodeList nodes = node.getChildNodes();
        int len = nodes.getLength();
        String cdada = empty;
        for (int i = 0; i < len; i++)
        {
            Node n = nodes.item(i);
            if (Node.CDATA_SECTION_NODE != n.getNodeType())
                continue;
            cdada = n.getTextContent();
        }
        String[] keys = key.split("\\|");
        for (String k : keys)
        {
            if (k.equals(empty))
                continue;
            snippetAdd(k, cdada);
        }
    }

    private void buildTypeRedefine (NodeList nodes, InvarContext c)
    {
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            Node n = nodes.item(i);
            if (Node.ELEMENT_NODE != n.getNodeType())
                continue;
            String typeName = n.getNodeName().toLowerCase();
            String type = getAttrOptional(n, "type");
            String pack = getAttrOptional(n, "pack");
            String generic = getAttrOptional(n, "generic");
            String initValue = getAttrOptional(n, "initValue");
            String include = getAttrOptional(n, "include");
            type = type.trim();
            pack = pack.trim();
            initValue = initValue.trim();
            InvarType buildInT = c.findBuildInType(typeName);
            if (buildInT == null)
            {
                context.addDialectType(pack, type, generic, TypeID.DIALECT, false, initValue, include);
                continue;
            }
            TypeID id = buildInT.getId();
            c.typeRedefine(id, pack, type, generic, initValue, include);
        }
    }

    private void buildExport (Node n)
    {
        String resPath = getAttrOptional(n, "resPath");
        String destDir = getAttrOptional(n, "destDir");
        String destName = getAttrOptional(n, "destName");
        writer.exportFile(resPath, destDir, destName);
    }

    private void snippetAdd (String key, String s)
    {
        String[] lines = s.split("\n|\r\n");
        StringBuilder code = new StringBuilder();
        int len = lines.length;
        for (int i = 0; i < len; i++)
        {
            String line = lines[i];
            line = line.replaceAll("(^\\s*|\\s*$)", empty);
            line = line.replaceAll("(\\s*)(" + Token.Indent //
                    + "|" + Token.Blank//
                    //+ "|" + Token.Body//
                    + ")(\\s*)", "$2");
            if (!line.equals(empty))
            {
                line = line.replaceAll(Token.Br, br);
                line = line.replaceAll(Token.Indent, indent);
                line = line.replaceAll(Token.Blank, empty);
                line = line.replaceAll(Token.Space, whiteSpace);
                code.append(line + (i != len - 1 ? br : empty));
            }
        }
        snippetMap.put(key, code.toString());
    }

    private Document getSnippetDoc (String langName, InvarContext ctx) throws Exception
    {
        String path = langName;
        InputStream res = getClass().getResourceAsStream(path);
        if (res != null)
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(res);
            if (!doc.hasChildNodes())
                return null;
            System.out.println("read  <- " + path);
            return doc;
        }
        else
        {
            throw new Exception("File doesn't exist: " + path);
        }
    }

    static public class Key
    {
        final static public String REFER_SPEC            = "refer.spec";
        final static public String REFER_INVOKE          = "refer.invoke";
        final static public String REFER_CONST           = "refer.const";
        final static public String POINTER_NULL          = "pointer.null";
        final static public String POINTER_SPEC          = "pointer.spec";
        final static public String POINTER_INVOKE        = "pointer.invoke";

        final static public String FILE                  = "file";
        final static public String FILE_PACK             = "file.pack";
        final static public String FILE_PACK_SPLIT       = "file.pack.split";
        final static public String FILE_BODY             = "file.body";
        final static public String FILE_INCLUDE          = "file.include";

        final static public String DOC                   = "doc";
        final static public String DOC_LINE              = "doc.line";
        final static public String IMPORT                = "import";
        final static public String IMPORT_SPLIT          = "import.split";
        final static public String IMPORT_BODY           = "import.body";

        final static public String NULL_BYTE_YES         = "null.byte.yes";
        final static public String NULL_BYTE_NO          = "null.byte.no";
        final static public String INIT_STRUCT           = "init.struct";
        final static public String INIT_ENUM             = "init.enum";
        final static public String CODE_ASSIGNMENT       = "code.assignment";
        final static public String CODE_DEFINITION       = "code.definition";

        final static public String CODE_FOREACH          = "code.foreach";
        final static public String CODE_FORI             = "code.fori";

        final static public String RUNTIME_PACK          = "runtime.pack";
        final static public String RUNTIME_NAME          = "runtime.name";
        final static public String RUNTIME_BODY          = "runtime.body";
        final static public String RUNTIME_ALIAS         = "runtime.alias";
        final static public String RUNTIME_ALIAS_BASIC   = "runtime.alias.basic";
        final static public String RUNTIME_ALIAS_VEC     = "runtime.alias.list";
        final static public String RUNTIME_ALIAS_MAP     = "runtime.alias.map";

        final static public String ENUM                  = "enum";
        final static public String ENUM_FIELD            = "enum.field";
        final static public String STRUCT                = "struct";
        final static public String STRUCT_META           = "struct.meta";
        final static public String STRUCT_FIELD          = "struct.field";
        final static public String STRUCT_GETTER         = "struct.getter";
        final static public String STRUCT_SETTER         = "struct.setter";

        final static public String CONSTRUCT_FIELD       = "ctor.field";
        final static public String CONSTRUCT_FIELD_SPLIT = "ctor.field.split";
    }

    static public class Token
    {
        final static String Br          = wrapToken("brk");
        final static String Concat      = wrapToken("concat");
        final static String Indent      = wrapToken("tab");
        final static String Blank       = wrapToken("blank");
        final static String Space       = wrapToken("space");
        final static String NullPtr     = wrapToken("null");
        final static String ByteNull    = wrapToken("bytenon");
        final static String ByteNotNull = wrapToken("byteyes");
        final static String Doc         = wrapToken("doc");

        final static String Define      = wrapToken("define");
        final static String Pack        = wrapToken("pack");
        final static String Import      = wrapToken("import");
        final static String Includes    = wrapToken("includes");
        final static String Enums       = wrapToken("enums");
        final static String Structs     = wrapToken("structs");
        final static String Const       = wrapToken("const");
        final static String ConstBlock  = wrapToken("constblock");

        final static String Argument    = wrapToken("arg");
        final static String SizeType    = wrapToken("sizetype");
        final static String Size        = wrapToken("len");
        final static String Key         = wrapToken("key");
        final static String Value       = wrapToken("value");
        final static String Body        = wrapToken("body");
        final static String BodyIndent  = wrapToken("bodyindent");
        final static String Invoke      = wrapToken("invoke");
        final static String Default     = wrapToken("deft");
        final static String Split       = wrapToken("split");
        final static String Index       = wrapToken("index");
        final static String IndexUpper  = wrapToken("indexupper");
        final static String Specifier   = wrapToken("spec");
        final static String SpecUpper   = wrapToken("specupper");
        final static String Name        = wrapToken("name");
        final static String NameUpper   = wrapToken("nameupper");
        final static String Type        = wrapToken("type");
        final static String TypeUpper   = wrapToken("typeupper");
        final static String RuleRight   = wrapToken("typer");
        final static String NameReal    = wrapToken("namer");

        final static String wrapToken (String name)
        {
            return "\\(#" + name + "\\)";
        }
    }
}
