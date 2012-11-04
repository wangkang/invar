package invar.model;

import invar.model.InvarType.TypeID;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class InvarPackage
{
    private final String              name;
    private final Boolean             needWrite;
    //
    private File                      codeDir;
    private HashMap<String,InvarType> typeMap;

    public InvarPackage(String name, Boolean needWrite, int capacity)
    {
        this.name = name;
        this.needWrite = needWrite;
        this.typeMap = new HashMap<String,InvarType>(capacity);
    }

    public void put(InvarType t)
    {
        typeMap.put(t.getName(), t);
    }

    public InvarType getType(String name)
    {
        return typeMap.get(name);
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarType> T findType(String typeName) throws Throwable
    {
        if (!typeMap.containsKey(typeName))
        {
            throw new Error("No type " + "named '" + typeName
                    + "' in package '" + name + "'");
        }
        InvarType type = typeMap.get(typeName);
        return (T)type;
    }

    public InvarType getType(TypeID id)
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

    public Set<String> getTypeNames()
    {
        return typeMap.keySet();
    }

    public InvarPackage add(InvarType t) throws Exception
    {
        checkTypeName(t.getName());
        typeMap.put(t.getName(), t);
        return this;
    }

    public InvarPackage add(TypeEnum t) throws Exception
    {
        checkTypeName(t.getName());
        typeMap.put(t.getName(), t);
        return this;
    }

    public InvarPackage add(TypeStruct t) throws Exception
    {
        checkTypeName(t.getName());
        typeMap.put(t.getName(), t);
        return this;
    }

    public InvarPackage add(TypeProtocol t) throws Exception
    {
        checkTypeName(t.getName());
        typeMap.put(t.getName(), t);
        return this;
    }

    public String getName()
    {
        return name;
    }

    public void checkTypeName(String typeName) throws Exception
    {
        if (typeMap.containsKey(typeName))
        {
            throw new Exception("Repeated type name '" + typeName
                    + "' in package '" + name + "'.");
        }
    }

    public File getCodeDir()
    {
        return codeDir;
    }

    public void setCodeDir(File codeDir)
    {
        this.codeDir = codeDir;
    }

    public Boolean getNeedWrite()
    {
        return needWrite;
    }

}
