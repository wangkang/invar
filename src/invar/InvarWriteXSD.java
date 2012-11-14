package invar;

import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeStruct;
import java.util.Iterator;
import java.util.TreeMap;

public class InvarWriteXSD
{
    final static private String    indent     = "    ";
    final static private String    br         = "\n";
    final static private String    brIndent   = br + indent;
    final static private String    brIndent2  = br + indent + indent;

    private InvarContext           context;
    private String                 typePrefix = "invar.";
    private TreeMap<TypeID,String> typeXsd;

    public InvarWriteXSD()
    {
        TreeMap<TypeID,String> map = new TreeMap<TypeID,String>();
        map.put(TypeID.INT8, "xs:byte");
        map.put(TypeID.INT16, "xs:short");
        map.put(TypeID.INT32, "xs:int");
        map.put(TypeID.INT64, "xs:long");
        map.put(TypeID.UINT8, "xs:unsignedByte");
        map.put(TypeID.UINT16, "xs:unsignedShort");
        map.put(TypeID.UINT32, "xs:unsignedInt");
        map.put(TypeID.UINT64, "xs:unsignedLong");
        map.put(TypeID.FLOAT, "xs:float");
        map.put(TypeID.DOUBLE, "xs:double");
        map.put(TypeID.BOOL, "xs:boolean");
        map.put(TypeID.STRING, "xs:string");
        typeXsd = map;
    }

    public void write(InvarContext context, TreeMap<TypeID,String> basics)
    {
        this.context = context;
        StringBuilder code = new StringBuilder();

        codeBasics(basics, code);

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
                }
                else
                {
                }
            }
        }
        System.out.println(code);
    }

    private void codeStruct(TypeStruct type, StringBuilder code)
    {
        code.append(br);
        code.append("<xs:complexType name=\"" + type.fullName(".") + "\">");
        code.append(brIndent);
        code.append("<xs:all>");
        Iterator<String> i = type.getKeys().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            InvarField f = type.getField(key);
            codeStructElement(f, code);
        }
        code.append(brIndent);
        code.append("</xs:all>");

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

    private void codeStructElement(InvarField f, StringBuilder code)
    {
        TypeID id = f.getType().getId();
        String tXSD = typeXsd.get(id);
        String tInvar = getContext().findTypes(f.getType().getName())
                                    .get(0)
                                    .fullName(".");
        if (tXSD != null && tInvar != null)
        {
            code.append(brIndent2);
            code.append("<xs:element ");
            code.append("name=\"" + f.getKey() + "\" ");
            code.append("type=\"" + typePrefix + tInvar + "\" ");
            code.append("minOccurs=\"" + "0" + "\" ");
            //code.append("maxOccurs=\"" + "1" + "\" ");
            code.append("/>");
            return;
        }
        if (tInvar != null)
        {
            code.append(brIndent2);
            code.append("<xs:element ");
            code.append("name=\"" + f.getKey() + "\" ");
            code.append("minOccurs=\"" + "0" + "\" ");
            //code.append("maxOccurs=\"" + "1" + "\" ");
            code.append("/>");
        }
    }

    private void codeStructAttr(InvarField f, StringBuilder code)
    {
        TypeID id = f.getType().getId();
        String tXSD = typeXsd.get(id);
        if (tXSD != null)
        {
            code.append(brIndent);
            code.append("<xs:attribute name=\"" + f.getKey() + "\" type=\"" + tXSD + "\" />");
            return;
        }
    }

    private void codeBasics(TreeMap<TypeID,String> basics, StringBuilder code)
    {
        Iterator<TypeID> i = typeXsd.keySet().iterator();
        while (i.hasNext())
        {
            TypeID id = i.next();
            String name = basics.get(id);
            String nameXsd = typeXsd.get(id);
            codeSimple(name, nameXsd, code);
        }
    }

    private void codeSimple(String name, String xs, StringBuilder code)
    {
        code.append(br);
        code.append("<xs:complexType name=\"" + typePrefix + name + "\">");
        code.append(brIndent);
        code.append("<xs:attribute name=\"value\" type=\"" + xs + "\" use=\"required\" />");
        code.append(br);
        code.append("</xs:complexType>");
    }

    private InvarContext getContext()
    {
        return context;
    }
}
