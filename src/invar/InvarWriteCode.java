package invar;

import invar.model.InvarField;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
        s = replace(s, tokenBody, block);
        s = replace(s, tokenDoc, makeDoc(type.getComment()));
        return makePack(type.getPack().getName(), s, null);
    }

    @Override
    protected String codeStruct (TypeStruct type)
    {
        TreeSet<String> imps = new TreeSet<String>();
        String block = makeStructBlock(type, imps);
        String s = snippetGet(Key.STRUCT);
        s = replace(s, tokenName, type.getName());
        s = replace(s, tokenBody, block);
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
        s = replace(s, tokenBody, block);
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
    final static protected String tokenBody      = tokenPrefix + "body" + tokenSuffix;
    final static protected String tokenImport    = tokenPrefix + "import" + tokenSuffix;
    final static protected String tokenPack      = tokenPrefix + "pack" + tokenSuffix;
    final static protected String tokenType      = tokenPrefix + "type" + tokenSuffix;
    final static protected String tokenTypeHost  = tokenPrefix + "typehost" + tokenSuffix;
    final static protected String tokenName      = tokenPrefix + "name" + tokenSuffix;
    final static protected String tokenNameUpper = tokenPrefix + "nameupper" + tokenSuffix;
    final static protected String tokenIndex     = tokenPrefix + "index" + tokenSuffix;
    final static protected String tokenLen       = tokenPrefix + "len" + tokenSuffix;

    final protected class Key
    {
        final static public String DOC                 = "doc";
        final static public String DOC_LINE            = "doc.line";
        final static public String IMPORT              = "import";
        final static public String INIT_STRUCT         = "init.struct";
        final static public String INIT_ENUM           = "init.enum";
        final static public String CODE_ASSIGNMENT     = "code.assignment";
        final static public String CODE_INDEXER        = "code.indexer";
        final static public String CODE_FOREACH        = "code.foreach";
        final static public String CODE_FORI           = "code.fori";
        final static public String CODE_FORI_ITYPE     = "code.fori.itype";

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
    final private StreamReadCoder        codeStreamRead;
    final private StreamWriteCoder       codeStreamWrite;

    public InvarWriteCode(InvarContext ctx, String langName, String dirRootPath) throws Exception
    {
        super(ctx, dirRootPath);
        this.snippetDoc = getSnippetDoc(langName, ctx);
        this.snippetMap = new LinkedHashMap<String,String>();
        this.codeStreamRead = new StreamReadCoder();
        this.codeStreamWrite = new StreamWriteCoder();
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

    private String makeCodeAssignment (String type, String name, String value)
    {
        String s = snippetGet(Key.CODE_ASSIGNMENT);
        s = replace(s, tokenValue, value);
        s = replace(s, tokenType, type != empty ? type + whiteSpace : empty);
        s = replace(s, tokenName, name);
        return s;
    }

    private String makeCodeIndexer (String name, String index)
    {
        String s = snippetGet(Key.CODE_INDEXER);
        s = replace(s, tokenName, name);
        s = replace(s, tokenIndex, index);
        return s;
    }

    protected String makePack (String packName, String blockCode, TreeSet<String> imps)
    {
        String s = snippetGet(Key.PACK);
        s = replace(s, tokenName, packName);
        s = replace(s, tokenBody, blockCode);
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
                code.append(key + br);
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
            //reads.addAll(codeStreamRead.makeStructStreamBody(f, Key.PREFIX_READ));
        }
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(setters);
        body.append(getters);
        body.append(br);
        body.append(codeStreamRead.code(type, fs, imps));
        body.append(br);
        body.append(codeStreamWrite.code(type, fs, imps));
        return body.toString();
    }

    private StringBuilder buildCodeLines (List<String> lines)
    {
        StringBuilder codes = new StringBuilder();
        for (String line : lines)
        {
            codes.append(br + line);
        }
        return codes;
    }

    private StringBuilder makeStructMeta (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_META);
        s = replace(s, tokenType, f.createAliasRule(getContext(), "."));
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
        s = replace(s, tokenBody, block);
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
        s = replace(s, tokenBody, block);
        return s;
    }

    protected void impsCheckAdd (TreeSet<String> imps, InvarType t)
    {
        if (getContext().findTypes(t.getName()).size() > 1)
            return;
        String s = snippetGet(Key.IMPORT);
        s = replace(s, tokenName, t.getName());
        s = replace(s, tokenPack, t.getPack().getName());
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
                    + "|" + tokenBody//
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

    private List<String> indentLines (String snippet)
    {
        int numIndent = 1;
        String[] lines = snippet.split("\n|\r\n");
        String strIndent = empty;
        for (int i = 0; i < numIndent; i++)
        {
            strIndent += indent;
        }
        List<String> codes = new ArrayList<String>();
        for (String line : lines)
        {
            codes.add(strIndent + line);
        }
        return codes;
    }

    private void indentLines (List<String> lines, int numIndent)
    {
        String strIndent = empty;
        for (int i = 0; i < numIndent; i++)
        {
            strIndent += indent;
        }
        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, strIndent + lines.get(i));
        }
    }

    private String replace (String s, String token, String replacement)
    {
        // RegExp Common Metacharacters: ^[.${*(\+)|?<> 
        return s.replaceAll(token, Matcher.quoteReplacement(replacement));
    }

    private String makeCodeMethod (List<String> lines, String returnType, String snippetKey)
    {
        indentLines(lines, 1);
        StringBuilder body = new StringBuilder();
        for (String line : lines)
        {
            body.append(br + line);
        }
        String b = body.toString();
        String s = snippetGet(snippetKey);
        s = replace(s, tokenType, returnType);
        s = replace(s, tokenBody, b);
        return s;
    }

    final private class CodeForParams
    {
        public Boolean needDefine = false;
        public String  name       = empty;
        public String  nameOutter = empty;
        public String  type       = empty;
        public String  init       = empty;
        public String  sizeInit   = empty;
    }

    final private class StreamReadCoder
    {
        private String prefix = "read.";

        public String code (TypeStruct type, List<InvarField> fs, TreeSet<String> imps)
        {
            imps.add(snippetGet(prefix + "imports"));
            List<String> reads = new ArrayList<String>();
            for (InvarField f : fs)
            {
                reads.addAll(makeField(f));
            }
            return makeCodeMethod(reads, type.getName(), prefix + "method");
        }

        private CodeForParams makeParams (Boolean needDefine, String type, String name)
        {
            CodeForParams params = new CodeForParams();
            String s = snippetGet(Key.INIT_STRUCT);
            params.name = name;
            params.type = type;
            params.init = replace(s, tokenType, type);
            //TODO
            params.sizeInit = "stream.ReadInt32()";
            params.needDefine = needDefine;
            return params;
        }

        private List<String> makeField (InvarField f)
        {
            String rule = f.createShortRule(getContext());
            List<String> lines = new ArrayList<String>();
            CodeForParams params = makeParams(false, rule, f.getKey());
            makeField(rule, 1, params, lines);
            return lines;
        }

        private String makeGeneric (String rule, int numLayer, CodeForParams params, String name)
        {
            CodeForParams p = makeParams(true, rule, name);
            List<String> body = new ArrayList<String>();
            makeField(rule, numLayer + 1, p, body);
            return buildCodeLines(body).toString();
        }

        private void makeField (String rule, int numLayer, CodeForParams p, List<String> lines)
        {
            String L = ruleLeft(rule);
            InvarType t = getTypeByShort(L);
            if (t == null)
            {
                logErr("No type named " + L);
                return;
            }
            TypeID type = t.getRealId();
            String code = empty;
            String s = snippetGet(prefix + type.getName());
            if (TypeID.LIST == type)
            {
                String R = ruleRight(rule);
                String nameItem = "item" + numLayer;
                String body = makeGeneric(R, numLayer, p, nameItem);
                s = replace(s, tokenName, p.name);
                s = replace(s, tokenValue, nameItem);
                body = body + br + indent + s;
                code = makeCodeFori(body, numLayer, p);
            }
            else if (TypeID.MAP == type)
            {
                String r = ruleRight(rule);
                String[] R = r.split(",");
                String ruleK = R[0];
                String ruleV = R[1];
                String nameKey = "key" + numLayer;
                String nameVal = "value" + numLayer;
                String body = empty;
                body += makeGeneric(ruleK, numLayer, p, nameKey);
                body += makeGeneric(ruleV, numLayer, p, nameVal);
                s = replace(s, tokenNameUpper, p.name);
                s = replace(s, tokenName, nameKey);
                s = replace(s, tokenValue, nameVal);
                body = body + br + indent + s;
                code = makeCodeFori(body, numLayer, p);
            }
            else
            {
                if (TypeID.ENUM == type)
                {
                    s = replace(s, tokenName, p.name);
                    s = replace(s, tokenType, p.type);
                    code = makeCodeAssignment(p.needDefine ? p.type : empty, p.name, s);
                }
                else if (TypeID.STRUCT == type)
                {
                    if (!p.needDefine)
                        s = snippetGet(prefix + "struct.check") + s;
                    else
                    {
                        s = makeCodeAssignment(p.type, p.name, p.init) + br + s;
                    }
                    s = replace(s, tokenName, p.name);
                    s = replace(s, tokenType, p.type);
                    code = s;
                }
                else
                {
                    code = makeCodeAssignment(p.needDefine ? p.type : empty, p.name, s);
                }
            }
            lines.addAll(indentLines(code));
        }

        private String makeCodeFori (String body, int numIndent, CodeForParams params)
        {
            String iterNameUp = upperHeadChar(params.name);
            String iterName = params.name;
            String index = "idx" + iterNameUp;
            String sizeName = "len" + iterNameUp;
            String sizeType = getContext().findBuildInType(snippetGet(Key.CODE_FORI_ITYPE)).getRedirect().getName();
            String head = empty;
            if (params.needDefine)
                head += makeCodeAssignment(params.type, iterName, params.init) + br;
            head += makeCodeAssignment(sizeType, sizeName, params.sizeInit) + br;
            String s = snippetGet(Key.CODE_FORI);
            s = head + s;
            s = replace(s, tokenType, sizeType);
            s = replace(s, tokenLen, sizeName);
            s = replace(s, tokenIndex, index);
            s = replace(s, tokenBody, body);
            return s;
        }
    }

    private class StreamWriteCoder
    {
        private final String prefix = "write.";
        private InvarType    sizeType;

        public String code (TypeStruct type, List<InvarField> fs, TreeSet<String> imps)
        {
            imps.add(snippetGet(prefix + "imports"));
            sizeType = getContext().findBuildInType(snippetGet(Key.CODE_FORI_ITYPE)).getRedirect();
            List<String> reads = new ArrayList<String>();
            for (InvarField f : fs)
            {
                reads.addAll(makeField(f));
            }
            return makeCodeMethod(reads, type.getName(), prefix + "method");
        }

        private CodeForParams makeParams (Boolean needDefine,
                                          String type,
                                          String name,
                                          String nameOutter,
                                          String indexer)
        {
            CodeForParams params = new CodeForParams();
            params.needDefine = needDefine;
            params.type = type;
            params.name = name;
            params.nameOutter = nameOutter;
            //TODO
            params.sizeInit = name + ".Count";
            params.init = makeCodeIndexer(nameOutter, indexer);
            return params;
        }

        private List<String> makeField (InvarField f)
        {
            String rule = f.createShortRule(getContext());
            List<String> lines = new ArrayList<String>();
            CodeForParams params = makeParams(false, rule, f.getKey(), f.getKey(), empty);
            makeField(rule, 1, params, lines);
            return lines;
        }

        private String makeGeneric (String rule,
                                    int numLayer,
                                    CodeForParams params,
                                    String name,
                                    Boolean needDef,
                                    String indexer)
        {
            CodeForParams p = makeParams(needDef, rule, name, params.name, indexer);
            List<String> body = new ArrayList<String>();
            makeField(rule, numLayer + 1, p, body);
            return buildCodeLines(body).toString();
        }

        private void makeField (String rule, int numLayer, CodeForParams p, List<String> lines)
        {
            String L = ruleLeft(rule);
            InvarType t = getTypeByShort(L);
            if (t == null)
            {
                logErr("No type named " + L);
                return;
            }
            TypeID type = t.getRealId();
            String code = empty;
            String indexer = "idx" + upperHeadChar(p.name);
            if (TypeID.LIST == type)
            {
                String R = ruleRight(rule);
                String nameItem = "item" + numLayer;
                String body = makeGeneric(R, numLayer, p, nameItem, true, indexer);
                code = makeCodeFori(body, p);
            }
            else if (TypeID.MAP == type)
            {
                String r = ruleRight(rule);
                String[] R = r.split(",");
                String ruleK = R[0];
                String ruleV = R[1];
                String nameKey = "key" + numLayer;
                String nameVal = "value" + numLayer;
                String body = empty;
                body += makeGeneric(ruleK, numLayer, p, nameKey, false, indexer);
                body += makeGeneric(ruleV, numLayer, p, nameVal, true, nameKey);
                String head = makeCodeForHead(p);
                CodeForParams p2 = makeParams(false, rule, nameKey, p.name, indexer);
                p2.type = ruleK;
                code = head + makeCodeForeach(body, p2);
            }
            else
            {
                String s = snippetGet(prefix + type.getName());
                s = replace(s, tokenName, p.name);
                s = replace(s, tokenType, p.type);
                if (p.needDefine)
                {
                    s = makeCodeAssignment(p.type, p.name, p.init) + br + s;
                }
                code = s;
            }
            lines.addAll(indentLines(code));
        }

        private String makeCodeForHead (CodeForParams params)
        {
            String iterNameUp = upperHeadChar(params.name);
            String iterName = params.name;
            String sizeName = "len" + iterNameUp;
            String sizeType = this.sizeType.getName();
            String head = empty;
            if (params.needDefine)
                head += makeCodeAssignment(params.type, iterName, params.init) + br;
            head += makeCodeAssignment(sizeType, sizeName, params.sizeInit) + br;
            //TODO
            head += "stream.Write(" + sizeName + ");" + br;
            return head;
        }

        private String makeCodeForeach (String body, CodeForParams params)
        {
            String s = snippetGet(prefix + "map.foreach");
            s = replace(s, tokenType, params.type);
            s = replace(s, tokenName, params.name);
            s = replace(s, tokenNameUpper, params.nameOutter);
            s = replace(s, tokenBody, body);
            return s;
        }

        private String makeCodeFori (String body, CodeForParams params)
        {
            String iterNameUp = upperHeadChar(params.name);
            String index = "idx" + iterNameUp;
            String sizeName = "len" + iterNameUp;
            String sizeType = this.sizeType.getName();
            String head = makeCodeForHead(params);
            String s = snippetGet(Key.CODE_FORI);
            s = head + s;
            s = replace(s, tokenType, sizeType);
            s = replace(s, tokenLen, sizeName);
            s = replace(s, tokenIndex, index);
            s = replace(s, tokenBody, body);
            return s;
        }
    }

}
