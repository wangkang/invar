package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
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
        // System.out.println(c.dumpTypeAll());
        exportFile("InvarReadData.java", "invar", "InvarReadData.java");
        return true;
    }
    @Override
    protected String codeStructAlias(InvarType typeWrapper)
    {
        Iterator<String> i = getContext().aliasNames();

        StringBuilder imports = new StringBuilder();
        StringBuilder codeA = new StringBuilder();
        StringBuilder codeT = new StringBuilder();

        codeA.append(brIndent);
        codeA.append("static public HashMap<String,Class<?>> mapAliasType()");
        codeA.append(brIndent);
        codeA.append("{");
        codeA.append(brIndent2);
        codeA.append("HashMap<String,Class<?>> map = new HashMap<String,Class<?>>();");
        imports.append(br);
        codeT.append(brIndent);
        codeT.append("static public HashMap<Class<?>,String> mapTypeAlias()");
        codeT.append(brIndent);
        codeT.append("{");
        codeT.append(brIndent2);
        codeT.append("HashMap<Class<?>,String> map = new HashMap<Class<?>,String>();");
        while (i.hasNext())
        {
            String alias = i.next();
            InvarType type = getContext().aliasGet(alias);
            codeA.append(brIndent2);
            codeA.append("map.put(\"" + alias + "\", " + type.getName()
                    + ".class);");
            codeT.append(brIndent2);
            codeT.append("map.put(" + type.getName() + ".class" + ", \""
                    + alias + "\");");
            if (!type.getPack().getName().startsWith("java.lang"))
            {
                imports.append(br);
                imports.append("import " + type.getPack().getName() + "."
                        + type.getName() + ";");
            }
        }
        codeA.append(brIndent2);
        codeA.append("return map;");
        codeA.append(brIndent);
        codeA.append("}");
        codeT.append(brIndent2);
        codeT.append("return map;");
        codeT.append(brIndent);
        codeT.append("}");
        StringBuilder body = new StringBuilder();
        body.append(codeA);
        //body.append(br);
        //body.append(codeT);
        return codeClassFile(typeWrapper, body, imports);
    }
    @Override
    protected String codeStruct(TypeStruct type)
    {
        List<InvarField<InvarType>> fs = type.listFields();
        int widthType = 1;
        int widthKey = 1;
        int widthDefault = 1;
        for (InvarField<InvarType> f : fs)
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
        StringBuilder imports = codeStructImports(fs, type.getPack());
        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();
        for (InvarField<InvarType> f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthKey);
            f.setWidthDefault(widthDefault);

            fields.append(codeStructField(f, type));
            setters.append(codeStructSetter(f, type.getName()));
            getters.append(codeStructGetter(f));
        }
        StringBuilder body = new StringBuilder();
        body.append(fields);
        body.append(br);
        body.append(setters);
        body.append(br);
        body.append(getters);
        return codeClassFile(type, body, imports);
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

    private StringBuilder codeStructImports(List<InvarField<InvarType>> fs, InvarPackage pack)
    {
        SortedSet<String> keys = new TreeSet<String>();
        for (InvarField<InvarType> f : fs)
        {
            String key = "";
            key = importTypeCheck(f.getType(), pack);
            // System.out.println(pack.getName() + "---" + f.getKey() + "---"
            //+ key + "---" + f.getGenerics().size());
            if (!keys.contains(key))
                keys.add(key);
            for (InvarType typeGene : f.getGenerics())
            {
                key = importTypeCheck(typeGene, pack);
                if (keys.contains(key))
                    continue;
                keys.add(key);
            }
        }
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

    private StringBuilder codeStructField(InvarField<InvarType> f, TypeStruct s)
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
    private String codeStructSetter(InvarField<InvarType> f, String nameType)
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

    private Object codeStructGetter(InvarField<InvarType> f)
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
        code.append(fixedLen(f.getWidthKey() + 3, upperHeadChar(f.getKey())
                + "() "));
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
        StringBuilder codePuts = new StringBuilder();
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
            code.append("public final static " + type.getName() + " ");
            code.append(fixedLen(lenKey, key) + " = new " + type.getName()
                    + "(");
            code.append(type.getValue(key) + ");");
            codePuts.append(brIndent2);
            codePuts.append("map.put(" + type.getValue(key) + ", " + key + ");");
        }
        code.append(br);
        code.append(brIndent);
        code.append("public static HashMap<Integer," + type.getName()
                + "> map;");
        code.append(brIndent);
        code.append("{");
        code.append(brIndent2);
        code.append("map = new HashMap<Integer," + type.getName() + ">();");
        code.append(codePuts);
        code.append(brIndent);
        code.append("}");
        code.append(br);
        code.append(brIndent);
        code.append("public " + type.getName() + "(Integer value)");
        code.append(brIndent);
        code.append("{");
        code.append(brIndent2);
        code.append("this.value = value;");
        code.append(brIndent);
        code.append("}");
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

    private String evalFieldDefault(InvarField<?> f)
    {
        String deft = "";
        switch (f.getType().getId()){
        case UINT64:
        case STRING:
            deft = "\"" + f.getDefault() + "\"";
            break;
        case INT8:
        case INT16:
        case INT32:
            deft = f.getDefault().equals("") ? "-1" : f.getDefault();
            break;
        case UINT8:
        case UINT16:
            deft = f.getDefault().equals("") ? "0" : f.getDefault();
            break;
        case UINT32:
        case INT64:
            deft = f.getDefault().equals("") ? "-1L" : f.getDefault() + "L";
            break;
        case FLOAT:
            deft = f.getDefault().equals("") ? "0.00F" : f.getDefault() + "F";
            break;
        case DOUBLE:
            deft = f.getDefault().equals("") ? "0.00" : f.getDefault();
            break;
        case BOOL:
            boolean bool = Boolean.parseBoolean(f.getDefault());
            deft = bool ? "true" : "false";
            break;
        case ENUM:
            @SuppressWarnings ("unchecked") Iterator<String> i = ((InvarField<TypeEnum>)f)
                    .getType().getKeys().iterator();
            if (i.hasNext())
                deft = (f.getType().getName() + "." + i.next());
            else
                deft = ("new " + f.getType().getName() + "(-999999)");
            break;
        default:
            deft = ("new " + f.getTypeFormatted() + "()");
        }
        return deft;
    }
}