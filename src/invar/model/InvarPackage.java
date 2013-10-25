package invar.model;

import invar.model.InvarType.TypeID;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class InvarPackage
{
    private final String              name;
    private final Boolean             needWrite;
    private File                      codeDir;
    private HashMap<String,InvarType> typeMap;

    public InvarPackage(String name, Boolean needWrite)
    {
        this.name = name;
        this.needWrite = needWrite;
        this.typeMap = new HashMap<String,InvarType>();
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
            if (type.getId() == TypeID.GHOST)
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

    public String getName ()
    {
        return name;
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
