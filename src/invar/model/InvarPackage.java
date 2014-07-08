package invar.model;

import invar.model.InvarType.TypeID;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class InvarPackage
{
    private final Boolean             needWrite;
    private final String              nameReal;
    private String                    name;
    private File                      codeDir;
    private HashMap<String,InvarType> typeMap;

    public InvarPackage(String name, Boolean needWrite)
    {
        this.name = name;
        this.nameReal = name;
        this.needWrite = needWrite;
        this.typeMap = new HashMap<String,InvarType>();
    }

    public void capitalizeNameHead (Boolean bool)
    {
        if (nameReal.length() <= 0)
            return;
        if (!bool)
        {
            name = nameReal;
            return;
        }
        String[] names = nameReal.split("\\.");
        name = "";
        int len = names.length;
        for (int i = 0; i < len; i++)
        {
            String s = names[i];
            if (s.length() < 1)
                continue;
            s = s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
            name += s;
            if (i + 1 < len)
                name += ".";
        }
    }

    public String getName ()
    {
        return name;
    }

    public void put (InvarType t)
    {
        typeMap.put(t.getName(), t);
    }

    public int size ()
    {
        return typeMap.size();
    }

    public InvarType getType (String name)
    {
        return typeMap.get(name);
    }

    public InvarType getType (TypeID id)
    {
        InvarType type = null;
        Iterator<String> i = typeMap.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            type = typeMap.get(key);
            if (type.getId() == id)
                return type;
        }
        return type;
    }

    public void clearGhostTypes ()
    {
        InvarType type = null;
        Iterator<String> i = typeMap.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            type = typeMap.get(key);
            if (type.getId() == TypeID.DIALECT)
                i.remove();
        }
    }

    public Iterator<String> getTypeNames ()
    {
        return typeMap.keySet().iterator();
    }

    public InvarPackage add (InvarType t)
    {
        typeMap.put(t.getName(), t);
        return this;
    }

    public File getCodeDir ()
    {
        return codeDir;
    }

    public void setCodeDir (File codeDir)
    {
        this.codeDir = codeDir;
    }

    public Boolean getNeedWrite ()
    {
        return needWrite;
    }
}
