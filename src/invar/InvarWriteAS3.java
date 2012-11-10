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

final public class InvarWriteAS3 extends InvarWrite
{
    public InvarWriteAS3(InvarContext context, String dirRootPath)
    {
        super(context, dirRootPath);
    }

    final static private String indent   = "    ";
    final static private String br       = "\n";
    final static private String brIndent = br + indent;

    @Override
    protected Boolean beforeWrite(final InvarContext c)
    {
        c.ghostClear();
        c.typeRedefine(TypeID.INT8, "", "int", "");
        c.typeRedefine(TypeID.INT16, "", "int", "");
        c.typeRedefine(TypeID.INT32, "", "int", "");
        c.typeRedefine(TypeID.INT64, "", "String", "");
        c.typeRedefine(TypeID.UINT8, "", "uint", "");
        c.typeRedefine(TypeID.UINT16, "", "uint", "");
        c.typeRedefine(TypeID.UINT32, "", "uint", "");
        c.typeRedefine(TypeID.UINT64, "", "String", "");
        c.typeRedefine(TypeID.FLOAT, "", "Number", "");
        c.typeRedefine(TypeID.DOUBLE, "", "Number", "");
        c.typeRedefine(TypeID.STRING, "", "String", "");
        c.typeRedefine(TypeID.BOOL, "", "Boolean", "");
        c.typeRedefine(TypeID.LIST, "", "Vector", ".<?>");
        c.typeRedefine(TypeID.MAP, "flash.utils", "Dictionary", "");
        //System.out.println(c.dumpTypeAll());
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
            f.setWidthTypeMax(20);
            f.makeTypeFormatted(getContext());
            if (f.getTypeFormatted().length() > widthType)
                widthType = f.getTypeFormatted().length();
            if (f.getKey().length() > widthKey)
                widthKey = f.getKey().length();
            if (f.getDefault().length() > widthDefault)
                widthDefault = f.getDefault().length();
        }
        StringBuilder imports = codeStructImports(fs, type.getPack());
        StringBuilder fields = new StringBuilder();
        StringBuilder setters = new StringBuilder();
        StringBuilder getters = new StringBuilder();
        for (InvarField f : fs)
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
            type = type.getRedirect() == null ? type : type.getRedirect();
            if (!type.getPack().getName().equals(""))
                key = type.getPack().getName() + "." + type.getName();
        }
        return key;
    }

    private StringBuilder codeStructImports(List<InvarField> fs, InvarPackage pack)
    {
        SortedSet<String> keys = new TreeSet<String>();
        for (InvarField f : fs)
        {
            String key = "";
            key = importTypeCheck(f.getType(), pack);
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

    private Object codeStructGetter(InvarField f)
    {
        StringBuilder code = new StringBuilder();
        if (!f.getComment().equals(""))
        {
            code.append(brIndent);
            code.append("/** " + f.getComment() + " */");
        }
        code.append(brIndent);
        code.append("public function ");
        code.append("get");
        code.append(fixedLen(f.getWidthKey() + 2, upperHeadChar(f.getKey()) + "()"));
        code.append(fixedLen(f.getWidthType() + 2, " :" + f.getTypeFormatted()));
        code.append(" {");
        code.append("return this." + f.getKey() + ";");
        code.append("}");
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
        String key = upperHeadChar(f.getKey());
        code.append(brIndent);
        code.append("public function ");
        code.append("set");
        code.append(fixedLen(f.getWidthKey(), key));
        code.append("(value:");
        code.append(f.getTypeFormatted() + ")");
        code.append(":" + nameType);
        code.append(" {");
        code.append("this." + f.getKey());
        code.append(" = value;");
        code.append(" return this;");
        code.append("}");
        return code.toString();
    }

    private StringBuilder codeStructField(InvarField f)
    {
        StringBuilder code = new StringBuilder();
        code.append(brIndent);
        code.append("private var ");
        code.append(fixedLen(f.getWidthKey(), f.getKey()));
        code.append(" :");
        code.append(fixedLen(f.getWidthType() + 1, f.getTypeFormatted()));
        code.append(" = ");
        String deft = evalFieldDefault(f);
        code.append(fixedLen(f.getWidthDefault() + 1, deft + ";"));
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
            code.append("public static var ");
            code.append(fixedLen(lenKey, key) + ":");
            code.append(type.getName());
            code.append(" = new " + type.getName() + "(");
            code.append(type.getValue(key) + ");");
            codePuts.append(brIndent);
            codePuts.append("map[" + type.getValue(key) + "] = " + key + ";");
        }
        code.append(br);
        code.append(brIndent);
        code.append("public static function isValid(value:int):Boolean {return map[value];}");
        code.append(brIndent);
        code.append("public static function convert(value:int):" + type.getName() + " {return map[value];}");
        code.append(br);
        code.append(brIndent);
        code.append("public function getValue():int {return this.value;}");
        code.append(brIndent);
        code.append("public function " + type.getName() + "(value:int) {this.value = value;}");
        code.append(br);
        code.append(brIndent);
        code.append("private var value:int;");
        code.append(brIndent);
        code.append("private static var map:Dictionary = new Dictionary();");
        code.append(codePuts);
        StringBuilder imports = new StringBuilder();
        imports.append(br);
        imports.append(br);
        imports.append("import flash.utils.Dictionary;");
        return codeClassFile(type, code, imports);
    }

    private String codeClassFile(InvarType type, StringBuilder body, StringBuilder imports)
    {
        StringBuilder code = new StringBuilder();
        code.append("// THIS FILE IS GENERATED BY INVAR. DO NOT EDIT !!!");
        code.append(br);
        code.append("package " + type.getPack().getName() + " {");
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
        code.append("}");
        return code.toString();
    }

    private String evalFieldDefault(InvarField f)
    {
        String deft = null;
        switch (f.getType().getId()){
        case INT64:
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
        case UINT32:
            deft = f.getDefault().equals("") ? "0" : f.getDefault();
            break;
        case FLOAT:
        case DOUBLE:
            deft = f.getDefault().equals("") ? "0.00" : f.getDefault();
            break;
        case BOOL:
            boolean bool = Boolean.parseBoolean(f.getDefault());
            deft = bool ? "true" : "false";
            break;
        case ENUM:
            Iterator<String> i = ((TypeEnum)f.getType()).getKeys().iterator();
            deft = i.hasNext()
                ? f.getType().getName() + "." + i.next()
                : "new " + f.getType().getName() + "(-999999)";
            break;
        default:
            deft = ("new " + f.getTypeFormatted() + "()");
        }
        return deft;
    }

    @Override
    protected String codeRuntime(InvarType type)
    {
        // TODO Auto-generated method stub
        return null;
    }
}