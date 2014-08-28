package invar.lang;

import invar.InvarContext;
import invar.model.InvarField;
import invar.model.InvarPackage;
import invar.model.InvarType;
import invar.model.InvarType.TypeID;
import invar.model.TypeEnum;
import invar.model.TypeProtocol;
import invar.model.TypeStruct;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TokenParser
{
    private final static String               STR_EMPTY       = "";
    private final static String               SPLIT_GNERICS   = "-";
    private final static String               SPLIT_PACK_TYPE = "::";

    private final Map<String,InvarType>       searchCache;
    private final Map<TokenNode,TypeEnum>     enumNodes;
    private final Map<TokenNode,TypeStruct>   structNodes;
    private final Map<TokenNode,TypeProtocol> protocNodes;

    public TokenParser()
    {
        enumNodes = new LinkedHashMap<TokenNode,TypeEnum>(256);
        structNodes = new LinkedHashMap<TokenNode,TypeStruct>(256);
        protocNodes = new LinkedHashMap<TokenNode,TypeProtocol>(256);
        searchCache = new HashMap<String,InvarType>(512);
    }

    public void init (TokenNode node, InvarContext ctx) throws Exception
    {
        //System.out.println("TokenParser.init()\n" + node.toStringXml());
        int len = node.numChildren();
        for (int i = 0; i < len; i++)
        {
            TokenNode n = node.getChild(i);
            String packName = "";
            if (n.getName().equals("package"))
            {
                packName = getAttrOptional(n, "name");
            }
            InvarPackage pack = ctx.findOrCreatePack(packName);
            initPack(pack, n, ctx);
        }
    }

    public void parse (InvarContext ctx) throws Exception
    {
        Iterator<Entry<TokenNode,TypeEnum>> ie = enumNodes.entrySet().iterator();
        while (ie.hasNext())
        {
            Entry<TokenNode,TypeEnum> e = ie.next();
            parseEnum(e.getKey(), e.getValue());
        }
        Iterator<Entry<TokenNode,TypeStruct>> is = structNodes.entrySet().iterator();
        while (is.hasNext())
        {
            Entry<TokenNode,TypeStruct> e = is.next();
            parseStruct(e.getKey(), e.getValue(), ctx);
        }
        Iterator<Entry<TokenNode,TypeProtocol>> ip = protocNodes.entrySet().iterator();
        while (ip.hasNext())
        {
            Entry<TokenNode,TypeProtocol> e = ip.next();
            parseProtoc(e.getKey(), e.getValue(), ctx);
        }
    }

    private void initPack (InvarPackage pack, TokenNode node, InvarContext ctx) throws Exception
    {
        int len = node.numChildren();
        for (int i = 0; i < len; i++)
        {
            TokenNode n = node.getChild(i);
            String alias = getAttrOptional(n, "alias");
            if (!alias.equals("") && ctx.aliasGet(alias) != null)
            {
                throw new Exception(error(n, "Repeated alias: " + alias));
            }
            if (n.getName().equals("enum"))
            {
                String name = getAttr(n, "name");
                String comment = getAttrOptional(n, "doc");
                TypeEnum t = new TypeEnum(name, pack, comment);
                addToPack(pack, t, n);
                if (alias.equals(""))
                {
                    t.setAlias(name);
                    ctx.aliasAdd(t);
                }
                enumNodes.put(n, t);
            }
            else if (n.getName().equals("struct"))
            {
                String name = getAttr(n, "name");
                String comment = getAttrOptional(n, "doc");
                TypeStruct t = new TypeStruct(name, pack, comment);
                addToPack(pack, t, n);
                if (alias.equals(""))
                {
                    t.setAlias(name);
                    ctx.aliasAdd(t);
                }
                structNodes.put(n, t);
                t.setShortField(getAttrOptional(n, "short"));
            }
            else if (n.getName().equals("protoc"))
            {
                String name = getAttr(n, "name");
                String comment = getAttrOptional(n, "doc");
                TypeProtocol t = new TypeProtocol(name, pack, comment);
                addToPack(pack, t, n);
                protocNodes.put(n, t);
            }
            else
            {
                warn(n, "Unexpected XML node. It has been ignorned: ");
            }
        }
    }

    private void parseEnum (TokenNode node, TypeEnum t) throws Exception
    {
        int len = node.numChildren();
        for (int i = 0; i < len; i++)
        {
            TokenNode n = node.getChild(i);
            String name = getAttr(n, "name");
            String value = getAttr(n, "value");
            String doc = getAttrOptional(n, "doc");
            try
            {
                Integer v = Integer.decode(value);
                t.addOption(name, v, doc);
            }
            catch (NumberFormatException e)
            {
                throw new Exception(error(n, "Not an inteager: " + value));
            }
        }
    }

    private void parseProtoc (TokenNode node, TypeProtocol t, InvarContext ctx) throws Exception
    {
        TokenNode nClient = null;
        TokenNode nServer = null;
        String pName = node.getName();
        int len = node.numChildren();
        for (int i = 0; i < len; i++)
        {
            TokenNode n = node.getChild(i);
            if (n.getName().equals("client"))
            {
                if (nClient == null)
                    nClient = n;
                else
                    throw new Exception(error(node, "Repeated client in protocol '" + pName + "'"));
            }
            else if (n.getName().equals("server"))
            {
                if (nServer == null)
                    nServer = n;
                else
                    throw new Exception(error(node, "Repeated server in protocol '" + pName + "'"));
            }
            else
            {
                throw new Exception(error(node, "Invalid element in protocol '" + pName + "'"));
            }

        }
        if (nClient != null)
        {
            parseStruct(nClient, t.getClient(), ctx);
            t.setNoClient(false);
        }
        if (nClient != null)
        {
            parseStruct(nServer, t.getServer(), ctx);
            t.setNoServer(false);
        }
    }

    private void parseStruct (TokenNode node, TypeStruct tStruct, InvarContext ctx) throws Exception
    {
        int len = node.numChildren();
        for (int i = 0; i < len; i++)
        {
            TokenNode n = node.getChild(i);
            String name = getAttr(n, "name");
            String type = getAttr(n, "type");
            String deft = getAttrOptional(n, "value");
            String doc = getAttrOptional(n, "doc");

            LinkedList<InvarType> types = parseFieldType(type, n, ctx);
            if (types == null || types.size() <= 0)
            {
                throw new Exception(error(n, "Field type is invalid"));
            }

            InvarType typeMain = types.get(0);
            types.remove(0);
            TypeID tid = typeMain.getId();
            boolean isStructSelf = (typeMain == tStruct);
            boolean useRef = tid.getUseRefer();
            boolean usePtr = false;
            boolean disableSetter = false;
            InvarField field = new InvarField(i, typeMain, name, doc, disableSetter);
            switch (tid) {
            case VEC:
            case MAP:
                disableSetter = true;
                break;
            default:
                break;
            }
            if (n.hasAttr("useref"))
            {
                useRef = Boolean.parseBoolean(getAttrOptional(n, "useref"));
            }
            disableSetter = Boolean.parseBoolean(getAttrOptional(n, "nosetter"));
            usePtr = Boolean.parseBoolean(getAttrOptional(n, "useptr"));

            field.setDefault(deft);
            field.setUseReference(useRef);
            field.setUsePointer(isStructSelf || typeMain.getId().getNullable() && usePtr);

            if (tid == TypeID.VEC)
            {
                if (types.size() < 1)
                {
                    types.addFirst(ctx.findBuildInType(TypeID.INT32));
                }
            }
            else if (tid == TypeID.MAP)
            {
                if (types.size() < 1)
                {
                    types.addFirst(ctx.findBuildInType(TypeID.INT32));
                }
                if (types.size() < 2)
                {
                    types.addFirst(ctx.findBuildInType(TypeID.INT32));
                }
            }
            else
            {
            }
            field.getGenerics().addAll(types);
            tStruct.addField(field);
        }
    }

    private LinkedList<InvarType> parseFieldType (String name, TokenNode n, InvarContext ctx) throws Exception
    {
        LinkedList<InvarType> ts = new LinkedList<InvarType>();
        if (name.indexOf(SPLIT_GNERICS) > 0)
        {
            String[] ss = name.split(SPLIT_GNERICS);
            for (String s : ss)
            {
                ts.add(searchType(s, ctx, n));
            }
        }
        else if (name.indexOf(SPLIT_GNERICS) == 0)
        {
        }
        else
        {
            ts.add(searchType(name, ctx, n));
        }
        return ts;
    }

    private InvarType searchType (String name, InvarContext ctx, TokenNode n) throws Exception
    {
        if (searchCache.containsKey(name))
        {
            return searchCache.get(name);
        }
        //System.out.println("TokenParser.searchType() --- " + name);
        String[] names = name.split(SPLIT_PACK_TYPE);
        String typeName = null;
        InvarPackage typePack = null;
        InvarType type = null;
        if (names.length == 1)
        {
            typeName = names[0];
        }
        else if (names.length == 2)
        {
            typeName = names[1];
            typePack = ctx.getPack(names[0]);
            if (typePack == null)
            {
                throw new Exception(error(n, "Package does not exist: " + name));
            }
        }
        else
        {
            throw new Exception(error(n, "Invalid type name: " + name));
        }
        type = ctx.findBuildInType(typeName);
        if (type == null)
        {
            if (typePack != null)
            {
                type = typePack.getType(typeName);
                if (type == null)
                {
                    throw new Exception(error(n, "No type in the package: " + name));
                }
            }
            else
            {
                List<InvarType> types = ctx.findTypes(typeName);
                if (types.size() == 1)
                {
                    type = types.get(0);
                }
                else if (types.size() > 1)
                {
                    // has conflicts
                    StringBuilder s = new StringBuilder();
                    for (InvarType t : types)
                    {
                        s.append('\n');
                        s.append(t.getPack().getName());
                        s.append(SPLIT_PACK_TYPE);
                        s.append(t.getName());
                    }
                    throw new Exception(error(n,
                                              "Find " + types.size() + " types. You should select one:" + s.toString()));
                }
                else
                {
                }
            }
        }
        if (type == null)
        {
            throw new Exception(error(n, "Undefined type: " + name));
        }
        else
        {
            searchCache.put(name, type);
        }
        if (type.getId() == TypeID.PROTOCOL)
        {
            throw new Exception(error(n, "Protocol type can not be here: " + type.getName()));
        }
        return type;
    }

    private void addToPack (InvarPackage pack, InvarType t, TokenNode n) throws Exception
    {
        if (pack.getType(t.getName()) != null)
        {
            throw new Exception(error(n, "Repeated type name: " + t.getName()));
        }
        pack.add(t);
    }

    private String getAttrOptional (TokenNode n, String key)
    {
        String s = n.getAttr(key);
        return s == null ? STR_EMPTY : s;
    }

    private String getAttr (TokenNode node, String key) throws Exception
    {
        String v = getAttrOptional(node, key);
        if (v.equals(STR_EMPTY))
        {
            throw new Exception(error(node, "Attribute '" + key + "' is required."));
        }
        return v;
    }

    private String error (TokenNode node, String hint)
    {
        boolean withChildren = node.numAttributes() < 1;
        StringBuilder s = new StringBuilder();
        s.append("[ERROR] ");
        s.append(hint);
        s.append('\n');
        s.append(node.toStringXml(withChildren));
        s.append('\n');
        s.append(node.getFilePath());
        return s.toString();
    }

    private void warn (TokenNode node, String hint)
    {
        boolean withChildren = node.numAttributes() < 1;
        StringBuilder s = new StringBuilder();
        s.append("[WARN] ");
        s.append(hint);
        s.append('\n');
        s.append(node.toStringXml(withChildren));
        s.append('\n');
        s.append(node.getFilePath());
        System.out.println(s);
    }
}
