package invar;

import invar.io.WriteOutputCode;
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

final public class InvarWriteJava extends WriteOutputCode
{
    final static private String indent    = "    ";
    final static private String br        = "\n";
    final static private String brIndent  = br + indent;
    final static private String brIndent2 = br + indent + indent;

    @Override
    protected Boolean beforeWrite(final InvarContext c)
    {
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
        c.typeRedefine(TypeID.GHOST, "java.lang", "Throwable", "");
        c.typeRedefine(TypeID.GHOST, "java.nio", "ByteBuffer", "");
        //log(c.dumpTypeAll().toString());
        return true;
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
            f.makeTypeFormatted(getContext());
            structDefaults(f);
            if (f.getTypeFormatted().length() > widthType)
                widthType = f.getTypeFormatted().length();
            if (f.getKey().length() > widthKey)
                widthKey = f.getKey().length();
            if (f.getDefault().length() > widthDefault)
                widthDefault = f.getDefault().length();
        }

        StringBuilder imports = codeStructImports2(fs, type.getPack());

        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();
        for (InvarField<InvarType> f : fs)
        {
            f.setWidthType(widthType);
            f.setWidthKey(widthKey);
            f.setWidthDefault(widthDefault);

            fields.append(codeStructField(f));
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
            type = getContext().typeRedirect(type);
            key = type.getPack().getName() + "." + type.getName();
        }
        return key;
    }

    private StringBuilder codeStructImports2(List<InvarField<InvarType>> fs, InvarPackage pack)
    {
        SortedSet<String> keys = new TreeSet<String>();
        for (InvarField<InvarType> f : fs)
        {
            String key = "";
            key = importTypeCheck(f.getType(), pack);
            if (keys.contains(key))
                continue;
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
        code.append(" get" + upperHeadChar(f.getKey()));
        code.append("()");
        code.append("{");
        code.append("return this." + f.getKey() + ";");
        code.append("}");
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
        code.append("{");
        code.append("this." + f.getKey());
        code.append(" = value;");
        code.append(" return this;");
        code.append("}");
        return code.toString();
    }

    private StringBuilder codeStructField(InvarField<InvarType> f)
    {
        StringBuilder code = new StringBuilder();
        code.append(brIndent);
        code.append(fixedLen(f.getWidthType() + 1, f.getTypeFormatted() + " "));
        code.append(fixedLen(f.getWidthKey(), f.getKey()));
        code.append(" = ");
        code.append(fixedLen(f.getWidthDefault() + 1, f.getDefault() + ";"));
        if (!f.getComment().equals(""))
        {
            code.append("// ");
            code.append(f.getComment());
        }
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
        code.append("// THIS FILE IS GENERATED BY TOOLS. DO NOT EDIT !!!");
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

    private void structDefaults(InvarField<?> f)
    {
        String deft = f.getDefault();
        switch (f.getType().getId()){
        case INT8:
        case INT16:
        case INT32:
            if (f.getDefault().equals(""))
                f.setDefault("-1");
            break;
        case UINT8:
        case UINT16:
            if (f.getDefault().equals(""))
                f.setDefault("0");
            break;
        case UINT32:
        case INT64:
            if (f.getDefault().equals(""))
                f.setDefault("-1L");
            else
                f.setDefault(f.getDefault() + "L");
            break;
        case FLOAT:
            if (f.getDefault().equals(""))
                f.setDefault("0.00F");
            else
                f.setDefault(f.getDefault() + "F");
            break;
        case DOUBLE:
            if (f.getDefault().equals(""))
                f.setDefault("0.00");
            break;
        case UINT64:
        case STRING:
            f.setDefault(deft.equals("") ? "\"\"" : "\"" + f.getDefault()
                    + "\"");
            break;
        case BOOL:
            boolean bool = Boolean.parseBoolean(deft);
            f.setDefault(bool ? "true" : "false");
            break;
        case ENUM:
            @SuppressWarnings ("unchecked") Iterator<String> i = ((InvarField<TypeEnum>)f)
                    .getType().getKeys().iterator();
            if (i.hasNext())
                f.setDefault(f.getType().getName() + "." + i.next());
            else
                f.setDefault("new " + f.getType().getName() + "(-999999)");
            break;
        default:
            f.setDefault("new " + f.getTypeFormatted() + "()");

        }
    }

}
