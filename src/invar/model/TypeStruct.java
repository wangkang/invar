package invar.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class TypeStruct extends InvarType
{
    private InvarType                  superType;
    private HashMap<String,InvarField> fields;
    private String                     charset;
    private String                     alias;
    private String                     shortField;

    public TypeStruct(String name, InvarPackage pack, String comment)
    {
        super(TypeID.STRUCT, name, pack, comment, false);
        fields = new LinkedHashMap<String,InvarField>();
        setCharset("UTF-8");
        setAlias("");
    }

    public List<InvarField> listFields ()
    {
        List<InvarField> list = new ArrayList<InvarField>();
        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            list.add(fields.get(key));
        }
        return list;
    }

    public int maxLenKeys ()
    {
        int len = 1;
        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            if (key.length() > len)
                len = key.length();
        }
        return len;
    }

    public TypeStruct addField (InvarField f) throws Exception
    {
        checkKey(f.getKey());
        if (shortField != null)
            f.setShortName(shortField + fields.size());
        fields.put(f.getKey(), f);
        return this;
    }

    public Set<String> getKeys ()
    {
        return fields.keySet();
    }

    public InvarField getField (String key)
    {
        return fields.get(key);
    }

    public TypeID getFieldType (String key)
    {
        return fields.get(key).getType().getId();
    }

    private void checkKey (String key) throws Exception
    {
        if (fields.containsKey(key))
        {
            throw new Exception("Repeated key '" + key + //
            "' in struct '" + getName() + "'.");
        }
    }

    public String getCharset ()
    {
        return charset;
    }

    public TypeStruct setCharset (String charset)
    {
        this.charset = charset;
        return this;
    }

    public String getAlias ()
    {
        return alias;
    }

    public void setAlias (String alias)
    {
        this.alias = alias;
    }

    public InvarType getSuperType ()
    {
        return superType;
    }

    public void setSuperType (InvarType superType)
    {
        this.superType = superType;
    }

    public String getShortField ()
    {
        return shortField;
    }

    public void setShortField (String shortField)
    {
        this.shortField = shortField;
    }

}
