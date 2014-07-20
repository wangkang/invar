package invar;

import invar.InvarSnippet.Key;
import invar.InvarSnippet.Token;
import invar.model.InvarField;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InvarWriteCode extends InvarWrite
{
    final static String                  empty              = "";
    final static String                  whiteSpace         = " ";
    final static String                  br                 = "\n";
    final static String                  indent             = whiteSpace + whiteSpace + whiteSpace + whiteSpace;
    final static String                  dotToken           = "\\.";

    final private InvarSnippet           snippet;
    final private TreeSet<String>        fileIncludes;
    final private HashMap<String,Method> mapInvoke;
    final private Pattern                patternInvoke;
    final private NestedCoder            nestedCoder;

    private Integer                      methodIndentNum    = 1;
    private Boolean                      packNameNested     = false;
    private Boolean                      useFullName        = false;
    private Boolean                      includeSelf        = false;
    private Boolean                      impExcludeConflict = false;
    private Boolean                      impExcludeSamePack = false;
    private List<String>                 impExcludePacks    = null;

    public InvarWriteCode(InvarContext ctx, String dirRootPath, String snippetPath) throws Exception
    {
        super(ctx, dirRootPath);
        this.snippet = new InvarSnippet(ctx, snippetPath, this);
        this.fileIncludes = new TreeSet<String>();
        this.nestedCoder = new NestedCoder();
        this.patternInvoke = Pattern.compile("\\[#.+\\(.*\\)\\]");
        this.mapInvoke = new HashMap<String,Method>(32);
        mapInvoke.put("fixedLen", InvarWrite.class.getMethod("fixedLen", Integer.class, String.class));
    }

    private String snippetGet (String key)
    {
        String s = snippet.tryGet(key, null);
        if (s == null)
        {
            logErr("Can't find snippet by key: " + key);
            return empty;
        }
        return s;
    }

    private String snippetTryGet (String key)
    {
        return snippet.tryGet(key, empty);
    }

    @Override
    protected Boolean beforeWrite (InvarContext c)
    {
        snippet.buildSnippetMap(c);

        genericOverride = snippet.getGenericOverride();

        methodIndentNum = 1;
        if (!snippetTryGet(Key.METHOD_INDENT_NUM).equals(empty))
            methodIndentNum = Integer.parseInt(snippetTryGet(Key.METHOD_INDENT_NUM));

        packNameNested = Boolean.parseBoolean(snippetTryGet("pack.name.nested"));
        useFullName = Boolean.parseBoolean(snippetTryGet("use.full.type.name"));
        includeSelf = Boolean.parseBoolean(snippetTryGet("include.self"));

        impExcludeConflict = Boolean.parseBoolean(snippetTryGet("import.exclude.conflict"));
        impExcludeSamePack = Boolean.parseBoolean(snippetTryGet("import.exclude.same.pack"));
        impExcludePacks = Arrays.asList(snippetTryGet("import.exclude.packs").trim().split(","));

        super.packNameReset(c, Boolean.parseBoolean(snippetTryGet("capitalize.pack.head")));
        super.setDirPrefix(snippetTryGet("code.dir.prefix"));
        super.setLowerFileName(Boolean.parseBoolean(snippetTryGet("file.name.lowercase")));
        super.setOnePackOneFile(Boolean.parseBoolean(snippetTryGet("one.pack.one.file")));
        super.setFlattenDir(Boolean.parseBoolean(snippetTryGet("code.dir.flatten")));
        super.setTraceAllTypes(Boolean.parseBoolean(snippetTryGet("trace.all.types")));
        return true;
    }

    @Override
    protected String codeOneFile (String packName, String filePath, List<TypeEnum> enums, List<TypeStruct> structs)
    {
        fileIncludes.clear();
        TreeSet<String> imps = new TreeSet<String>();
        String body = codeOneFileBody(enums, structs, imps);
        if (body.equals(empty))
        {
            return empty;
        }
        if (enums.size() > 0)
        {
            String codePath = getContext().findBuildInType(TypeID.INT32).getRedirect().getCodePath();
            fileIncludes.add(codePath);
        }

        StringBuilder includes = new StringBuilder();
        Iterator<String> iter = fileIncludes.descendingIterator();
        while (iter.hasNext())
        {
            String inc = iter.next();
            String s = snippetTryGet(Key.FILE_INCLUDE);
            if (s.equals(empty))
                continue;
            if (inc.equals(empty))
                continue;
            s = replace(s, Token.Name, inc);
            includes.append(s);
        }

        List<String> packNames = new LinkedList<String>();
        if (packNameNested)
        {
            String[] names;
            names = packName.split(dotToken);
            for (String n : names)
                packNames.add(n);
        }
        else
        {
            packNames.add(packName);
        }

        String ifndef = filePath;
        ifndef = ifndef.toUpperCase();
        ifndef = replace(ifndef, "\"", empty);
        ifndef = replace(ifndef, "//", "_");
        ifndef = replace(ifndef, "/", "_");
        ifndef = replace(ifndef, dotToken, "_");
        String s = snippet.tryGet(Key.FILE,
                                  "//Error: No template named '" + Key.FILE + "' in " + snippet.getSnippetPath());

        s = replace(s, Token.Define, ifndef);
        s = replace(s, Token.Includes, includes.toString());
        s = replace(s, Token.Pack, codeOneFilePack(packNames, body));
        return codeEval(s);
    }

    private String codeEval (String s)
    {
        Matcher m = patternInvoke.matcher(s);
        StringBuffer sb = new StringBuffer();
        int mend = 0;
        while (m.find())
        {
            String result = codeInvoke(s.substring(m.start(), m.end()));
            m.appendReplacement(sb, result);
            mend = m.end();
        }
        return sb.toString() + s.substring(mend);
    }

    private String codeInvoke (String expr)
    {
        expr = replace(expr, "\\[#", empty);
        expr = replace(expr, "\\]", empty);
        String key = expr.substring(0, expr.indexOf("("));
        if (mapInvoke.containsKey(key))
        {
            String strParam = expr.substring(expr.indexOf("(") + 1, expr.indexOf(")"));
            Method m = mapInvoke.get(key);
            Class<?>[] types = m.getParameterTypes();
            String[] strParams = strParam.split("\\s*,\\s*");
            if (types.length != strParams.length)
            {
                return makeDoc(expr + " param count mismatch.");
            }
            Object[] params = new Object[types.length];
            for (int i = 0; i < types.length; i++)
            {
                Class<?> t = types[i];
                if (t.equals(String.class))
                    params[i] = strParams[i];
                else if (t.equals(Byte.class))
                    params[i] = Byte.parseByte(strParams[i]);
                else if (t.equals(Short.class))
                    params[i] = Short.parseShort(strParams[i]);
                else if (t.equals(Integer.class))
                    params[i] = Integer.parseInt(strParams[i]);
                else if (t.equals(Long.class))
                    params[i] = Long.parseLong(strParams[i]);
                else if (t.equals(Float.class))
                    params[i] = Float.parseFloat(strParams[i]);
                else if (t.equals(Double.class))
                    params[i] = Double.parseDouble(strParams[i]);
                else if (t.equals(Boolean.class))
                    params[i] = Boolean.parseBoolean(strParams[i]);
                //else if (t.isEnum())
                //    params[i] = Enum.valueOf(t, strParams[i]);
                else
                    return makeDoc(expr + " type unsupported: " + t);
            }
            try
            {
                return m.invoke(null, params).toString();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return makeDoc(expr);
    }

    String codeOneFileBody (List<TypeEnum> enums, List<TypeStruct> structs, TreeSet<String> imps)
    {
        StringBuilder codeEnums = new StringBuilder();
        StringBuilder codeStructs = new StringBuilder();
        if (!snippetTryGet(Key.ENUM).equals(empty))
        {
            for (TypeEnum type : enums)
            {
                String s = snippetGet(Key.ENUM);
                if (s.indexOf("body") < 0)
                    continue;
                String body = makeEnumBlock(type);
                s = replace(s, Token.Name, type.getName());
                s = replace(s, Token.Body, body);
                s = replace(s, Token.Doc, makeDoc(type.getComment()));
                codeEnums.append(s);
            }
        }
        for (TypeStruct type : structs)
        {
            impsCheckAdd(imps, type, type);
            String block = makeStructBlock(type, imps);
            codeStructs.append(block);
        }

        String blockEnums = codeEnums.toString();
        String blockStructs = codeStructs.toString();
        if (blockEnums.equals(empty) && blockStructs.equals(empty))
        {
            return empty;
        }
        String s = snippet.tryGet(Key.FILE_BODY,
                                  "//Error: No template named '" + Key.FILE_BODY + "' in " + snippet.getSnippetPath());
        s = replace(s, Token.Import, makeImorts(imps));
        s = replace(s, Token.Enums, blockEnums);
        s = replace(s, Token.Structs, blockStructs);
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
        String s = snippet.tryGet(Key.FILE_PACK,
                                  "//Error: No template named '" + Key.FILE_PACK + "' in " + snippet.getSnippetPath());
        s = replace(s, Token.Name, name);
        s = replace(s, Token.Body, body);
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
        s = replace(s, Token.Name, typeName);
        s = replace(s, Token.Body, block);
        s = replace(s, Token.Doc, empty);
        addExportFile(fileDir, typeName + suffix, makePack(fileDir, Key.RUNTIME_NAME, s, imps));
    }

    protected String makeDocLine (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC_LINE);
        s = replace(s, Token.Doc, comment);
        return s;
    }

    protected String makeDoc (String comment)
    {
        if (comment == null || comment.equals(empty))
            return empty;
        String s = snippetGet(Key.DOC);
        s = replace(s, Token.Doc, comment);
        return s;
    }

    protected String makePack (String packName, String typeName, String blockCode, TreeSet<String> imps)
    {
        String m = packName + "_" + typeName;
        m = m.toUpperCase();
        m = replace(m, "\\.", "_");

        String s = snippetGet(Key.PACK);
        s = replace(s, Token.Define, m);
        s = replace(s, Token.Name, packName);
        s = replace(s, Token.Body, blockCode);
        s = replace(s, Token.Import, makeImorts(imps));
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
            s = replaceFirst(s, Token.Name, fixedLen(lenKey, key));
            s = replace(s, Token.Name, key);
            s = replace(s, Token.Type, type.getName());
            s = replace(s, Token.Value, fixedLenBackward(whiteSpace, lenVal, type.getValue(key).toString()));
            s = replace(s, Token.Doc, makeDoc(type.getComment(key)));
            code.append(s);
        }
        return code.toString();
    }

    private String makeStructBlock (TypeStruct type, TreeSet<String> imps)
    {
        List<InvarField> fs = type.listFields();

        if (type.getSuperType() != null)
            impsCheckAdd(imps, type.getSuperType(), type);
        int widthType = 1;
        int widthName = 1;
        int widthDeft = 1;
        for (InvarField f : fs)
        {
            f.makeTypeFormatted(getContext(), snippetGet(Key.IMPORT_SPLIT), useFullName);
            f.setDeftFormatted(makeStructFieldInit(f, type));

            if (f.getDeftFormatted().length() > widthDeft)
                widthDeft = f.getDeftFormatted().length();
            if (f.getTypeFormatted().length() > widthType)
                widthType = f.getTypeFormatted().length();
            if (f.getKey().length() > widthName)
                widthName = f.getKey().length();

            impsCheckAdd(imps, f.getType().getRedirect(), type);
            for (InvarType typeGene : f.getGenerics())
            {
                impsCheckAdd(imps, typeGene.getRedirect(), type);
            }
        }

        StringBuilder ctor = new StringBuilder();
        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();

        Iterator<InvarField> i = fs.iterator();
        while (i.hasNext())
        {
            InvarField f = i.next();
            f.setWidthType(widthType);
            f.setWidthKey(widthName);
            f.setWidthDefault(widthDeft);
            fields.append(makeStructField(f, type));
            setters.append(makeStructSetter(f, type));
            getters.append(makeStructGetter(f, type));
            ctor.append(makeConstructorField(f, type, i.hasNext()));
        }
        String s = snippetGet(Key.STRUCT);
        s = replace(s, Token.Name, type.getName());
        s = replace(s, Token.Doc, makeDoc(type.getComment()));
        s = replace(s, Token.LessCompare, makeLessMethod(type));
        s = replace(s, Token.Constructor, ctor.toString());
        s = replace(s, Token.Fields, fields.toString());
        s = replace(s, Token.Setters, setters.toString());
        s = replace(s, Token.Getters, getters.toString());
        s = replace(s, Token.Copy, nestedCoder.code(Key.PREFIX_COPY, useFullName, type, fs, imps));
        s = replace(s, Token.Decoder, nestedCoder.code(Key.PREFIX_READ, useFullName, type, fs, imps));
        s = replace(s, Token.Encoder, nestedCoder.code(Key.PREFIX_WRITE, useFullName, type, fs, imps));
        return s;
    }

    private String makeLessMethod (TypeStruct type)
    {
        String s = snippetTryGet("less.method");
        if (s.equals(empty))
            return s;
        s = replace(s, Token.Body, snippetGet(type.getField("key") != null ? "less.body.key" : "less.body.deft"));
        s = replace(s, Token.Type, type.getName());
        return s;
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
        s = replace(s, Token.Type, f.createAliasRule(getContext(), "."));
        s = replace(s, Token.Name, f.getShortName());
        code.append(s);
        return code;
    }

    private StringBuilder makeStructField (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        String typeName = f.getTypeFormatted();
        String fieldName = f.isStructSelf() ? snippetTryGet(Key.POINTER_SPEC) : empty;
        fieldName += f.getKey();
        String s = snippetGet(Key.STRUCT_FIELD);
        int deltaKeyWidth = 0;
        if (typeName.length() > f.getWidthType())
            deltaKeyWidth = typeName.length() - f.getWidthType();
        int deltaValWidth = 0;
        if (f.getDeftFormatted().length() > f.getWidthDefault())
            deltaValWidth = f.getDeftFormatted().length() - f.getWidthDefault();
        s = replace(s, Token.Type, fixedLen(f.getWidthType(), typeName));
        s = replace(s, Token.Name, fixedLen(f.getWidthKey() - deltaKeyWidth, fieldName));
        s = replace(s, Token.Value, fixedLen(f.getWidthDefault() - deltaKeyWidth - deltaValWidth, f.getDeftFormatted()));
        s = replace(s, Token.Doc, makeDocLine(f.getComment()));
        code.append(s);
        return code;
    }

    private String makeConstructorField (InvarField f, TypeStruct type, Boolean hasNext)
    {
        if (snippetTryGet(Key.CONSTRUCT_FIELD).equals(empty))
            return empty;
        String s = snippetGet(Key.CONSTRUCT_FIELD);
        s = replace(s, Token.Name, fixedLen(f.getWidthKey(), f.getKey()));
        s = replace(s, Token.Value, f.getDeftFormatted());
        if (hasNext)
            s += snippetGet(Key.CONSTRUCT_FIELD_SPLIT);
        return s;
    }

    private StringBuilder makeStructSetter (InvarField f, TypeStruct struct)
    {
        StringBuilder code = new StringBuilder();
        if (TypeID.VEC == f.getType().getId())
            return code;
        if (TypeID.MAP == f.getType().getId())
            return code;

        String type = f.getTypeFormatted();
        if (f.getType().getRealId().getUseRefer() && !f.isStructSelf())
            type = snippetTryGet(Key.REFER_CONST) + whiteSpace + type;

        String s = snippetGet(Key.STRUCT_SETTER);
        s = replace(s, Token.TypeHost, struct.getName());
        s = replace(s, Token.Meta, makeStructMeta(f).toString());
        s = replace(s, Token.Type, type);
        s = replace(s, Token.Specifier, makeStructFieldSpec(f, struct, empty));//pointer or refer
        s = replace(s, Token.Name, f.getKey());
        s = replace(s, Token.NameUpper, upperHeadChar(f.getKey()));
        s = replace(s, Token.Doc, makeDoc(f.getComment()));
        code.append(s);
        return code;
    }

    private StringBuilder makeStructGetter (InvarField f, TypeStruct struct)
    {
        String type = fixedLen(f.getWidthType(), f.getTypeFormatted());
        String name = fixedLen(f.getWidthKey(), upperHeadChar(f.getKey()));

        StringBuilder code = new StringBuilder();
        String s = snippetGet(Key.STRUCT_GETTER);
        s = replace(s, Token.TypeHost, struct.getName());
        s = replace(s, Token.Meta, makeStructMeta(f).toString());
        s = replace(s, Token.Type, type);
        s = replace(s, Token.Specifier, makeStructFieldSpec(f, struct, whiteSpace));//pointer or refer
        s = replace(s, Token.Name, f.getKey());
        s = replace(s, Token.NameUpper, name);
        s = replace(s, Token.Doc, makeDoc(f.getComment()));
        s = replace(s, Token.DocLine, makeDocLine(f.getComment()));
        code.append(s);
        return code;
    }

    private String makeStructFieldSpec (InvarField f, TypeStruct struct, String deft)
    {
        if (f.isStructSelf())
            return snippetTryGet(Key.POINTER_SPEC);
        else if (f.getType().getRealId().getUseRefer())
            return snippetTryGet(Key.REFER_SPEC);
        else
            return deft;
    }

    protected String makeStructFieldInit (InvarField f, TypeStruct struct)
    {
        String deft = f.getDefault();
        InvarType type = f.getType();
        if (deft != null && !deft.equals(empty))
        {
            return type.getInitPrefix() + deft + type.getInitSuffix();
        }
        if (f.isStructSelf())
        {
            String s = snippetGet(Key.POINTER_NULL);
            return s;
        }
        String s = null;
        switch (type.getRealId()) {
        case STRUCT:
        case VEC:
        case MAP:
            String t = f.getTypeFormatted();
            s = snippetGet(Key.INIT_STRUCT);
            s = replace(s, Token.Type, t);
            return s;
        case ENUM:
            TypeEnum tEnum = (TypeEnum)type;
            s = snippetGet(Key.INIT_ENUM);
            s = replace(s, Token.Type, tEnum.getName());
            s = replace(s, Token.Name, tEnum.firstOptionKey());
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
        s = replace(s, Token.Body, block);
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
            impsCheckAdd(imps, type, null);

            String key = null;
            if (TypeID.VEC == type.getRealId())
                key = Key.RUNTIME_ALIAS_VEC;
            else if (TypeID.MAP == type.getRealId())
                key = Key.RUNTIME_ALIAS_MAP;
            else
                key = Key.RUNTIME_ALIAS_BASIC;

            String s = snippetGet(key);
            s = replace(s, Token.Name, alias);
            s = replace(s, Token.Type, type.getName());

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
        s = replace(s, Token.Name, name);
        s = replace(s, Token.Body, block);
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
                String[] names = key.split(ruleTypeSplit);
                if (names.length <= 0)
                    continue;
                String body = snippetGet(Key.IMPORT_BODY);
                if (names.length < 1)
                {
                    continue;
                }
                else if (names.length == 1)
                {
                    body = replace(body, Token.Pack, empty);
                    body = replace(body, Token.Name, names[0]);
                }
                else
                {
                    String split = snippetGet(Key.IMPORT_SPLIT);
                    String pName = names[0];
                    pName = replace(pName, dotToken, split);
                    body = replace(body, Token.Pack, pName);
                    if (pName.equals(empty))
                        split = empty;
                    body = replace(body, Token.Name, split + names[1]);
                }
                if (body.equals(empty))
                    continue;
                String s = snippetGet(Key.IMPORT);
                s = replace(s, Token.Body, body);
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

    protected void impsCheckAdd (TreeSet<String> imps, InvarType t, TypeStruct struct)
    {
        String include = t.getCodePath();
        if (include != null && !include.equals(empty))
        {
            if (super.getLowerFileName())
                include = include.toLowerCase();
            if (t == struct)
            {
                if (includeSelf)
                    fileIncludes.add(include);
            }
            else
            {
                if (!includeSelf)
                    fileIncludes.add(include);
            }
        }
        //TODO if (impExcludeConflict && t.getIsConflict())
        if (impExcludeConflict && getContext().findTypes(t.getName()).size() > 1)
        {
            return;
        }
        if (impExcludeSamePack && struct != null && t.getPack() == struct.getPack())
        {
            return;
        }
        if (impExcludePacks != null && impExcludePacks.size() > 0 && impExcludePacks.contains(t.getPack().getName()))
        {
            return;
        }
        String packName = t.getPack().getName();
        String typeName = t.getName();
        String rule = packName + ruleTypeSplit + typeName;

        imps.add(rule);
    }

    protected void impsCheckAdd (TreeSet<String> imps, String ss, TypeStruct struct)
    {
        if (ss.equals(empty))
            return;
        ss = ss.trim();
        String[] lines = ss.split((","));
        for (String line : lines)
        {
            if (line.equals(empty))
                continue;
            InvarType t = super.findType(getContext(), line);
            if (t == null)
            {
                logErr("impsCheckAdd() --- Can't find type named " + line);
                continue;
            }
            impsCheckAdd(imps, t, struct);
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

    static private Boolean genericOverride = false; //For C++ template ">>" issue in GCC

    static private String createRule (InvarField f, InvarContext ctx, Boolean useFullName)
    {
        String split = ruleTypeSplit;
        InvarType typeBasic = f.getType().getRedirect();
        if (f.getGenerics().size() == 0)
        {
            if (ctx.findTypes(typeBasic.getName()).size() > 1 || useFullName)
                return typeBasic.fullName(split);
            else
                return typeBasic.getName();
        }
        String s = getGenericOverride(typeBasic);
        for (InvarType t : f.getGenerics())
        {
            t = t.getRedirect();
            String forShort = null;
            if (t.getRealId() == TypeID.VEC || t.getRealId() == TypeID.MAP)
                forShort = t.getName();
            else
            {
                if (ctx.findTypes(t.getName()).size() > 1 || useFullName)
                    forShort = t.fullName(split);
                else
                    forShort = t.getName();
            }
            s = s.replaceFirst("\\?", forShort + getGenericOverride(t));
        }
        String rule = useFullName ? typeBasic.fullName(split) : typeBasic.getName();
        rule = rule + s;
        return rule;
    }

    static String getGenericOverride (InvarType t)
    {
        String s = t.getRealId().getGeneric();
        if (genericOverride)
        {
            s = t.getGeneric();
        }
        return s;
    }

    public String makeCodeAssignment (String type, String name, String value, Boolean refer)
    {
        name = name.trim();
        value = value.trim();
        String s = snippetGet(Key.CODE_ASSIGNMENT);
        if (value.equals(name))
        {
            value = empty;
        }
        if (value.equals(empty))
        {
            refer = false;
            s = snippetGet(Key.CODE_DEFINITION);
        }
        if (refer)
            name = snippetTryGet(Key.REFER_SPEC) + name;
        s = replace(s, Token.Value, value);
        s = replace(s, Token.Type, !type.equals(empty) ? type + whiteSpace : empty);
        s = replace(s, Token.Name, name);
        return s;
    }

    private class NestedCoder
    {
        final private TypeID sizeType    = TypeID.UINT32;
        private Boolean      useFullName = false;
        private String       prefix      = empty;
        private String       snippetMet  = empty;
        private String       snippetArg  = empty;

        public String code (String prefix,
                            Boolean useFullName,
                            TypeStruct type,
                            List<InvarField> fs,
                            TreeSet<String> imps)
        {
            impsCheckAdd(imps, snippetTryGet(prefix + Key.IMPORT), type);
            this.prefix = prefix;
            this.useFullName = useFullName;
            this.snippetMet = snippetTryGet(prefix + "method");
            this.snippetArg = snippetTryGet(prefix + "method.arg");
            if (empty.equals(snippetMet))
                return empty;
            List<String> lines = new ArrayList<String>();
            for (InvarField f : fs)
                lines.addAll(makeField(f));
            return makeCodeMethod(lines, type.getName(), snippetMet);
        }

        private String makeCodeMethod (List<String> lines, String returnType, String snippet)
        {
            indentLines(lines, methodIndentNum);
            StringBuilder body = new StringBuilder();
            for (String line : lines)
            {
                body.append(br + line);
            }
            String s = snippet;
            s = replace(s, Token.Type, returnType);
            s = replace(s, Token.Body, body.toString());
            return s;
        }

        private List<String> makeField (InvarField f)
        {
            String rule = createRule(f, getContext(), useFullName);
            TypeID type = f.getType().getRealId();
            List<String> lines = new ArrayList<String>();
            NestedParam params = makeParams(null, rule, f.getKey(), empty);
            makeGeneric(f, type, rule, params, lines);
            return lines;
        }

        private void makeGeneric (InvarField f, TypeID type, String rule, NestedParam p, List<String> lines)
        {
            p.field = f;
            p.type = type;
            String code = null;
            if (TypeID.VEC == type)
                code = makeUnitVec(p, rule);
            else if (TypeID.MAP == type)
                code = makeUnitMap(p, rule);
            else
                code = makeUnitSimple(p, type);
            code = replace(code, Token.Argument, snippetArg);
            lines.addAll(indentLines(code));
        }

        private String makeUnitVec (NestedParam p, String rule)
        {
            String ruleV = ruleRight(rule);
            String nameVal = "n" + p.depth;
            NestedParam pVal = makeParams(p, ruleV, nameVal, ".n");
            String body = empty;
            body += makeUnitGeneric(TypeID.VEC, ruleV, pVal);
            String head = makeGenericDefine(p);
            return head + makeUnitIter(TypeID.VEC.getName(), body, p, pVal, null);
        }

        private String makeUnitMap (NestedParam p, String rule)
        {
            String r = ruleRight(rule);
            String[] R = r.split(",");
            String ruleK = R[0];
            String ruleV = R[1];
            String nameKey = "k" + p.depth;
            String nameVal = "v" + p.depth;
            NestedParam pKey = makeParams(p, ruleK, nameKey, ".k");
            NestedParam pVal = makeParams(p, ruleV, nameVal, ".v");
            String body = empty;
            body += makeUnitGeneric(TypeID.MAP, ruleK, pKey);
            body += makeUnitGeneric(TypeID.MAP, ruleV, pVal);
            String head = makeGenericDefine(p);
            return head + makeUnitIter(TypeID.MAP.getName(), body, p, pVal, pKey);
        }

        private String makeUnitGeneric (TypeID id, String rule, NestedParam p)
        {
            String L = ruleLeft(rule);
            InvarType t = getTypeByShort(L);
            if (t == null)
            {
                logErr("No type named " + L);
                return empty;
            }
            TypeID type = t.getRealId();
            String iterSuffix = p.snippetTag;
            p.snippetRef = snippetTryGet(prefix + id.getName() + ".iter" + iterSuffix);
            p.snippetDef = snippetTryGet(prefix + id.getName() + ".def" + iterSuffix);
            List<String> body = new ArrayList<String>();
            makeGeneric(p.field, type, rule, p, body);
            return buildCodeLines(body).toString();
        }

        private String makeGenericDefine (NestedParam p)
        {
            String s0 = empty;
            String s = empty;
            if (p.parent == null)
            {
                s0 = snippetTryGet(prefix + p.type.getName() + ".field");
                s0 = replace(s0, Token.Type, p.rule);
                s0 = replace(s0, Token.Name, p.name);
            }
            if (p.parent != null && p.parent.type != TypeID.VOID)
            {
                s = snippetTryGet(prefix + p.parent.type.getName() + ".define");
                String v = snippetTryGet(prefix + p.parent.type.getName() + ".define" + p.snippetTag);
                s = replace(s, Token.Value, v);

                s = replace(s, Token.Type, p.rule);
                s = replace(s, Token.Name, p.name);
                s = replace(s, Token.TypeUpper, p.parent.rule);
                s = replace(s, Token.NameUpper, p.parent.name);

            }
            return s0 + s;
        }

        private String makeUnitIter (String typeName, String body, NestedParam param, NestedParam pv, NestedParam pk)
        {
            String s = snippetGet(prefix + typeName + ".for");
            String iterNameUp = upperHeadChar(param.name);
            String sizeType = getContext().findBuildInType(this.sizeType).getRedirect().getName();
            String sizeName = "len" + iterNameUp;
            s = replace(s, Token.Body, body);
            s = replace(s, Token.SizeType, sizeType);
            s = replace(s, Token.Size, sizeName);
            s = replace(s, Token.Index, "i" + iterNameUp);
            s = replace(s, Token.Type, param.rule);
            s = replace(s, Token.Name, param.name);
            if (param.parent != null)
            {
                s = replace(s, Token.TypeUpper, param.parent.rule);
                s = replace(s, Token.NameUpper, param.parent.name);
            }
            if (pk != null)
                s = replace(s, Token.Key, pk.name);
            s = replace(s, Token.Value, pv.name);

            return s;
        }

        private NestedParam makeParams (NestedParam parent, String rule, String name, String tag)
        {
            NestedParam p = new NestedParam();
            p.name = name;
            p.snippetTag = tag;
            p.parent = parent;
            p.depth = (parent != null ? parent.depth : 0) + 1;
            p.setRule(rule);
            if (parent != null)
            {
                p.field = parent.field;
            }
            return p;
        }

        private String makeUnitSimple (NestedParam p, TypeID t)
        {
            String k = prefix + t.getName();
            if (TypeID.STRUCT == t && p.field.isStructSelf())
                k += ".check";
            String s = snippetGet(k);
            String invoke = snippetTryGet(p.field.isStructSelf() ? Key.POINTER_INVOKE : Key.REFER_INVOKE);
            String split = invoke;
            if (p.depth > 1)
            {
                split = "_";
            }
            if (!p.snippetRef.equals(empty))
            {
                s = replace(s, Token.Name, p.snippetRef);
            }
            if (!p.snippetDef.equals(empty))
            {
                if (p.snippetRef.equals(empty))
                    s = p.snippetDef;
                else
                    s = p.snippetDef + s;
            }
            s = replace(s, Token.Type, p.rule);
            s = replace(s, Token.Name, p.name);
            s = replace(s, Token.Invoke, invoke);
            s = replace(s, Token.Split, split);
            s = replace(s, Token.NullPtr, snippetTryGet(Key.POINTER_NULL));
            if (p.parent != null)
            {
                s = replace(s, Token.TypeUpper, p.parent.rule);
                s = replace(s, Token.NameUpper, p.parent.name);
            }
            return s;
        }

    } //class

    private class NestedParam
    {
        private NestedParam parent     = null;
        private InvarField  field      = null;
        private Integer     depth      = 0;
        private TypeID      type       = TypeID.VOID;
        private String      rule       = empty;
        private String      name       = empty;
        private String      snippetRef = empty;
        private String      snippetDef = empty;
        private String      snippetTag = empty;

        public void setRule (String t)
        {
            String split = snippetGet(Key.IMPORT_SPLIT);
            t = t.replace(ruleTypeSplit, split);
            t = t.replaceAll("(^\\s*|\\s*$)", empty);
            this.rule = t;
        }
    }
}
