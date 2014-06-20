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
    private Integer                      methodIndentNum = 1;
    private Boolean                      packNameNested  = false;

    final private TreeSet<String>        fileIncludes;
    final private Document               snippetDoc;
    final private HashMap<String,String> snippetMap;
    final private String                 snippetPath;
    final private StreamCoder            codeStreamRead;
    final private StreamCoder            codeStreamWrite;
    final private ToXmlCoder             codeToXml;

    public InvarWriteCode(InvarContext ctx, String langName, String dirRootPath) throws Exception
    {
        super(ctx, dirRootPath);
        this.snippetPath = "/res/" + langName + "/snippet.xml";
        this.snippetDoc = getSnippetDoc(snippetPath, ctx);
        this.snippetMap = new LinkedHashMap<String,String>();
        this.codeStreamRead = new StreamCoder(Key.PREFIX_READ);
        this.codeStreamWrite = new StreamCoder(Key.PREFIX_WRITE);
        this.codeToXml = new ToXmlCoder();
        this.fileIncludes = new TreeSet<String>();
    }

    public InvarWriteCode(InvarContext ctx, String langName, String dirRootPath, String snippetName) throws Exception
    {
        super(ctx, dirRootPath);
        this.snippetPath = "/res/" + langName + "/" + snippetName;
        this.snippetDoc = getSnippetDoc(snippetPath, ctx);
        this.snippetMap = new LinkedHashMap<String,String>();
        this.codeStreamRead = new StreamCoder(Key.PREFIX_READ);
        this.codeStreamWrite = new StreamCoder(Key.PREFIX_WRITE);
        this.codeToXml = new ToXmlCoder();
        this.fileIncludes = new TreeSet<String>();
    }

    @Override
    protected Boolean beforeWrite (InvarContext c)
    {
        buildSnippetMap(c);

        methodIndentNum = 1;
        if (!snippetTryGet(Key.METHOD_INDENT_NUM).equals(empty))
            methodIndentNum = Integer.parseUnsignedInt(snippetTryGet(Key.METHOD_INDENT_NUM));

        packNameNested = false;
        if (!snippetTryGet(Key.PACK_NAME_NESTED).equals(empty))
            packNameNested = Boolean.parseBoolean(snippetTryGet(Key.PACK_NAME_NESTED));

        Boolean capitalize = Boolean.parseBoolean(snippetTryGet(Key.PACK_CAPITALIZE));
        super.packNameReset(c, capitalize);
        super.setDirPrefix(snippetTryGet(Key.CODE_DIR_PREFIX));
        super.setLowerFileName(Boolean.parseBoolean(snippetTryGet(Key.FILE_NAME_LOWER)));
        super.setOnePackOneFile(Boolean.parseBoolean(snippetTryGet(Key.ONE_PACK_ONE_FILE)));
        super.setFlattenDir(Boolean.parseBoolean(snippetTryGet(Key.CODE_DIR_FLATTEN)));
        super.setTraceAllTypes(Boolean.parseBoolean(snippetTryGet("trace.all.types")));
        return true;
    }

    @Override
    protected String codeOneFile (String packName, String filePath, List<TypeEnum> enums, List<TypeStruct> structs)
    {
        String ifndef = filePath;
        ifndef = ifndef.toUpperCase();
        ifndef = replace(ifndef, "\"", empty);
        ifndef = replace(ifndef, "//", "_");
        ifndef = replace(ifndef, "/", "_");
        ifndef = replace(ifndef, tokenDot, "_");

        fileIncludes.clear();
        TreeSet<String> imps = new TreeSet<String>();
        String body = codeOneFileBody(enums, structs, imps);

        if (enums.size() > 0)
        {
            String codePath = getContext().findBuildInType(TypeID.INT32).getRedirect().getCodePath();
            fileIncludes.add(codePath);
        }
        StringBuilder includes = new StringBuilder();
        for (String inc : fileIncludes)
        {
            String s = snippetTryGet(Key.FILE_INCLUDE);
            if (s.equals(empty))
                continue;
            if (inc.equals(empty))
                continue;
            //if (inc.equals(filePath))
            //  continue;
            s = replace(s, tokenName, inc);
            includes.append(s);
        }

        List<String> packNames = new LinkedList<String>();
        if (packNameNested)
        {
            String[] names;
            names = packName.split(tokenDot);
            for (String n : names)
                packNames.add(n);
        }
        else
        {
            packNames.add(packName);
        }

        String s = snippetTryGet(Key.FILE, "//Error: No template named '" + Key.FILE + "' in " + snippetPath);
        s = replace(s, tokenDefine, ifndef);
        s = replace(s, tokenIncludes, includes.toString());
        s = replace(s, tokenPack, codeOneFilePack(packNames, body));
        return s;
    }

    String codeOneFileBody (List<TypeEnum> enums, List<TypeStruct> structs, TreeSet<String> imps)
    {
        StringBuilder codeEnums = new StringBuilder();
        StringBuilder codeStructs = new StringBuilder();

        if (!snippetTryGet(Key.ENUM).equals(empty))
        {
            for (TypeEnum type : enums)
            {
                String block = makeEnumBlock(type);
                String s = snippetGet(Key.ENUM);
                s = replace(s, tokenName, type.getName());
                s = replace(s, tokenBody, block);
                s = replace(s, tokenDoc, makeDoc(type.getComment()));
                codeEnums.append(s);
            }
        }

        for (TypeStruct type : structs)
        {
            StringBuilder fields = new StringBuilder();
            StringBuilder setters = new StringBuilder();
            StringBuilder getters = new StringBuilder();
            StringBuilder encoder = new StringBuilder();
            StringBuilder decoder = new StringBuilder();

            String block = makeStructBlock(type, imps, fields, setters, getters, encoder, decoder);
            String s = snippetGet(Key.STRUCT);
            s = replace(s, tokenName, type.getName());
            s = replace(s, tokenBody, block);
            s = replace(s, tokenDoc, makeDoc(type.getComment()));
            s = replace(s, tokenFields, fields.toString());
            s = replace(s, tokenSetters, setters.toString());
            s = replace(s, tokenGetters, getters.toString());
            s = replace(s, tokenDecoder, decoder.toString());
            s = replace(s, tokenEncoder, encoder.toString());
            codeStructs.append(s);
        }

        String s = snippetTryGet(Key.FILE_BODY, "//Error: No template named '" + Key.FILE_BODY + "' in " + snippetPath);
        s = replace(s, tokenImport, makeImorts(imps));
        s = replace(s, tokenEnums, codeEnums.toString());
        s = replace(s, tokenStructs, codeStructs.toString());
        return s;
    }

    String codeOneFilePack (List<String> packNames, String body)
    {
        int i = packNames.size() - 1;
        if (i < 0)
        {
            return body;
        }
        String name = packNames.get(i);
        packNames.remove(i);
        if (name.equals(empty))
        {
            return body;
        }
        String s = snippetTryGet(Key.FILE_PACK, "//Error: No template named '" + Key.FILE_PACK + "' in " + snippetPath);
        s = replace(s, tokenName, name);
        s = replace(s, tokenBody, body);
        return codeOneFilePack(packNames, s);
    }

    @Override
    protected void codeRuntime (String suffix)
    {
        String typeName = snippetTryGet(Key.RUNTIME_NAME);
        if (empty.equals(typeName))
            return;
        String fileDir = snippetGet(Key.RUNTIME_PACK);
        TreeSet<String> imps = new TreeSet<String>();
        String block = makeRuntimeBlock(imps);
        String s = snippetGet(Key.STRUCT);
        s = replace(s, tokenName, typeName);
        s = replace(s, tokenBody, block);
        s = replace(s, tokenDoc, empty);
        addExportFile(fileDir, typeName + suffix, makePack(fileDir, Key.RUNTIME_NAME, s, imps));
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

    protected String makePack (String packName, String typeName, String blockCode, TreeSet<String> imps)
    {
        String m = packName + "_" + typeName;
        m = m.toUpperCase();
        m = replace(m, "\\.", "_");

        String s = snippetGet(Key.PACK);
        s = replace(s, tokenDefine, m);
        s = replace(s, tokenName, packName);
        s = replace(s, tokenBody, blockCode);
        s = replace(s, tokenImport, makeImorts(imps));
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
            s = replaceFirst(s, tokenName, fixedLen(lenKey, key));
            s = replace(s, tokenName, key);
            s = replace(s, tokenType, type.getName());
            s = replace(s, tokenValue, fixedLenBackward(whiteSpace, lenVal, type.getValue(key).toString()));
            s = replace(s, tokenDoc, makeDoc(type.getComment(key)));
            code.append(s);
        }
        return code.toString();
    }

    private String makeStructBlock (TypeStruct type,
                                    TreeSet<String> imps,
                                    StringBuilder fields,
                                    StringBuilder setters,
                                    StringBuilder getters,
                                    StringBuilder encoder,
                                    StringBuilder decoder)
    {
        List<InvarField> fs = type.listFields();

        if (type.getSuperType() != null)
            impsCheckAdd(imps, type.getSuperType());
        int widthType = 1;
        int widthName = 1;
        int widthDeft = 1;
        for (InvarField f : fs)
        {
            f.makeTypeFormatted(getContext(), snippetGet(Key.IMPORT_SPLIT));
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

        for (InvarField f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthName);
            f.setWidthDefault(widthDeft);
            fields.append(makeStructField(f, type));
            setters.append(makeStructSetter(f, type));
            getters.append(makeStructGetter(f, type));
            //reads.addAll(codeStreamRead.makeStructStreamBody(f, Key.PREFIX_READ));
        }
        encoder.append(codeStreamWrite.code(type, fs, imps));
        decoder.append(codeStreamRead.code(type, fs, imps));
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(setters);
        body.append(getters);
        body.append(decoder);
        body.append(encoder);
        body.append(codeToXml.code(fs, imps));
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
        if (TypeID.VEC == f.getType().getId())
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

    private StringBuilder makeStructGetter (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_GETTER);
        s = replace(s, tokenTypeHost, struct.getName());
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
        case VEC:
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
        case STRING:
            return type.getInitValue();
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
            if (TypeID.VEC == type.getRealId())
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

    protected String makeImorts (TreeSet<String> imps)
    {
        if (imps == null || imps.size() == 0)
        {
            return empty;
        }
        else
        {
            TreeSet<String> lines = new TreeSet<String>();

            for (String key : imps)
            {
                if (key.equals(empty))
                    continue;
                String[] names = key.split(typeSplit);
                if (names.length <= 0)
                    continue;
                String body = snippetGet(Key.IMPORT_BODY);
                if (names.length < 1)
                {
                    continue;
                }
                else if (names.length == 1)
                {
                    body = replace(body, tokenPack, empty);
                    body = replace(body, tokenName, names[0]);
                }
                else
                {
                    String split = snippetGet(Key.IMPORT_SPLIT);
                    String pName = names[0];
                    pName = replace(pName, tokenDot, split);
                    body = replace(body, tokenPack, pName);
                    if (pName.equals(empty))
                        split = empty;
                    body = replace(body, tokenName, split + names[1]);
                }
                if (body.equals(empty))
                    continue;
                String s = snippetGet(Key.IMPORT);
                s = replace(s, tokenBody, body);
                lines.add(s);
            }
            StringBuilder code = new StringBuilder();
            for (String line : lines)
            {
                code.append(line);
            }
            return code.toString();
        }
    }

    protected void impsCheckAdd (TreeSet<String> imps, InvarType t)
    {
        if (getContext().findTypes(t.getName()).size() > 1)
            return;
        String packName = t.getPack().getName();
        String typeName = t.getName();
        imps.add(packName + typeSplit + typeName);

        String include = t.getCodePath();
        if (include != null && !include.equals(empty))
        {
            if (super.getLowerFileName())
                include = include.toLowerCase();
            fileIncludes.add(include);
            //System.out.println("InvarWriteCode.impsCheckAdd()  " + include);
        }
    }

    protected void impsCheckAdd (TreeSet<String> imps, String ss)
    {
        if (ss.equals(empty))
            return;
        String[] lines = ss.split((","));
        for (String line : lines)
        {
            if (line.equals(empty))
                continue;
            imps.add(line);
        }
    }

    private void buildSnippetMap (InvarContext c)
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
            String type = getAttrOptional(n, "type");
            String pack = getAttrOptional(n, "pack");
            String generic = getAttrOptional(n, "generic");
            String initValue = getAttrOptional(n, "initValue");
            String initSuffix = getAttrOptional(n, "initSuffix");
            String initPrefix = getAttrOptional(n, "initPrefix");
            String include = getAttrOptional(n, "include");
            type = type.trim();
            pack = pack.trim();
            InvarType buildInT = c.findBuildInType(typeName);
            if (buildInT == null)
            {
                getContext().ghostAdd(pack, type, generic, TypeID.GHOST);
                continue;
            }
            TypeID id = buildInT.getId();
            c.typeRedefine(id, pack, type, generic, initValue, initPrefix, initSuffix, include);
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
                    //+ "|" + tokenBody//
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

    private String snippetTryGet (String key)
    {
        if (!snippetMap.containsKey(key))
        {
            return empty;
        }
        return snippetMap.get(key);
    }

    private String snippetTryGet (String key, String deft)
    {
        if (!snippetMap.containsKey(key))
        {
            return deft;
        }
        return snippetMap.get(key);
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
            log("read  <- " + path);
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

    private String replaceFirst (String s, String token, String replacement)
    {
        // RegExp Common Metacharacters: ^[.${*(\+)|?<> 
        return s.replaceFirst(token, Matcher.quoteReplacement(replacement));
    }

    private String makeCodeMethod (List<String> lines, String returnType, String snippetKey)
    {
        indentLines(lines, methodIndentNum);
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

    private class ToXmlCoder
    {
        private String prefix = "toxml.";

        public Object code (List<InvarField> fs, TreeSet<String> imps)
        {
            String s = snippetTryGet(prefix + Key.IMPORT);
            if (empty.equals(s))
                return empty;
            impsCheckAdd(imps, s);
            List<String> reads = new ArrayList<String>();
            //TODO for (InvarField f : fs)
            //reads.addAll(makeField(f));
            String rt = getContext().findBuildInType(TypeID.STRING).getRedirect().getName();
            return makeCodeMethod(reads, rt, "toxml.method");
        }

    }

    static private String createShortRule (InvarField f, InvarContext ctx)
    {
        String split = ".";
        InvarType typeBasic = f.getType().getRedirect();
        if (f.getGenerics().size() == 0)
        {
            if (ctx.findTypes(typeBasic.getName()).size() > 1)
                return typeBasic.fullName(split);
            else
                return typeBasic.getName();
        }
        String s = typeBasic.getRealId().getGeneric();
        for (InvarType t : f.getGenerics())
        {
            t = t.getRedirect();
            String forShort = null;
            if (t.getRealId() == TypeID.VEC || t.getRealId() == TypeID.MAP)
                forShort = t.getName();
            else
            {
                if (ctx.findTypes(t.getName()).size() > 1)
                    forShort = t.fullName(split);
                else
                    forShort = t.getName();
            }
            s = s.replaceFirst("\\?", forShort + t.getRealId().getGeneric());
        }
        return typeBasic.getName() + s;

    }

    private class StreamCoder
    {
        private class CodeForParams
        {
            public Boolean needDefine = false;
            public Boolean needCheck  = false;
            public int     numLayer   = 0;
            public String  name       = empty;
            public String  nameOutter = empty;
            public String  init       = empty;
            private String type       = empty;

            public String getType ()
            {
                return type;
            }

            public void setType (String type)
            {

                this.type = type.replace(tokenDot, typeSplit);
            }
        }

        private String prefix   = empty;
        private TypeID sizeType = TypeID.INT32;

        public StreamCoder(String prefix)
        {
            this.prefix = prefix;
        }

        public String code (TypeStruct type, List<InvarField> fs, TreeSet<String> imps)
        {
            String s = snippetTryGet(prefix + Key.IMPORT);
            if (empty.equals(s))
                return empty;
            impsCheckAdd(imps, snippetGet(prefix + Key.IMPORT));
            List<String> lines = new ArrayList<String>();
            for (InvarField f : fs)
                lines.addAll(makeField(f));
            return makeCodeMethod(lines, type.getName(), prefix + "method");
        }

        private CodeForParams makeParams (TypeID typeID,
                                          Boolean needDefine,
                                          Boolean needCheck,
                                          String type,
                                          String name,
                                          String nameOutter,
                                          String indexer)
        {
            CodeForParams params = new CodeForParams();
            params.needDefine = needDefine;
            params.needCheck = needCheck;
            params.setType(type);
            params.name = name;
            params.nameOutter = nameOutter;
            if (Key.PREFIX_READ.equals(prefix))
            {
                if (TypeID.STRUCT == typeID || TypeID.VEC == typeID || TypeID.MAP == typeID)
                    params.init = replace(snippetGet(Key.INIT_STRUCT), tokenType, type);
                else
                    params.init = empty;
            }
            else if (Key.PREFIX_WRITE.equals(prefix))
                params.init = makeCodeIndexer(nameOutter, indexer);
            else
                params.init = empty;
            return params;
        }

        private String makeGeneric (String rule, CodeForParams params, String name, Boolean needDef, String indexer)
        {
            String L = ruleLeft(rule);
            InvarType t = getTypeByShort(L);
            if (t == null)
            {
                logErr("No type named " + L);
                return empty;
            }
            TypeID type = t.getRealId();
            CodeForParams p = makeParams(type, needDef, false, rule, name, params.name, indexer);
            List<String> body = new ArrayList<String>();
            p.numLayer = params.numLayer + 1;
            makeField(type, rule, p, body);
            return buildCodeLines(body).toString();
        }

        private List<String> makeField (InvarField f)
        {
            String rule = createShortRule(f, getContext());
            TypeID type = f.getType().getRealId();
            List<String> lines = new ArrayList<String>();
            CodeForParams params = makeParams(type, false, true, rule, f.getKey(), f.getKey(), empty);
            params.numLayer = 1;
            makeField(type, rule, params, lines);
            return lines;
        }

        private void makeField (TypeID type, String rule, CodeForParams p, List<String> lines)
        {
            String code = null;
            if (TypeID.VEC == type)
                code = makeFieldVec(p, rule);
            else if (TypeID.MAP == type)
                code = makeFieldMap(p, rule);
            else
                code = makeFieldSimple(p, type);
            lines.addAll(indentLines(code));
        }

        private String makeFieldSimple (CodeForParams p, TypeID type)
        {
            //non-collection fields
            String s = empty;
            if (p.needDefine)
            {
                if (!p.init.equals(empty))
                {
                    s = makeCodeAssignment(p.getType(), p.name, p.init);
                }
                else
                {
                    s = snippetGet(Key.CODE_DEFINITION);
                }
            }

            if (TypeID.STRUCT == type && p.needCheck)
            {
                s += snippetGet(prefix + type.getName() + ".check");
            }
            else
            {
                s += snippetGet(prefix + type.getName());
            }
            s = replace(s, tokenName, p.name);
            s = replace(s, tokenType, p.getType());
            return s;
        }

        private String makeFieldVec (CodeForParams p, String rule)
        {
            String R = ruleRight(rule);
            String indexer = "i" + upperHeadChar(p.name);
            String nameItem = "v" + p.numLayer;
            String head = empty;
            if (p.needDefine)
                head += makeCodeAssignment(p.getType(), p.name, p.init);
            String body = makeGeneric(R, p, nameItem, true, indexer);
            return head + makeCodeFor(TypeID.VEC, body, p, nameItem, empty, empty);
        }

        private String makeFieldMap (CodeForParams p, String rule)
        {
            String r = ruleRight(rule);
            String[] R = r.split(",");
            String ruleK = R[0];
            String ruleV = R[1];
            String indexer = "i" + upperHeadChar(p.name);
            String nameKey = "k" + p.numLayer;
            String nameVal = "v" + p.numLayer;
            String body = empty;
            String snippet = snippetGet(prefix + "map.keys").trim();
            if (snippet.equals(empty))
            {
                body += makeGeneric(ruleK, p, nameKey, true, indexer);
                body += makeGeneric(ruleV, p, nameVal, true, nameKey);
                String head = empty;
                if (p.needDefine)
                    head += makeCodeAssignment(p.getType(), p.name, p.init);
                return head + makeCodeFor(TypeID.MAP, body, p, nameVal, nameKey, empty);
            }
            else
            {
                body += makeGeneric(ruleK, p, nameKey, false, indexer);
                body += makeGeneric(ruleV, p, nameVal, true, nameKey);
                p.nameOutter = p.name;
                String nameLen = "len" + upperHeadChar(p.nameOutter);
                String head = empty;
                if (p.needDefine)
                    head += makeCodeAssignment(p.getType(), p.nameOutter, p.init);
                p.name = nameKey;
                p.setType(ruleK);
                return head + makeCodeFor(TypeID.MAP, body, p, nameVal, nameKey, nameLen);
            }
        }

        private String makeCodeFor (TypeID type,
                                    String body,
                                    CodeForParams params,
                                    String nameItem,
                                    String nameKey,
                                    String nameLen)
        {
            String iterNameUp = upperHeadChar(params.name);
            String index = "i" + iterNameUp;

            String sizeName = null;
            if (nameLen.equals(empty))
                sizeName = "len" + iterNameUp;
            else
                sizeName = nameLen;

            String sizeType = getContext().findBuildInType(this.sizeType).getRedirect().getName();

            String s = snippetGet(prefix + type.getName() + ".for");
            s = replace(s, tokenBody, body);
            //
            s = replace(s, tokenType, params.getType());
            s = replace(s, tokenName, params.name);
            s = replace(s, tokenNameUpper, params.nameOutter);
            //
            s = replace(s, tokenTypeSize, sizeType);
            s = replace(s, tokenLen, sizeName);
            s = replace(s, tokenIndex, index);
            //
            s = replace(s, tokenKey, nameKey);
            s = replace(s, tokenValue, nameItem);
            return s;
        }
    }

}
