import invar.InvarContext;
import invar.InvarWrite;
import invar.model.InvarField;
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

@Deprecated
final public class InvarWriteJava extends InvarWrite
{
    public InvarWriteJava(InvarContext context, String dirRootPath)
    {
        super(context, dirRootPath);
    }

    final static private String mapKeyAttr = "key";
    final static private String indent     = "    ";
    final static private String br         = "\n";
    final static private String brIndent   = br + indent;
    final static private String brIndent2  = br + indent + indent;
    final static private String brIndent3  = br + indent + indent + indent;
    final static private String brIndent4  = br + indent + indent + indent + indent;

    @Override
    protected Boolean beforeWrite (final InvarContext c)
    {
        c.ghostClear();
        c.typeRedefine(TypeID.INT8, "java.lang", "Byte", "");
        c.typeRedefine(TypeID.INT16, "java.lang", "Short", "");
        c.typeRedefine(TypeID.INT32, "java.lang", "Integer", "");
        c.typeRedefine(TypeID.INT64, "java.lang", "Long", "");
        c.typeRedefine(TypeID.UINT8, "java.lang", "Short", "");
        c.typeRedefine(TypeID.UINT16, "java.lang", "Integer", "");
        c.typeRedefine(TypeID.UINT32, "java.lang", "Long", "");
        c.typeRedefine(TypeID.UINT64, "java.lang", "String", "");
        c.typeRedefine(TypeID.FLOAT, "java.lang", "Float", "");
        c.typeRedefine(TypeID.DOUBLE, "java.lang", "Double", "");
        c.typeRedefine(TypeID.STRING, "java.lang", "String", "");
        c.typeRedefine(TypeID.BOOL, "java.lang", "Boolean", "");
        c.typeRedefine(TypeID.MAP, "java.util", "HashMap", "<?,?>");
        c.typeRedefine(TypeID.LIST, "java.util", "LinkedList", "<?>");
        exportFile("/res/java/InvarRule.txt", "invar", "InvarRule.java");
        exportFile("/res/java/InvarReadData.txt", "invar", "InvarReadData.java");
        return true;
    }

