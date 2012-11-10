package invar;

import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

final public class InvarContext
{
    private final InvarPackage                 packBuildIn;
    private final HashMap<String,InvarPackage> packAll;
    private final HashMap<String,InvarType>    typeWithAlias;

    public InvarContext() throws Exception
    {
        typeWithAlias = new LinkedHashMap<String,InvarType>();
        packBuildIn = new InvarPackage("#INVAR#", false);
        packAll = new HashMap<String,InvarPackage>();
        packAll.put(packBuildIn.getName(), packBuildIn);
    }

    public InvarPackage addBuildInTypes(TreeMap<TypeID,String> map)
    {
        InvarPackage pack = packBuildIn;
        Set<TypeID> keys = map.keySet();
        Iterator<TypeID> i = keys.iterator();
        while (i.hasNext())
        {
            TypeID id = i.next();
            String name = map.get(id);
            InvarType type = new InvarType(id, name, pack, name + "[buildin]");
            packBuildIn.put(type);
        }
        packAll.put(pack.getName(), pack);
        return pack;
    }

    public InvarContext typeRedefine(TypeID id, String namePack, String nameType, String generic)
    {
        if (TypeID.ENUM == id || TypeID.STRUCT == id || TypeID.PROTOCOL == id)
        {
            return this;
        }
        InvarPackage pack = packAll.get(namePack);
        if (pack == null)
        {
            pack = new InvarPackage(namePack, false);
            packAll.put(namePack, pack);
        }
        InvarType type = packBuildIn.getType(id);
        InvarType typeRedi = new InvarType(id, nameType, pack, "").setGeneric(generic);
        type.setRedirect(typeRedi);
        typeWithAlias.put(type.getName(), typeRedi);
        pack.put(typeRedi);
        return this;
    }

    public InvarType ghostAdd(String namePack, String nameType, String generic)
    {
        InvarPackage pack = packAll.get(namePack);
        if (pack == null)
        {
            pack = new InvarPackage(namePack, false);
            packAll.put(namePack, pack);
        }
        InvarType t = new InvarType(TypeID.GHOST, nameType, pack, "").setGeneric(generic);
        pack.put(t);
        return t;
    }

    public void ghostClear()
    {
        Iterator<String> i = packAll.keySet().iterator();
        while (i.hasNext())
        {
            InvarPackage pack = packAll.get(i.next());
            pack.clearGhostTypes();
        }
    }

    public InvarPackage findOrCreatePack(String name)
    {
        InvarPackage info = packAll.get(name);
        if (info == null)
        {
            info = new InvarPackage(name, true);
            packAll.put(name, info);
        }
        return info;
    }

    public InvarPackage getPack(String name)
    {
        return packAll.get(name);
    }

    public Iterator<String> getPackNames()
    {
        return packAll.keySet().iterator();
    }

    public List<InvarType> findTypes(String typeName)
    {
        Iterator<String> i = packAll.keySet().iterator();
        InvarType type = null;
        List<InvarType> types = new ArrayList<InvarType>();
        while (i.hasNext())
        {
            InvarPackage pack = packAll.get(i.next());
            type = pack.getType(typeName);
            if (type != null)
                types.add(type);
        }
        return types;
    }

    public InvarType findBuildInType(String typeName)
    {
        return packBuildIn.getType(typeName.toLowerCase());
    }

    public boolean isBuildInPack(InvarPackage pack)
    {
        return pack == packBuildIn;
    }

    @SuppressWarnings ("unchecked")
    public <T extends InvarType> T findType(String name, InvarPackage pack)
        throws Throwable
    {
        InvarType t = pack.getType(name);
        if (t == null)
        {
            t = packBuildIn.getType(name);
        }
        return (T)t;
    }

    public void aliasAdd(TypeEnum type)
    {
        typeWithAlias.put(type.getAlias(), type);
    }

    public void aliasAdd(TypeStruct type)
    {
        typeWithAlias.put(type.getAlias(), type);
    }

    public InvarType aliasGet(String alias)
    {
        return typeWithAlias.get(alias);
    }

    public Iterator<String> aliasNames()
    {
        return typeWithAlias.keySet().iterator();
    }

}