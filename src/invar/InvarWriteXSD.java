package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

public class InvarWriteXSD
{
    private String                 nsKey   = "tns";
    private String                 nsValue = "invar.data";
    private InvarContext           context;
    private TreeMap<TypeID,String> typeXsd;
    private TreeMap<String,TypeID> typeBasic;

    public InvarWriteXSD()
    {
        TreeMap<TypeID,String> map = new TreeMap<TypeID,String>();
        map.put(TypeID.INT08, "xs:byte");
        map.put(TypeID.INT16, "xs:short");
        map.put(TypeID.INT32, "xs:int");
        map.put(TypeID.INT64, "xs:long");
        map.put(TypeID.UINT08, "xs:unsignedByte");
        map.put(TypeID.UINT16, "xs:unsignedShort");
        map.put(TypeID.UINT32, "xs:unsignedInt");
        map.put(TypeID.UINT64, "xs:unsignedLong");
        map.put(TypeID.FLOAT, "xs:float");
        map.put(TypeID.DOUBLE, "xs:double");
        map.put(TypeID.BOOL, "xs:boolean");
        map.put(TypeID.STRING, "xs:string");
        typeXsd = map;
        typeBasic = new TreeMap<String,TypeID>();
    }

    public void write (InvarContext c, TreeMap<TypeID,String> basics, String dirRootPath) throws IOException
    {
        this.context = c;
        StringBuilder code = new StringBuilder();
        code.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        code.append(br + "<schema xmlns=\"http://www.w3.org/2001/XMLSchema\"");
        code.append(" " + "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"");
        code.append(br + "xmlns:" + nsKey + "=\"" + nsValue + "\"");
        code.append(" " + "targetNamespace=\"" + nsValue + "\"");
        code.append(">");

        code.append(br);
        TypeStruct root = c.getStructRoot();
        if (root != null)
        {
            code.append(br);
            code.append("<xs:element ");
            code.append("name=\"" + root.getAlias() + "\" ");
            code.append("type=\"" + nsKey + ":" + root.fullName(TYPE_SPLIT) + "\" ");
            code.append("/>");
        }

        code.append(br);
        codeBasics(basics, code);

        code.append(br);
        Iterator<String> i = getContext().getPackNames();
        while (i.hasNext())
        {
            InvarPackage pack = getContext().getPack(i.next());
            Iterator<String> iTypeName = pack.getTypeNames();
            while (iTypeName.hasNext())
            {
                String name = iTypeName.next();
                InvarType type = pack.getType(name);
                if (TypeID.STRUCT == type.getId())
                {
                    codeStruct((TypeStruct)type, code);
                }
                else if (TypeID.ENUM == type.getId())
                {
                    codeEnum((TypeEnum)type, code);
                }
                else
                {
                }
            }
        }
        code.append(br + "</schema>");

        File dir = new File(dirRootPath);
        if (!dir.exists())
            dir.mkdirs();
        if (dir.exists())
        {
            File file = new File(dir, "data.xsd");
            FileWriter writer = new FileWriter(file, false);
            String content = code.toString();
            writer.write(content);
            writer.close();
            System.out.println("write -> " + file.getAbsolutePath());

        }
    }

    private InvarContext getContext ()
    {
        return context;
    }

    private void codeStruct (TypeStruct type, StringBuilder code)
    {
        code.append(br);
        code.append("<xs:complexType name=\"" + type.fullName(".") + "\">");
        code.append(brIndent);
        code.append("<xs:sequence>");
        Iterator<String> i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            String rule = type.getField(key).createAliasRule(getContext(), ".");
            code.append(codeByRule(key, rule, brIndent2, 0, 2048, null));
        }
        code.append(brIndent);
        code.append("</xs:sequence>");

        i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            InvarField f = type.getField(key);
            codeStructAttr(f, code);
        }
        code.append(br);
        code.append("</xs:complexType>");
    }

    private StringBuilder codeByRule (String key,
                                      String rule,
                                      String indent,
                                      Integer minOccurs,
                                      Integer maxOccurs,
                                      StringBuilder ext)
    {
        StringBuilder code = new StringBuilder();
        String strL = ruleLeft(rule);
        String strR = ruleRight(rule);
        code.append(indent);
        code.append("<xs:element ");
        code.append("name=\"" + simplifyTypeName(key) + "\" ");
        code.append("minOccurs=\"" + minOccurs + "\" ");
        code.append("maxOccurs=\"" + maxOccurs + "\" ");
        if (strL.equals("vec"))
        {
            code.append(">");
            code.append(brIndent3);
            code.append("<xs:complexType><xs:sequence>");
            String strRL = ruleLeft(strR);
            code.append(codeByRule(strRL, strR, brIndent3, 0, 2048, null));
            code.append(brIndent3);
            code.append("</xs:sequence>");
            if (ext != null)
                code.append(ext);
            code.append("</xs:complexType>");
            code.append(indent);
            code.append("</xs:element>");
        }
        else if (strL.equals("map"))
        {
            code.append(">");
            code.append(brIndent3);
            code.append("<xs:complexType>");
            String[] R = strR.split(GENERIC_SPLIT);
            String k = ruleLeft(R[0]);
            String v = ruleLeft(R[1]);
            TypeID id = typeBasic.get(k);
            Boolean isSimpleKey = (id != null);
            InvarType kType = findType(getContext(), k);
            if (id == null)
            {
                if (kType != null)
                {
                    id = kType.getId();
                    isSimpleKey = (id == TypeID.ENUM);
                    //System.out.println("InvarWriteXSD.codeByRule()" + k);
                }
            }
            if (!isSimpleKey)
            {
                code.append(brIndent3);
                code.append("<xs:sequence minOccurs=\"0\" maxOccurs=\"2048\">");
                code.append(codeByRule("k-" + simplifyTypeName(k), R[0], brIndent3, 1, 1, null));
                code.append(codeByRule("v-" + simplifyTypeName(v), R[1], brIndent3, 1, 1, null));
                code.append(brIndent3);
                code.append("</xs:sequence>");
            }
            else
            {
                code.append(brIndent3);
                code.append("<xs:sequence>");
                StringBuilder codeExt = null;
                TypeStruct ts = null;
                InvarType vType = findType(getContext(), v);
                if (vType instanceof TypeStruct)
                    ts = (TypeStruct)vType;
                InvarField f = ts == null ? null : ts.getField(ATTR_MAP_KEY);
                if (f == null)
                {
                    String typeName = id == TypeID.ENUM//
                        ? nsKey + ":" + kType.fullName(TYPE_SPLIT) + "Attr"
                        : typeXsd.get(id);
                    codeExt = new StringBuilder();
                    codeExt.append(brIndent3);
                    codeExt.append("<xs:attribute name=\"" + ATTR_MAP_KEY + "\" ");
                    codeExt.append("type=\"" + typeName + "\" ");
                    codeExt.append("use=\"required\" ");
                    codeExt.append("/>");
                }
                code.append(codeByRule(simplifyTypeName(v), R[1], brIndent3, 0, 2048, codeExt));
                code.append(brIndent3);
                code.append("</xs:sequence>");
            }
            if (ext != null)
                code.append(ext);
            code.append("</xs:complexType>");
            code.append(indent);
            code.append("</xs:element>");
        }
        else
        {
            String elemType = nsKey + ":" + strL;
            if (ext == null)
            {
                code.append("type=\"" + elemType + "\" ");
                code.append("/>");
            }
            else
            {
                code.append(">");
                code.append(brIndent3);
                code.append("<xs:complexType><xs:complexContent>");
                code.append(brIndent3);
                code.append("<xs:extension base=\"" + elemType + "\">");
                code.append(ext);
                code.append(brIndent3);
                code.append("</xs:extension></xs:complexContent></xs:complexType>");
                code.append(indent);
                code.append("</xs:element>");
            }
        }
        return code;
    }

    private InvarType findType (InvarContext ctx, String fullName)
    {
        int iEnd = fullName.lastIndexOf(TYPE_SPLIT);
        if (iEnd < 0)
            return null;
        String packName = fullName.substring(0, iEnd);
        String typeName = fullName.substring(iEnd + 1);
        InvarPackage pack = ctx.getPack(packName);
        if (pack == null)
            return null;
        return pack.getType(typeName);
    }

    private void codeStructAttr (InvarField f, StringBuilder code)
    {
        TypeID id = f.getType().getId();
        String tXSD = typeXsd.get(id);
        if (tXSD != null)
        {
            code.append(brIndent);
            code.append("<xs:attribute ");
            code.append("name=\"" + f.getKey() + "\" ");
            code.append("type=\"" + tXSD + "\" ");
            code.append("/>");
        }
        else if (TypeID.ENUM == id)
        {
            String tEnum = nsKey + ":" + f.getType().fullName(TYPE_SPLIT) + "Attr";
            code.append(brIndent);
            code.append("<xs:attribute ");
            code.append("name=\"" + f.getKey() + "\" ");
            code.append("type=\"" + tEnum + "\" ");
            code.append("/>");
        }
        else
        {
        }
    }

    private void codeBasics (TreeMap<TypeID,String> basics, StringBuilder code)
    {
        Iterator<TypeID> i = typeXsd.keySet().iterator();
        while (i.hasNext())
        {
            TypeID id = i.next();
            String name = basics.get(id);
            String nameXsd = typeXsd.get(id);
            code.append(br);
            code.append("<xs:complexType name=\"" + name + "\">");
            //code.append(brIndent);
            code.append("<xs:attribute type=\"" + nameXsd + "\" name=\"value\" use=\"required\"" + "/>");
            //code.append(br);
            code.append("</xs:complexType>");
            typeBasic.put(name, id);
        }
    }

    private void codeEnum (TypeEnum type, StringBuilder code)
    {
        String simpleName = type.fullName(".") + "Attr";
        code.append(br);
        code.append("<xs:complexType name=\"" + type.fullName(".") + "\">");
        code.append("<xs:attribute ");
        code.append("type=\"" + nsKey + ":" + simpleName + "\" ");
        code.append("name=\"" + "value" + "\" ");
        code.append("use=\"" + "required" + "\" ");
        code.append("/>");
        code.append("</xs:complexType>");
        code.append(br);
        code.append("<xs:simpleType name=\"" + simpleName + "\"");
        code.append(">");
        code.append(brIndent);
        code.append("<xs:restriction base=\"xs:string\">");
        Iterator<String> i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            Integer value = type.getValue(key);
            code.append(brIndent2);
            code.append("<!-- ");
            code.append(value);
            //code.append(key + ": " + type.getComment(key));
            code.append(" -->");
            //code.append(brIndent2);
            code.append("<xs:enumeration value=\"" + key + "\" />");
        }
        code.append(brIndent);
        code.append("</xs:restriction>");
        code.append("</xs:simpleType>");
    }

    final static private String indent        = "  ";
    final static private String br            = "\n";
    final static private String brIndent      = br + indent;
    final static private String brIndent2     = br + indent + indent;
    final static private String brIndent3     = br + indent + indent + indent;
    final static private String GENERIC_LEFT  = "<";
    final static private String GENERIC_RIGHT = ">";
    final static private String GENERIC_SPLIT = ",";
    final static private String TYPE_SPLIT    = ".";
    final static private String ATTR_MAP_KEY  = "key";

    static private String simplifyTypeName (String key)
    {
        int iBegin = key.lastIndexOf(TYPE_SPLIT) + 1;
        if (iBegin >= 0)
            return key.substring(iBegin, key.length());
        else
            return key;
    }

    static private String ruleLeft (String rule)
    {
        String name = rule;
        if (rule.indexOf(GENERIC_LEFT) >= 0)
        {
            name = rule.substring(0, rule.indexOf(GENERIC_LEFT));
        }
        return name;
    }

    static private String ruleRight (String rule)
    {
        int iBegin = rule.indexOf(GENERIC_LEFT) + 1;
        int iEnd = rule.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            return rule.substring(iBegin, iEnd);
        }
        return null;
    }

}