    @Override
    protected String codeStruct (TypeStruct type)
    {
        List<InvarField> fs = type.listFields();
        int widthType = 1;
        int widthKey = 1;
        int widthDefault = 1;
        for (InvarField f : fs)
        {
            f.makeTypeFormatted(getContext());
            if (f.getTypeFormatted().length() > widthType)
                widthType = f.getTypeFormatted().length();
            if (f.getKey().length() > widthKey)
                widthKey = f.getKey().length();
            String deft = evalFieldDefault(f);
            if (deft.length() > widthDefault)
                widthDefault = deft.length();
        }
        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();
        TreeSet<String> imps = new TreeSet<String>();

        List<InvarField> genericFields = new LinkedList<InvarField>();
        for (InvarField f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthKey);
            f.setWidthDefault(widthDefault);
            fields.append(codeStructField(f, type));
            setters.append(codeStructSetter(f, type.getName()));
            getters.append(codeStructGetter(f));
            impsCheckAdd(imps, f.getType().getRedirect());
            for (InvarType typeGene : f.getGenerics())
            {
                impsCheckAdd(imps, typeGene.getRedirect());
            }

            imps.add("invar.InvarRule");
            switch (f.getType().getId()) {
            case MAP:
            case LIST:
                genericFields.add(f);
                break;
            default:
                break;
            }
        }
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(br);
        body.append(setters);
        body.append(br);
        body.append(getters);
        body.append(codeToXmlStruct(type));
        for (InvarField f : genericFields)
        {
            StringBuilder code = new StringBuilder();
            code.append("StringBuilder code = new StringBuilder(); ");
            String rule = f.createFullNameRule(getContext(), ".");
            code.append(codeToXmlNode(0, rule, new StringBuilder(), f.getKey(), f.getKey(), "", ""));
            code.append(brIndent2 + "return code;");
            String mName = "public StringBuilder toXml" + upperHeadChar(f.getKey()) + "()";
            body.append(br);
            body.append(codeMethod(mName, code.toString()));
        }
        return codeClassFile(type, body, codeStructImports(imps));
    }

    private void impsCheckAdd (TreeSet<String> imps, InvarType t)
    {
        if (getContext().findTypes(t.getName()).size() > 1)
            return;
        String str = t.fullName(".");
        imps.add(str);
        if (t.getId() == TypeID.MAP)
            imps.add("java.util.Iterator");
    }

    private StringBuilder codeToXmlStruct (TypeStruct type)
    {
        StringBuilder code = new StringBuilder();
        code.append("StringBuilder code = new StringBuilder(); ");
        List<InvarField> fs = type.listFields();
        List<InvarField> structFields = new LinkedList<InvarField>();
        LinkedHashMap<String,String> attrs = new LinkedHashMap<String,String>();
        for (InvarField f : fs)
        {
            switch (f.getType().getId()) {
            case MAP:
            case LIST:
            case STRUCT:
                structFields.add(f);
                break;
            default:
                String k = f.getKey();
                String rule = f.createFullNameRule(getContext(), ".");
                InvarType t = findType(getContext(), ruleLeft(rule));
                String s = "this." + k;
                if (TypeID.ENUM == t.getId())
                    s = s + ".value";
                attrs.put(shortNameField(f), s);
                break;
            }
        }
        codeToXmlNodeStart(code, "\" + nodeName + \"", attrs, structFields.size() > 0, 0);
        if (structFields.size() > 0)
        {
            for (InvarField f : structFields)
            {
                String rule = f.createFullNameRule(getContext(), ".");
                code.append(codeToXmlNode(0, rule, new StringBuilder(), f.getKey(), shortNameField(f), "", ""));
            }
            code.append(brIndent2 + "code.append(\"</\" + nodeName + \">\");");
        }
        code.append(brIndent2 + "return code.toString();");
        return codeMethod("public String toXmlString(String nodeName)", code.toString());
    }

    private String shortNameField (InvarField f)
    {
        String k = f.getKey();
        if (k.equals(mapKeyAttr))
            return k;
        String shortKey = f.getShortName() != null ? f.getShortName() : k;
        return shortKey;
    }

    private StringBuilder codeToXmlNode (int depth,
                                         String rule,
                                         StringBuilder code,
                                         String upRef,
                                         String strNode,
                                         String nodePrefix,
                                         String keyAttr)
    {
        if (code == null)
            code = new StringBuilder();
        if (rule == null)
            return code;
        String ind = brIndent2;
        if (depth > 0)
            ind = brIndent3;
        String L = ruleLeft(rule);
        String R = ruleRight(rule);
        InvarType t = findType(getContext(), L);
        if (strNode.equals(""))
        {
            strNode = getContext().findBuildInType(t.getId()).getName();
        }
        String nodeName = nodePrefix + strNode;
        HashMap<String,String> attrs = new LinkedHashMap<String,String>();
        if (keyAttr != "")
        {
            attrs.put(mapKeyAttr, keyAttr);
        }
        if (TypeID.LIST == t.getId())
        {
            code.append(ind + "if (" + upRef + ".size() > 0)");
            code.append(ind + "{");
            codeToXmlNodeStart(code, nodeName, attrs, true, depth);
            codeToXmlVector(R, code, depth, upRef);
            code.append(ind + "code.append(\"</" + nodeName + ">\");");
            code.append(ind + "}");
        }
        else if (TypeID.MAP == t.getId())
        {
            code.append(ind + "if (" + upRef + ".size() > 0)");
            code.append(ind + "{");
            codeToXmlNodeStart(code, nodeName, attrs, true, depth);
            codeToXmlMap(R, code, depth, upRef);
            code.append(ind + "code.append(\"</" + nodeName + ">\");");
            code.append(ind + "}");
        }
        else if (TypeID.STRUCT == t.getId())
        {
            if (depth == 0)
            {
                //nodeName = upRef;
                code.append(ind + "if (" + upRef + " != null)");
                code.append(ind);
            }
            if (keyAttr != "")
            {
                TypeStruct ts = (TypeStruct)t;
                if (ts.getField(mapKeyAttr) == null)
                {
                    log("error ---------> This struct must have a field named 'key': " + t.fullName("::"));
                }
            }
            String s = upRef;
            s = upRef + ".toXmlString(\"" + nodeName + "\")";
            code.append("code.append(" + s + ");");
        }
        else if (TypeID.ENUM == t.getId())
        {
            String s = upRef;
            attrs.put("value", s + ".value");
            codeToXmlNodeStart(code, nodeName, attrs, false, depth);
        }
        else
        {
            String s = upRef;
            //InvarType rawType = getContext().findBuildInType(t.getId());
            //nodeName = rawType.getName();
            attrs.put("value", s);
            codeToXmlNodeStart(code, nodeName, attrs, false, depth);
        }
        return code;
    }

    private void codeToXmlVector (String rule, StringBuilder code, int depth, String strO)
    {
        String ind = brIndent2;
        if (depth > 0)
            ind = brIndent3;
        String ruleV = rule;
        String vName = "n" + depth;
        code.append(ind + "for (" + ruleV + " " + vName + " : " + strO + ")");
        code.append(ind + "{");
        depth++;
        code.append(ind + codeToXmlNode(depth, rule, null, vName, codeToXmlNodeName(ruleV), "", ""));
        code.append(ind + "}");
    }

    private void codeToXmlMap (String rule, StringBuilder code, int depth, String strO)
    {
        String ind = brIndent2;
        if (depth > 0)
            ind = brIndent3;
        String[] R = rule.split(",");
        String ruleK = R[0];
        String ruleV = R[1];
        String kName = "k" + depth;
        String vName = "v" + depth;
        code.append(ind + "for (Iterator<" + ruleK + "> i = " + strO + ".keySet().iterator(); i.hasNext();)");
        code.append(ind + "{");
        code.append(ind + ruleK + " " + kName + " = i.next();");
        code.append(ind + ruleV + " " + vName + " = " + strO + ".get(" + kName + ");");
        InvarType typeK = findType(getContext(), ruleLeft(ruleK));
        TypeID id = typeK.getId();
        depth++;
        if (TypeID.MAP == id || TypeID.LIST == id || TypeID.STRUCT == id)
        {
            code.append(ind + codeToXmlNode(depth, ruleK, null, kName, codeToXmlNodeName(ruleK), "k-", ""));
            code.append(ind + codeToXmlNode(depth, ruleV, null, vName, codeToXmlNodeName(ruleV), "v-", ""));
        }
        else
        {
            code.append(ind + codeToXmlNode(depth, ruleV, null, vName, codeToXmlNodeName(ruleV), "", kName));
        }
        code.append(ind + "}");
    }

    private StringBuilder codeToXmlNodeStart (StringBuilder code,
                                              String nodeName,
                                              HashMap<String,String> attrs,
                                              Boolean isOpen,
                                              int depth)
    {
        String ind = brIndent2;
        if (depth > 0)
            ind = brIndent3;
        code.append(ind + "code.append(\"\\n\");");
        code.append(ind + "code.append(\"<" + nodeName + "\");");
        Iterator<String> i = attrs.keySet().iterator();
        String kk = mapKeyAttr;
        String v = attrs.get(kk);
        if (v != null)
            code.append(ind + "code.append(\" " + kk + "=\\\"\" + " + v + " + \"\\\"\");");
        while (i.hasNext())
        {
            String k = i.next();
            if (k.equals(kk))
                continue;
            v = attrs.get(k);
            code.append(ind + "code.append(\" " + k + "=\\\"\" + " + v + " + \"\\\"\");");
        }
        code.append(ind + "code.append(\"" + (isOpen ? "" : " /") + ">" + "\");");
        return code;
    }

    private String codeToXmlNodeName (String rule)
    {
        //  return "x" for smaller size
        return "x";
        //
        //        InvarType typeV = findType(getContext(), ruleLeft(rule));
        //        String strNode = typeV.getName();
        //        if (TypeID.MAP == typeV.getId() || TypeID.LIST == typeV.getId())
        //        {
        //            InvarType rawType = getContext().findBuildInType(typeV.getId());
        //            strNode = rawType.getName();
        //        }
        //        return strNode;
    }

    private StringBuilder codeStructImports (TreeSet<String> keys)
    {
        StringBuilder code = new StringBuilder();
        code.append(br);
        for (String key : keys)
        {
            if (key.equals("") || key.startsWith("java.lang"))
                continue;
            code.append(br);
            code.append("import ");
            code.append(key);
            code.append(";");
        }
        return code;
    }

    private StringBuilder codeStructField (InvarField f, TypeStruct s)
    {
        StringBuilder code = new StringBuilder();
        code.append(brIndent);
        code.append(fixedLen(f.getWidthType() + 1, f.getTypeFormatted() + " "));
        code.append(fixedLen(f.getWidthKey(), f.getKey()));
        code.append(" = ");
        String deft = f.getType() != s ? evalFieldDefault(f) : "null";
        code.append(fixedLen(f.getWidthDefault() + 1, deft + ";"));
        if (!f.getComment().equals(""))
        {
            code.append("// ");
            code.append(f.getComment());
        }
        return code;
    }

    private String codeStructSetter (InvarField f, String nameType)
    {
        if (TypeID.LIST == f.getType().getId())
            return "";
        if (TypeID.MAP == f.getType().getId())
            return "";
        StringBuilder code = new StringBuilder();
        if (!f.getComment().equals(""))
        {
            code.append(brIndent);
            code.append("/** @param value " + f.getComment() + " */");
        }
        String key = upperHeadChar(f.getKey());
        codeFieldAnno(code, f);
        code.append(brIndent);
        code.append("public " + nameType);
        code.append(" set" + key);
        code.append("(");
        code.append(f.getTypeFormatted());
        code.append(" value)");
        code.append(" {");
        code.append("this." + f.getKey());
        code.append(" = value;");
        code.append(" return this;");
        code.append("}");
        return code.toString();
    }

    private Object codeStructGetter (InvarField f)
    {
        StringBuilder code = new StringBuilder();
        if (!f.getComment().equals(""))
        {
            code.append(brIndent);
            code.append("/** " + f.getComment() + " */");
        }
        codeFieldAnno(code, f);
        code.append(brIndent);
        code.append("public ");
        code.append(f.getTypeFormatted());
        code.append(" get" + upperHeadChar(f.getKey()) + "() ");
        code.append("{");
        code.append("return " + f.getKey() + ";");
        code.append("}");
        code.append(br);
        return code;
    }

    private void codeFieldAnno (StringBuilder code, InvarField f)
    {
        String meta = f.createAliasRule(getContext(), ".");
        String shortName = f.getShortName() == null ? "" : f.getShortName();
        if (!meta.equals(""))
        {
            code.append(brIndent);
            code.append("@InvarRule(T=\"" + meta + "\", S=\"" + shortName + "\")");
        }
    }

    @Override
    protected String codeEnum (TypeEnum type)
    {
        StringBuilder code = new StringBuilder();
        Iterator<String> i = type.getKeys().iterator();
        int lenKey = 1;
        int lenVal = 1;
        while (i.hasNext())
        {
            String key = i.next();
            if (key.length() > lenKey)
                lenKey = key.length();
            if (type.getValue(key).toString().length() > lenVal)
                lenVal = type.getValue(key).toString().length();
        }
        String tName = type.getName();
        String sPub = "static final public ";
        StringBuilder codePuts = new StringBuilder();
        codePuts.append("if (map != null)");
        codePuts.append(brIndent3);
        codePuts.append("return map;");
        codePuts.append(brIndent2);
        codePuts.append("map = new HashMap<Integer," + tName + ">();");
        i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            if (!type.getComment(key).equals(""))
            {
                code.append(brIndent);
                code.append("/** " + type.getComment(key) + " */");
            }
            code.append(brIndent);
            code.append(sPub + tName + " ");
            code.append(fixedLen(lenKey, key) + " = new " + tName + "(");
            code.append(type.getValue(key) + ");");
            codePuts.append(brIndent2);
            codePuts.append("map.put(" + type.getValue(key) + ", " + key + ");");
        }
        codePuts.append(brIndent2);
        codePuts.append("return map;");
        StringBuilder codeParse = new StringBuilder();
        codeParse.append("HashMap<Integer," + tName + "> m = all();");
        codeParse.append(brIndent2);
        codeParse.append("return m.get(v);");
        code.append(br);
        code.append(codeMethod(sPub + tName + " parse(Integer v)", codeParse.toString()));
        code.append(br);
        code.append(codeMethod(sPub + "HashMap<Integer," + tName + "> all()", codePuts.toString()));
        code.append(br);
        code.append(brIndent);
        code.append("static private HashMap<Integer," + tName + "> map;");
        code.append(br);
        code.append(codeMethod("public " + type.getName() + "(Integer value)", "this.value = value;"));
        code.append(br);
        code.append(codeMethod("@Override" + brIndent + "public String toString()",//
                               "return value.toString();"));
        code.append(br);
        code.append(brIndent);
        code.append("public final Integer value;");
        code.append(brIndent);

        StringBuilder imports = new StringBuilder();
        imports.append(br);
        imports.append(br);
        imports.append("import java.util.HashMap;");

        return codeClassFile(type, code, imports);
    }

    private StringBuilder codeMethod (String head, String body)
    {
        StringBuilder code = new StringBuilder();
        code.append(brIndent);
        code.append(head);
        code.append(brIndent);
        code.append("{");
        code.append(brIndent2);
        code.append(body);
        code.append(brIndent);
        code.append("}");
        return code;
    }

    @Override
    protected void codeRuntime (String suffix)
    {
        StringBuilder meBasic = new StringBuilder();
        StringBuilder meEnums = new StringBuilder();
        StringBuilder meStruct = new StringBuilder();

        String hm2 = "HashMap<String,Class<?>> map = new HashMap<String,Class<?>>();";
        Iterator<String> i = getContext().aliasNames();
        TreeSet<String> imps = new TreeSet<String>();
        meBasic.append(hm2);
        meEnums.append(hm2);
        meStruct.append(hm2);
        while (i.hasNext())
        {
            String alias = i.next();
            InvarType type = getContext().aliasGet(alias);
            imps.add(type.getPack().getName() + "." + type.getName());
            String put = brIndent2 + "map.put(\"" + alias + "\", " + type.getName() + ".class);";
            if (type instanceof TypeStruct)
                meStruct.append(put);
            else if (type instanceof TypeEnum)
                meEnums.append(put);
            else
                meBasic.append(put);
        }
        meBasic.append(brIndent2 + "return map;");
        meEnums.append(brIndent2 + "return map;");
        meStruct.append(brIndent2 + "return map;");

        StringBuilder meMain = new StringBuilder();
        StringBuilder meStart = new StringBuilder();
        StringBuilder meParse = new StringBuilder();
        StringBuilder meParseFull = new StringBuilder();
        StringBuilder meInit = new StringBuilder();

        TypeStruct root = getContext().getStructRoot();
        String rootTypeName = root == null ? "Object" : root.getName();

        String hMain = "static public void main(String[] args) throws Exception";
        String hStart3 = "static public void start(String dir, String suffix, Boolean verbose) throws Exception";
        String hStart4 = "static public void start(Object o, String dir, String suffix, Boolean verbose) throws Exception";
        String hParse = "static public void parse(Object o, String xml) throws Exception";
        String hParseFull = "static public void parseFull(Object o, String xml) throws Exception";
        String hInit = "static private void init()";

        String hBasic = "static private HashMap<String,Class<?>> aliasBasic()";
        String hEnum = "static private HashMap<String,Class<?>> aliasEnums()";
        String hStruct = "static private HashMap<String,Class<?>> aliasStructs()";

        meMain.append("HashMap<String,List<String>> mapArgs = new HashMap<String,List<String>>();");
        meMain.append(brIndent2 + "List<String> listCurrent = null;");
        meMain.append(brIndent2 + "for (String arg : args)");
        meMain.append(brIndent2 + "{");
        meMain.append(brIndent3 + "if (arg.charAt(0) == '-')");
        meMain.append(brIndent3 + "{");
        meMain.append(brIndent4 + "listCurrent = new LinkedList<String>();");
        meMain.append(brIndent4 + "mapArgs.put(arg, listCurrent);");
        meMain.append(brIndent3 + "}");
        meMain.append(brIndent3 + "else if (listCurrent != null)");
        meMain.append(brIndent3 + "{");
        meMain.append(brIndent4 + "listCurrent.add(arg);");
        meMain.append(brIndent3 + "}");
        meMain.append(brIndent2 + "}");
        meMain.append(brIndent2 + "List<String> argS = mapArgs.get(\"-suffix\");");
        meMain.append(brIndent2 + "List<String> argP = mapArgs.get(\"-path\");");
        meMain.append(brIndent2 + "String suffix = argS != null && argS.size() > 0 ? argS.get(0) : \".xml\";");
        meMain.append(brIndent2 + "String path = argP != null && argP.size() > 0 ? argP.get(0) : \"data\";");
        meMain.append(root != null
            ? brIndent2 + "start(new " + root.getName() + "(), path, suffix, true);"
            : brIndent2 + "System.out.println(\"Please define a struct with alias '" + getContext().getStructRootAlias() + "'.\");");

        //meMain.append(brIndent2 + "System.out.println(new Info().toXmlString(\"ss\"));");

        meStart.append("init();");
        meStart.append(brIndent2 + "InvarReadData.verbose = verbose;");
        meStart.append(brIndent2 + "InvarReadData.start(o, dir, suffix);");

        meParse.append("init();");
        meParse.append(brIndent2 + "InvarReadData.parse(o, xml);");

        meParseFull.append("init();");
        meParseFull.append(brIndent2 + "InvarReadData.parseFull(o, xml);");

        meInit.append("if (InvarReadData.aliasBasics != null)");
        meInit.append(brIndent3 + "return;");
        meInit.append(brIndent2 + "InvarReadData.aliasBasics = aliasBasic();");
        meInit.append(brIndent2 + "InvarReadData.aliasEnums = aliasEnums();");
        meInit.append(brIndent2 + "InvarReadData.aliasStructs = aliasStructs();");

        StringBuilder body = new StringBuilder();

        body.append(brIndent);
        body.append("static public final ");
        body.append(rootTypeName);
        body.append(" root = new ");
        body.append(rootTypeName);
        body.append("();");
        body.append(br);
        body.append(codeMethod(hStart3, "start(root, dir, suffix, verbose);"));
        body.append(br);
        body.append(codeMethod(hStart4, meStart.toString()));
        body.append(br);
        body.append(codeMethod(hParse, meParse.toString()));
        body.append(br);
        body.append(codeMethod(hParseFull, meParseFull.toString()));
        body.append(br);
        body.append(codeMethod(hInit, meInit.toString()));
        body.append(br);
        body.append(codeMethod(hBasic, meBasic.toString()));
        body.append(br);
        body.append(codeMethod(hEnum, meEnums.toString()));
        body.append(br);
        body.append(codeMethod(hStruct, meStruct.toString()));
        body.append(br);
        body.append(codeMethod(hMain, meMain.toString()));
        imps.add("java.util.List");
        //return ;
        //addExportFile(fileDir, fileName, body.toString());
    }

    private String codeClassFile (InvarType type, StringBuilder body, StringBuilder imports)
    {
        StringBuilder code = new StringBuilder();
        code.append("// THIS FILE IS GENERATED BY INVAR. DO NOT EDIT !!!");
        if (!type.getPack().getName().equals(""))
        {
            code.append(br);
            code.append("package " + type.getPack().getName() + ";");
        }
        if (imports != null)
        {
            code.append(imports);
        }
        code.append(br);
        if (!type.getComment().equals(""))
        {
            code.append(br);
            code.append("/** " + type.getComment() + " */");
        }
        code.append(br);
        code.append("final public class " + type.getName());
        code.append(br);
        code.append("{");
        code.append(body);
        code.append(br);
        code.append("}");
        return code.toString();
    }

    private String evalFieldDefault (InvarField f)
    {
        String deft = "";
        String deftF = f.getDefault();
        switch (f.getType().getId()) {
        case STRING:
            deft = "\"" + deftF + "\"";
            break;
        case INT8:
        case INT16:
        case INT32:
            deft = deftF.equals("") ? "-1" : deftF;
            break;
        case UINT8:
        case UINT16:
            deft = deftF.equals("") ? "0" : deftF;
            break;
        case UINT32:
            deft = deftF.equals("") ? "0L" : deftF + "L";
            break;
        case INT64:
            deft = deftF.equals("") ? "-1L" : deftF + "L";
            break;
        case UINT64:
            deft = deftF.equals("") ? "\"0\"" : "\"" + deftF + "\"";
            break;
        case FLOAT:
            deft = deftF.equals("") ? "0.00F" : deftF + "F";
            break;
        case DOUBLE:
            deft = deftF.equals("") ? "0.00" : deftF;
            break;
        case BOOL:
            deft = deftF.equals("") ? "false" : deftF;
            break;
        case ENUM:
            Iterator<String> i = ((TypeEnum)f.getType()).getKeys().iterator();
            if (deftF.equals(""))
            {
                deft = i.hasNext()
                    ? f.getType().getName() + "." + i.next()
                    : "new " + f.getType().getName() + "(-999999)";
            }
            else
            {
                deft = deftF;
            }
            break;
        default:
            deft = "new " + f.getTypeFormatted() + "()";
        }
        return deft;
    }

}
