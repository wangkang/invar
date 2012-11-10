package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

final public class InvarWriteJava extends InvarWrite
{
    public InvarWriteJava(InvarContext context, String dirRootPath)
    {
        super(context, dirRootPath);
    }

    final static private String indent    = "    ";
    final static private String br        = "\n";
    final static private String brIndent  = br + indent;
    final static private String brIndent2 = br + indent + indent;
    final static private String brIndent3 = br + indent + indent + indent;
    final static private String brIndent4 = br + indent + indent + indent + indent;

    @Override
    protected Boolean beforeWrite(final InvarContext c)
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
        exportFile("InvarNum.java", "invar", "InvarNum.java");
        exportFile("InvarReadData.java", "invar", "InvarReadData.java");
        return true;
    }

    @Override
    protected String codeStruct(TypeStruct type)
    {
        List<InvarField> fs = type.listFields();
        int widthType = 1;
        int widthKey = 1;
        int widthDefault = 1;
        for (InvarField f : fs)
        {
            f.setWidthTypeMax(35);
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
        for (InvarField f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthKey);
            f.setWidthDefault(widthDefault);
            fields.append(codeStructField(f, type));
            setters.append(codeStructSetter(f, type.getName()));
            getters.append(codeStructGetter(f));

            String numAnotation = "invar.InvarNum";
            TypeID id = f.getType().getId();
            if (!imps.contains(numAnotation) && //
            TypeID.UINT8 == id || TypeID.UINT16 == id || TypeID.UINT32 == id)
            {
                imps.add(numAnotation);
            }
            String key = "";
            key = importTypeCheck(f.getType(), type.getPack());
            imps.add(key);
            for (InvarType typeGene : f.getGenerics())
            {
                key = importTypeCheck(typeGene, type.getPack());
                imps.add(key);
            }
        }
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(br);
        body.append(setters);
        body.append(br);
        body.append(getters);
        return codeClassFile(type, body, codeStructImports(imps));
    }

    private String importTypeCheck(InvarType type, InvarPackage packCurr)
    {
        String key = "";
        if (type.getPack() != packCurr)
        {
            type = type.getRedirect() == null ? type : type.getRedirect();
            key = type.getPack().getName() + "." + type.getName();
        }
        return key;
    }

    private StringBuilder codeStructImports(TreeSet<String> keys)
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

    private StringBuilder codeStructField(InvarField f, TypeStruct s)
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

    private String codeStructSetter(InvarField f, String nameType)
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
        String max = null;
        if (TypeID.UINT8 == f.getType().getId())
            max = "255";
        else if (TypeID.UINT16 == f.getType().getId())
            max = "65535";
        else if (TypeID.UINT32 == f.getType().getId())
            max = "4294967295L";
        if (max != null)
        {
            code.append(brIndent);
            code.append("@InvarNum (min = 0, max = " + max + ")");
        }

        String key = upperHeadChar(f.getKey());
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

    private Object codeStructGetter(InvarField f)
    {
        StringBuilder code = new StringBuilder();
        if (!f.getComment().equals(""))
        {
            code.append(brIndent);
            code.append("/** " + f.getComment() + " */");
        }
        code.append(brIndent);
        code.append("public ");
        code.append(fixedLen(f.getWidthType(), f.getTypeFormatted()));
        code.append(" get");
        code.append(fixedLen(f.getWidthKey() + 3, upperHeadChar(f.getKey()) + "() "));
        code.append("{");
        code.append("return this." + f.getKey() + ";");
        code.append("}");
        return code;
    }

    @Override
    protected String codeEnum(TypeEnum type)
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
                               "return \"" + tName + "(\" + value + \")\";"));
        code.append(brIndent);
        code.append("public final Integer value;");
        code.append(brIndent);

        StringBuilder imports = new StringBuilder();
        imports.append(br);
        imports.append(br);
        imports.append("import java.util.HashMap;");
        return codeClassFile(type, code, imports);
    }

    private StringBuilder codeMethod(String head, String body)
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
    protected String codeRuntime(InvarType typeWrapper)
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
        InvarType rootType = null;
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
            if (alias.equals("root"))
                rootType = type;
        }
        meBasic.append(brIndent2 + "return map;");
        meEnums.append(brIndent2 + "return map;");
        meStruct.append(brIndent2 + "return map;");

        StringBuilder meMain = new StringBuilder();
        StringBuilder meStart = new StringBuilder();

        String hMain = "static public void main(String[] args) throws Exception";
        String hStart = "static public void start(Object o, String dir, String suffix, Boolean verbose) throws Exception";
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
        meMain.append(rootType != null
            ? brIndent2 + "start(new " + rootType.getName() + "(), path, suffix, true);"
            : brIndent2 + "System.out.println(\"Please define a struct with alias 'root'.\");");

        meStart.append("InvarReadData.verbose = verbose;");
        meStart.append(brIndent2 + "InvarReadData.aliasBasics = aliasBasic();");
        meStart.append(brIndent2 + "InvarReadData.aliasEnums = aliasEnums();");
        meStart.append(brIndent2 + "InvarReadData.aliasStructs = aliasStructs();");
        meStart.append(brIndent2 + "InvarReadData.start(o, dir, suffix);");

        StringBuilder body = new StringBuilder();
        body.append(codeMethod(hMain, meMain.toString()));
        body.append(br);
        body.append(codeMethod(hStart, meStart.toString()));
        body.append(br);
        body.append(codeMethod(hBasic, meBasic.toString()));
        body.append(br);
        body.append(codeMethod(hEnum, meEnums.toString()));
        body.append(br);
        body.append(codeMethod(hStruct, meStruct.toString()));
        imps.add("java.util.List");

        return codeClassFile(typeWrapper, body, codeStructImports(imps));
    }

    private String codeClassFile(InvarType type, StringBuilder body, StringBuilder imports)
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

    private String evalFieldDefault(InvarField f)
    {
        String deft = "";
        String deftF = f.getDefault();
        switch (f.getType().getId()){
        case UINT64:
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
        case INT64:
            deft = deftF.equals("") ? "-1L" : deftF + "L";
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