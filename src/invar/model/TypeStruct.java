package invar.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class TypeStruct extends InvarType
{
    private LinkedHashMap<String,InvarField<InvarType>> fields;

    public TypeStruct(String name, InvarPackage pack, String comment)
    {
        super(TypeID.STRUCT, name, pack, comment);
        fields = new LinkedHashMap<String,InvarField<InvarType>>();
    }

    public List<InvarField<InvarType>> listFields()
    {
        List<InvarField<InvarType>> list = new ArrayList<InvarField<InvarType>>();
        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            list.add(fields.get(key));
        }
        return list;
    }

    public int maxLenKeys()
    {
        int len = 1;
        Iterator<String> i = fields.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            //TypeID id = getFieldType(key);
            if (key.length() > len)
            {
                len = key.length();
            }
        }
        return len;
    }

    @SuppressWarnings ("unchecked")
    public TypeStruct addField(InvarField<? extends InvarType> f) throws Exception
    {
        checkKey(f.getKey());
        fields.put(f.getKey(), (InvarField<InvarType>)f);
        return this;
    }

    public Set<String> getKeys()
    {
        return fields.keySet();
    }

    public InvarField<InvarType> getField(String key)
    {
        return fields.get(key);
    }

    public TypeID getFieldType(String key)
    {
        return fields.get(key).getType().getId();
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarField<InvarType>> T getFieldCorect(String key)
    {
        return (T)fields.get(key);
    }

    private void checkKey(String key) throws Exception
    {
        if (fields.containsKey(key))
        {
            throw new Exception("Repeated key '" + key + "' in struct '"
                    + getName() + "'.");
        }
    }

}