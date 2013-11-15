package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InvarWriteCode extends InvarWrite
{
    @Override
    protected Boolean beforeWrite (InvarContext c)
    {
        buildSnippetMap(c);
        return true;
    }

    @Override
    protected String codeEnum (TypeEnum type)
    {
        String block = makeEnumBlock(type);
        String s = snippetGet(Key.ENUM);
        s = replace(s, tokenName, type.getName());
        s = replace(s, tokenBlock, block);
        s = replace(s, tokenDoc, makeDoc(type.getComment()));
        return makePack(type.getPack(), s);
    }

    @Override
    protected String codeStruct (TypeStruct type)
    {
        TreeSet<String> imps = new TreeSet<String>();
        String block = makeStructBlock(type, imps);
        String s = snippetGet(Key.STRUCT);
        s = replace(s, tokenName, type.getName());
        s = replace(s, tokenBlock, block);
        s = replace(s, tokenDoc, makeDoc(type.getComment()));
        return makePack(type.getPack().getName(), s, imps);
    }

    @Override
    protected void codeRuntime (String suffix)
    {
        String fileDir = snippetGet(Key.RUNTIME_PACK);
        String typeName = snippetGet(Key.RUNTIME_NAME);
        TreeSet<String> imps = new TreeSet<String>();
        String block = makeRuntimeBlock(imps);
        String s = snippetGet(Key.STRUCT);
        s = replace(s, tokenName, typeName);
        s = replace(s, tokenBlock, block);
        s = replace(s, tokenDoc, empty);
        addExportFile(fileDir, typeName + suffix, makePack(fileDir, s, imps));
    }

    final static protected String empty          = "";
    final static protected String whiteSpace     = " ";
    final static protected String br             = "\n";
    final static protected String indent         = whiteSpace + whiteSpace + whiteSpace + whiteSpace;

    final static protected String tokenPrefix    = "\\(#";
    final static protected String tokenSuffix    = "\\)";
    final static protected String tokenBr        = tokenPrefix + "brk" + tokenSuffix;
    final static protected String tokenIndent    = tokenPrefix + "tab" + tokenSuffix;
    final static protected String tokenBlank     = tokenPrefix + "blank" + tokenSuffix;

    final static protected String tokenDoc       = tokenPrefix + "doc" + tokenSuffix;
    final static protected String tokenMeta      = tokenPrefix + "meta" + tokenSuffix;
    final static protected String tokenValue     = tokenPrefix + "value" + tokenSuffix;
    final static protected String tokenBlock     = tokenPrefix + "block" + tokenSuffix;
    final static protected String tokenImport    = tokenPrefix + "import" + tokenSuffix;
    final static protected String tokenPack      = tokenPrefix + "pack" + tokenSuffix;
    final static protected String tokenType      = tokenPrefix + "type" + tokenSuffix;
    final static protected String tokenTypeHost  = tokenPrefix + "typehost" + tokenSuffix;
    final static protected String tokenName      = tokenPrefix + "name" + tokenSuffix;
    final static protected String tokenNameUpper = tokenPrefix + "nameupper" + tokenSuffix;

    final protected class Key
    {
        final static public String DOC                 = "doc";
        final static public String DOC_LINE            = "doc.line";
        final static public String IMPORT              = "import";
        final static public String INIT_STRUCT         = "init.struct";
        final static public String INIT_ENUM           = "init.enum";

        final static public String RUNTIME_PACK        = "runtime.pack";
        final static public String RUNTIME_NAME        = "runtime.name";
        final static public String RUNTIME_BODY        = "runtime.body";
        final static public String RUNTIME_ALIAS       = "runtime.alias";
        final static public String RUNTIME_ALIAS_BASIC = "runtime.alias.basic";
        final static public String RUNTIME_ALIAS_VEC   = "runtime.alias.list";
        final static public String RUNTIME_ALIAS_MAP   = "runtime.alias.map";

        final static public String PACK                = "pack";
        final static public String ENUM                = "enum";
        final static public String ENUM_FIELD          = "enum.field";
        final static public String STRUCT              = "struct";
        final static public String STRUCT_META         = "struct.meta";
        final static public String STRUCT_FIELD        = "struct.field";
        final static public String STRUCT_GETTER       = "struct.getter";
        final static public String STRUCT_SETTER       = "struct.setter";
    }

    final private Document               snippetDoc;
    final private HashMap<String,String> snippetMap;

    public InvarWriteCode(InvarContext ctx, String langName, String dirRootPath) throws Exception
    {
        super(ctx, dirRootPath);
        this.snippetDoc = getSnippetDoc(langName, ctx);
        this.snippetMap = new LinkedHashMap<String,String>();
    }

    protected String makeDocLine (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC_LINE);
        s = replace(s, tokenDoc, comment);
        return s;
    }

    protected String makeDoc (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC);
        s = replace(s, tokenDoc, comment);
        return s;
    }

    protected String makePack (InvarPackage pack, String blockCode)
    {
        return makePack(pack.getName(), blockCode, null);
    }

    protected String makePack (String packName, String blockCode, TreeSet<String> imps)
    {
        String s = snippetGet(Key.PACK);
        s = replace(s, tokenName, packName);
        s = replace(s, tokenBlock, blockCode);
        if (imps == null || imps.size() == 0)
        {
            s = replace(s, tokenImport, empty);
        }
        else
        {
            StringBuilder code = new StringBuilder();
            for (String key : imps)
            {
                if (key.equals(empty))
                    continue;
                code.append(key);
            }
            s = replace(s, tokenImport, code.toString());
        }
        return s;
    }

    protected String makeEnumBlock (TypeEnum type)
    {
        StringBuilder code = new StringBuilder();
        Iterator<String> i = type.getKeys().iterator();
        int lenDoc = 1;
        int lenKey = 1;
        int lenVal = 1;
        while (i.hasNext())
        {
            String key = i.next();
            if (key.length() > lenKey)
                lenKey = key.length();
            if (type.getValue(key).toString().length() > lenVal)
                lenVal = type.getValue(key).toString().length();
            if (type.getComment(key).length() > lenDoc)
                lenDoc = type.getComment(key).length();
        }
        i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            String s = snippetGet(Key.ENUM_FIELD);
            s = replace(s, tokenName, fixedLen(lenKey, key));
            s = replace(s, tokenName, key);
            s = replace(s, tokenValue, fixedLenBackward(whiteSpace, lenVal, type.getValue(key).toString()));
            s = replace(s, tokenDoc, makeDoc(type.getComment(key)));
            code.append(s);
        }
        return code.toString();
    }

    private String makeStructBlock (TypeStruct type, TreeSet<String> imps)
    {
        List<InvarField> fs = type.listFields();
        List<InvarField> genericFields = new LinkedList<InvarField>();

        if (type.getSuperType() != null)
            impsCheckAdd(imps, type.getSuperType());
        int widthType = 1;
        int widthName = 1;
        int widthDeft = 1;
        for (InvarField f : fs)
        {
            f.makeTypeFormatted(getContext());
            f.setDeftFormatted(makeStructFieldInit(f, type));

            if (f.getDeftFormatted().length() > widthDeft)
                widthDeft = f.getDeftFormatted().length();
            if (f.getTypeFormatted().length() > widthType)
                widthType = f.getTypeFormatted().length();
            if (f.getKey().length() > widthName)
                widthName = f.getKey().length();

            impsCheckAdd(imps, f.getType().getRedirect());
            for (InvarType typeGene : f.getGenerics())
            {
                impsCheckAdd(imps, typeGene.getRedirect());
            }
            //imps.add("invar.InvarRule");
            switch (f.getType().getId()) {
            case MAP:
            case LIST:
                genericFields.add(f);
                break;
            default:
                break;
            }
        }
        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();
        for (InvarField f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthName);
            f.setWidthDefault(widthDeft);
            fields.append(makeStructField(f, type));
            setters.append(makeStructSetter(f, type));
            getters.append(makeStructGetter(f));
        }
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(setters);
        body.append(getters);

        //        body.append(codeToXmlStruct(type));
        //        for (InvarField f : genericFields)
        //        {
        //            StringBuilder code = new StringBuilder();
        //            code.append("StringBuilder code = new StringBuilder(); ");
        //            String rule = f.evalGenericsFull(getContext(), ".");
        //            code.append(codeToXmlNode(0, rule, new StringBuilder(), f.getKey(), f.getKey(), "", ""));
        //            code.append(brIndent2 + "return code;");
        //            String mName = "public StringBuilder toXml" + upperHeadChar(f.getKey()) + "()";
        //            body.append(br);
        //            body.append(codeMethod(mName, code.toString()));
        //        }

        return body.toString();
    }

    private StringBuilder makeStructMeta (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_META);
        s = replace(s, tokenType, f.evalGenerics(getContext(), "."));
        s = replace(s, tokenName, f.getShortName());
        code.append(s);
        return code;
    }

    protected StringBuilder makeStructField (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_FIELD);
        s = replace(s, tokenName, fixedLen(f.getWidthKey(), f.getKey()));
        s = replace(s, tokenType, fixedLen(f.getWidthType(), f.getTypeFormatted()));
        s = replace(s, tokenValue, fixedLen(f.getWidthDefault(), f.getDeftFormatted()));
        s = replace(s, tokenDoc, makeDocLine(f.getComment()));
        code.append(s);
        return code;
    }

    private StringBuilder makeStructSetter (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        if (TypeID.LIST == f.getType().getId())
            return code;
        if (TypeID.MAP == f.getType().getId())
            return code;
        String s = snippetGet(Key.STRUCT_SETTER);
        s = replace(s, tokenTypeHost, struct.getName());
        s = replace(s, tokenMeta, makeStructMeta(f).toString());
        s = replace(s, tokenType, f.getTypeFormatted());
        s = replace(s, tokenName, f.getKey());
        s = replace(s, tokenNameUpper, upperHeadChar(f.getKey()));
        s = replace(s, tokenDoc, makeDoc(f.getComment()));
        code.append(s);
        return code;
    }

    private StringBuilder makeStructGetter (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_GETTER);
        s = replace(s, tokenMeta, makeStructMeta(f).toString());
        s = replace(s, tokenType, f.getTypeFormatted());
        s = replace(s, tokenName, f.getKey());
        s = replace(s, tokenNameUpper, upperHeadChar(f.getKey()));
        s = replace(s, tokenDoc, makeDoc(f.getComment()));
        code.append(s);
        return code;
    }

    protected String makeStructFieldInit (InvarField f, TypeStruct struct)
    {
        if (f.getType() == struct)
            return "null";
        String deft = f.getDefault();
        InvarType type = f.getType();
        if (deft != null && !deft.equals(empty))
        {
            return type.getInitPrefix() + deft + type.getInitSuffix();
        }
        String s = null;
        switch (type.getRealId()) {
        case STRUCT:
        case LIST:
        case MAP:
            String t = f.getTypeFormatted();
            s = snippetGet(Key.INIT_STRUCT);
            s = replace(s, tokenType, t);
            return s;
        case ENUM:
            TypeEnum tEnum = (TypeEnum)type;
            s = snippetGet(Key.INIT_ENUM);
            s = replace(s, tokenType, tEnum.getName());
            s = replace(s, tokenName, tEnum.firstOptionKey());
            return s;
        default:
            return type.getInitPrefix() + type.getInitValue() + type.getInitSuffix();
        }

    }

    private String makeRuntimeBlock (TreeSet<String> imps)
    {
        String s = snippetGet(Key.RUNTIME_BODY);
        String block = makeRuntimeAliasBlock(imps);
        s = replace(s, tokenBlock, block);
        return s;
    }

    private String makeRuntimeAliasBlock (TreeSet<String> imps)
    {
        StringBuilder meBasic = new StringBuilder();
        StringBuilder meEnums = new StringBuilder();
        StringBuilder meStruct = new StringBuilder();
        Iterator<String> i = getContext().aliasNames();
        while (i.hasNext())
        {
            String alias = i.next();
            InvarType type = getContext().aliasGet(alias);
            impsCheckAdd(imps, type);

            String key = null;
            if (TypeID.LIST == type.getRealId())
                key = Key.RUNTIME_ALIAS_VEC;
            else if (TypeID.MAP == type.getRealId())
                key = Key.RUNTIME_ALIAS_MAP;
            else
                key = Key.RUNTIME_ALIAS_BASIC;

            String s = snippetGet(key);
            s = replace(s, tokenName, alias);
            s = replace(s, tokenType, type.getName());

            if (type instanceof TypeStruct)
                meStruct.append(s);
            else if (type instanceof TypeEnum)
                meEnums.append(s);
            else
                meBasic.append(s);
        }
        StringBuilder body = new StringBuilder();
        body.append(makeRuntimeAliasFunc("aliasBasic", meBasic.toString()));
        body.append(makeRuntimeAliasFunc("aliasEnum", meEnums.toString()));
        body.append(makeRuntimeAliasFunc("aliasStruct", meStruct.toString()));
        return body.toString();
    }

    private String makeRuntimeAliasFunc (String name, String block)
    {
        String s = snippetGet(Key.RUNTIME_ALIAS);
        s = replace(s, tokenName, name);
        s = replace(s, tokenBlock, block);
        return s;
    }

    protected void impsCheckAdd (TreeSet<String> imps, InvarType t)
    {
        if (getContext().findTypes(t.getName()).size() > 1)
            return;
        String s = snippetGet(Key.IMPORT);
        s = replace(s, tokenName, t.getName());
        s = replace(s, tokenPack, t.getPack().getName());
        //if (t.getName().endsWith(" "))
        //log("Gocha");
        imps.add(s);
    }

    private void buildSnippetMap (InvarContext c)
    {
        c.ghostClear();
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
                //log("Invalid xml node: " + formatXmlNode(n));
                continue;
            }
        }
    }

    private void buildTemplates (Node node)
    {
        String key = getAttrOptional(node, "key");
        NodeList nodes = node.getChildNodes();
        int len = nodes.getLength();
        for (int i = 0; i < len; i++)
        {
            Node n = nodes.item(i);
            if (Node.CDATA_SECTION_NODE != n.getNodeType())
                continue;
            snippetAdd(key, n.getTextContent());
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
            TypeID id = c.findBuildInType(typeName).getId();
            String type = getAttrOptional(n, "type");
            String pack = getAttrOptional(n, "pack");
            String generic = getAttrOptional(n, "generic");
            String initValue = getAttrOptional(n, "initValue");
            String initSuffix = getAttrOptional(n, "initSuffix");
            String initPrefix = getAttrOptional(n, "initPrefix");
            type = type.trim();
            pack = pack.trim();
            c.typeRedefine(id, pack, type, generic, initValue, initPrefix, initSuffix);
        }
    }

    private void buildExport (Node n)
    {
        String resPath = getAttrOptional(n, "resPath");
        String destDir = getAttrOptional(n, "destDir");
        String destName = getAttrOptional(n, "destName");
        exportFile(resPath, destDir, destName);
    }

    private String getAttrOptional (Node node, String name)
    {
        Node n = node.getAttributes().getNamedItem(name);
        String v = empty;
        if (n != null)
            v = n.getNodeValue();
        return v;
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
            line = line.replaceAll("(\\s*)(" + tokenIndent //
                    + "|" + tokenBlank//
                    + "|" + tokenBlock//
                    + ")(\\s*)", "$2");
            if (!line.equals(empty))
            {
                line = line.replaceAll(tokenBr, br);
                line = line.replaceAll(tokenIndent, indent);
                line = line.replaceAll(tokenBlank, empty);
                code.append(line + (i != len - 1 ? br : empty));
            }
        }
        snippetMap.put(key, code.toString());
        //System.out.println("================================================= " + key);
        //System.out.println(snippetMap.get(key));
    }

    private String snippetGet (String key)
    {
        if (!snippetMap.containsKey(key))
        {
            logErr("Can't find snippet by key: " + key);
            return empty;
        }
        return snippetMap.get(key);
    }

    private Document getSnippetDoc (String langName, InvarContext ctx) throws Exception
    {
        String path = "/res/" + langName + "/snippet.xml";
        InputStream res = getClass().getResourceAsStream(path);
        if (res != null)
        {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(res);
            if (!doc.hasChildNodes())
                return null;
            log("Read <- " + path);
            return doc;
        }
        else
        {
            throw new Exception("File doesn't exist: " + path);
        }
    }

    private String replace (String s, String token, String replacement)
    {
        // RegExp Common Metacharacters: ^[.${*(\+)|?<> 
        return s.replaceAll(token, Matcher.quoteReplacement(replacement));
    }

}
