package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InvarWriteCode extends InvarWrite
{

    @Override
    protected Boolean beforeWrite (InvarContext c)
    {
        buildSnippetMap(c);
        log(dumpTypeAll());
        return true;
    }

    @Override
    protected String codeEnum (TypeEnum type)
    {
        String block = makeEnumBlock(type);
        String s = snippetGet(Key.ENUM);
        s = s.replaceAll(tokenName, type.getName());
        s = s.replaceFirst(tokenBlock, block);
        return makePack(type.getPack(), s);
    }

    @Override
    protected String codeStruct (TypeStruct type)
    {
        TreeSet<String> imps = new TreeSet<String>();
        String block = makeStructBlock(type, imps);
        String s = snippetGet(Key.STRUCT);
        s = s.replaceAll(tokenName, type.getName());
        s = s.replaceFirst(tokenBlock, block);
        return makePack(type.getPack(), s, imps);
    }

    @Override
    protected String codeRuntime (InvarType type)
    {
        // TODO Auto-generated method stub
        return null;
    }

    final static protected String empty          = "";
    final static protected String whiteSpace     = " ";
    final static protected String br             = "\n";
    final static protected String indent         = whiteSpace + whiteSpace + whiteSpace + whiteSpace;

    // RegExp Common Metacharacters: ^[.${*(\+)|?<> 
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
        final static public String DOC           = "doc";
        final static public String DOC_LINE      = "doc.line";
        final static public String IMPORT        = "import";
        final static public String INIT_STRUCT   = "init.struct";
        final static public String INIT_ENUM     = "init.enum";

        final static public String PACK          = "pack";
        final static public String ENUM          = "enum";
        final static public String ENUM_FIELD    = "enum.field";
        final static public String STRUCT        = "struct";
        final static public String STRUCT_META   = "struct.meta";
        final static public String STRUCT_FIELD  = "struct.field";
        final static public String STRUCT_GETTER = "struct.getter";
        final static public String STRUCT_SETTER = "struct.setter";
    }

    final private Document               snippetDoc;
    final private HashMap<String,String> snippetMap;

    public InvarWriteCode(InvarContext ctx, Document templateDoc, String dirRootPath)
    {
        super(ctx, dirRootPath);
        this.snippetDoc = templateDoc;
        this.snippetMap = new LinkedHashMap<String,String>();
    }

    protected void snippetAdd (String key, String s)
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

    protected String snippetGet (String key)
    {
        if (!snippetMap.containsKey(key))
        {
            log("Can't find snippet by key: " + key);
            return empty;
        }
        return snippetMap.get(key);
    }

    protected String makeDocLine (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC_LINE);
        s = s.replaceAll(tokenDoc, comment);
        return s;
    }

    protected String makeDoc (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC);
        s = s.replaceAll(tokenDoc, comment);
        return s;
    }

    protected String makePack (InvarPackage pack, String blockCode)
    {
        return makePack(pack, blockCode, null);
    }

    protected String makePack (InvarPackage pack, String blockCode, TreeSet<String> imps)
    {
        String s = snippetGet(Key.PACK);
        s = s.replaceAll(tokenName, pack.getName());
        s = s.replaceFirst(tokenBlock, blockCode);
        if (imps == null || imps.size() == 0)
        {
            s = s.replaceFirst(tokenImport, empty);
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
            s = s.replaceFirst(tokenImport, br + code.toString());
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
            s = s.replaceAll(tokenDoc, makeDoc(fixedLen(lenDoc, type.getComment(key))));
            s = s.replaceFirst(tokenName, fixedLen(lenKey, key));
            s = s.replaceAll(tokenName, key);
            s = s.replaceAll(tokenValue, fixedLenBackward(whiteSpace, lenVal, type.getValue(key).toString()));
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

    protected StringBuilder makeStructField (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_FIELD);
        s = s.replaceAll(tokenName, fixedLen(f.getWidthKey(), f.getKey()));
        s = s.replaceAll(tokenType, fixedLen(f.getWidthType(), f.getTypeFormatted()));
        s = s.replaceAll(tokenValue, fixedLen(f.getWidthDefault(), f.getDeftFormatted()));
        s = s.replaceAll(tokenDoc, makeDocLine(f.getComment()));
        code.append(s);
        return code;
    }

    private StringBuilder makeStructMeta (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_META);
        s = s.replaceAll(tokenType, f.evalGenerics(getContext(), "."));
        s = s.replaceAll(tokenName, f.getShortName());
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
        s = s.replaceAll(tokenTypeHost, struct.getName());
        s = s.replaceAll(tokenDoc, makeDocLine(f.getComment()));
        s = s.replaceAll(tokenMeta, makeStructMeta(f).toString());
        s = s.replaceAll(tokenType, f.getTypeFormatted());
        s = s.replaceAll(tokenName, f.getKey());
        s = s.replaceAll(tokenNameUpper, upperHeadChar(f.getKey()));
        code.append(s);
        return code;
    }

    private StringBuilder makeStructGetter (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_GETTER);
        s = s.replaceAll(tokenDoc, makeDocLine(f.getComment()));
        s = s.replaceAll(tokenMeta, makeStructMeta(f).toString());
        s = s.replaceAll(tokenType, f.getTypeFormatted());
        s = s.replaceAll(tokenName, f.getKey());
        s = s.replaceAll(tokenNameUpper, upperHeadChar(f.getKey()));
        code.append(s);
        return code;
    }

    protected String makeStructFieldInit (InvarField f, TypeStruct struct)
    {
        if (f.getType() == struct)
            return "null";
        String deft = f.getDefault();
        if (deft != null && !deft.equals(empty))
            return deft;
        InvarType type = f.getType();
        if (type.getConstruct() != null && !type.getConstruct().equals(empty))
            return type.getConstruct();
        if (TypeID.ENUM == type.getId())
        {
            TypeEnum tEnum = (TypeEnum)type;
            String s = snippetGet(Key.INIT_ENUM);
            s = s.replaceAll(tokenType, tEnum.getName());
            s = s.replaceAll(tokenName, tEnum.firstOptionKey());
            return s;
        }
        else
        {
            String t = f.getTypeFormatted();
            String s = snippetGet(Key.INIT_STRUCT);
            s = s.replaceAll(tokenType, t);
            return s;
        }
    }

    protected void impsCheckAdd (TreeSet<String> imps, InvarType t)
    {
        if (getContext().findTypes(t.getName()).size() > 1)
            return;
        String s = snippetGet(Key.IMPORT);
        s = s.replaceAll(tokenName, t.getName());
        s = s.replaceAll(tokenPack, t.getPack().getName());
        if (t.getName().endsWith(" "))
            log("Gocha");
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
            String construct = getAttrOptional(n, "construct");
            type = type.trim();
            pack = pack.trim();
            c.typeRedefine(id, pack, type, generic, construct);
        }
    }

    private String getAttrOptional (Node node, String name)
    {
        Node n = node.getAttributes().getNamedItem(name);
        String v = empty;
        if (n != null)
            v = n.getNodeValue();
        return v;
    }

}
