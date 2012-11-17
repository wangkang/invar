package invar.model;

import java.util.LinkedHashMap;
import java.util.Set;

public class TypeEnum extends InvarType
{
    private String                        alias;
    private LinkedHashMap<String,Integer> options;
    private LinkedHashMap<String,String>  optionComments;

    public TypeEnum(String name, InvarPackage pack, String comment)
    {
        super(TypeID.ENUM, name, pack, comment);
        options = new LinkedHashMap<String,Integer>();
        optionComments = new LinkedHashMap<String,String>();
    }

    public TypeEnum addOption(String key, Integer value, String comment) throws Exception
    {
        if (options.containsKey(key))
        {
            throw new Exception("Repeated key '" + key + //
            "' in enum '" + getName() + "'.");
        }
        if (options.containsValue(value))
        {
            throw new Exception("Repeated value '" + value + //
            "' in enum '" + getName() + "'.");
        }
        options.put(key, value);
        optionComments.put(key, comment);
        return this;
    }

    public Set<String> getKeys()
    {
        return options.keySet();
    }

    public String getComment(String key)
    {
        return optionComments.get(key);
    }

    public Integer getValue(String key)
    {
        return options.get(key);
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }
}