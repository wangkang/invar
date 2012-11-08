package invar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InvarReadData
{
    static public HashMap<String,Class<?>> alias   = null;
    static public Boolean                  verbose = false;

    static public void start(Object root, String path, String suffix) throws Exception
    {
        File file = new File(path);
        if (!file.exists())
            throw new IOException("\nFile don't exist: "
                    + file.getAbsolutePath());
        List<File> files = new ArrayList<File>();
        recursiveReadFile(files, file, suffix);
        for (File f : files)
        {
            log("Read <- " + f.getAbsolutePath());
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(f);
            if (!doc.hasChildNodes())
                return;
            Node nRoot = doc.getFirstChild();
            new InvarReadData(f.getAbsolutePath()).parse(root, nRoot, "");
        }
    }
    static private void recursiveReadFile(List<File> all, File file, String suffix)
    {
        if (all.size() > 1024)
            return;
        if (file.isFile())
        {
            all.add(file);
        }
        else if (file.isDirectory())
        {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++)
                recursiveReadFile(all, files[i], suffix);
        }
        else
        {
        }
    }

    static private final String GENERIC_LEFT    = "<";
    static private final String GENERIC_RIGHT   = ">";
    static private final String GENERIC_SPLIT   = ", ";
    static private final String ATTR_FIELD_NAME = "var";
    static private final String ATTR_VALUE      = "value";
    static private final String PREFIX_SETTER   = "set";
    static private final String PREFIX_GETTER   = "get";

    static private String upperHeadChar(String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }
    static private String fixedLen(String blank, Integer len, String str)
    {
        int delta = len - str.length();
        if (delta > 0)
            for (int i = 0; i < delta; i++)
                str += blank;
        return str;
    }
    static private boolean isSimpleValue(Class<?> vType)
    {
        if (String.class == vType)
            return true;
        else if (Boolean.class == vType)
            return true;
        else if (Byte.class == vType)
            return true;
        else if (Short.class == vType)
            return true;
        else if (Integer.class == vType)
            return true;
        else if (Long.class == vType)
            return true;
        else if (Float.class == vType)
            return true;
        else if (Double.class == vType)
            return true;
        else
            return false;
    }
    static private Object parseSimple(Class<?> vType, String s)
    {
        Object arg = null;
        if (String.class == vType)
            arg = s;
        else if (Boolean.class == vType)
            arg = Boolean.parseBoolean(s);
        else if (Byte.class == vType)
            arg = Byte.parseByte(s);
        else if (Short.class == vType)
            arg = Short.parseShort(s);
        else if (Integer.class == vType)
            arg = Integer.parseInt(s);
        else if (Long.class == vType)
            arg = Long.parseLong(s);
        else if (Float.class == vType)
            arg = Float.parseFloat(s);
        else if (Double.class == vType)
            arg = Double.parseDouble(s);
        else
        {
        }
        if (verbose)
        {
            log(fixedLen(" ", 10, vType.getSimpleName()) + ": " + arg);
        }
        return arg;
    }
    static private String[] parseGenericTypes(String T)
    {
        int iBegin = T.indexOf(GENERIC_LEFT) + 1;
        int iEnd = T.lastIndexOf(GENERIC_RIGHT);
        if (iBegin > 0 && iEnd > iBegin)
        {
            String substr = T.substring(iBegin, iEnd);
            return substr.split(GENERIC_SPLIT);
        }
        return null;
    }
    static private String getAttrOptional(Node node, String name)
    {
        String v = "";
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null)
            return v;
        Node n = attrs.getNamedItem(name);
        if (n != null)
            v = n.getNodeValue();
        return v;
    }
    static private String formatXmlNode(Node n)
    {
        NamedNodeMap attrs = n.getAttributes();
        StringBuilder code = new StringBuilder();
        code.append("<" + n.getNodeName());
        int len = attrs != null ? attrs.getLength() : 0;
        for (int i = 0; i < len; i++)
        {
            Node a = attrs.item(i);
            code.append(" " + a.toString());
        }
        code.append(" />");
        return code.toString();
    }
    static private void log(Object txt)
    {
        System.out.println(txt);
    }

    String path;

    public InvarReadData(String path)
    {
        this.path = path;
    }

    @SuppressWarnings ("unchecked")
    private void parse(Object o, Node n, String T) throws Exception
    {
        if (o instanceof LinkedList)
            parseVec((LinkedList<Object>)o, n, T);
        else if (o instanceof HashMap)
            parseMap((HashMap<Object,Object>)o, n, T);
        else
            parseStruct(o, n);
    }

    private void parseStruct(Object o, Node n) throws Exception
    {
        //log("parseStruct() -------- " + o.getClass().getName());
        Class<?> ClsO = o.getClass();
        validateNode(n, ClsO);
        HashMap<String,Node> mapKeyNode = mergeAttrsAndChildren(n);
        Iterator<String> i = mapKeyNode.keySet().iterator();
        while (i.hasNext())
        {
            String key = i.next();
            Node cn = mapKeyNode.get(key);
            HashMap<String,Method> mapGetters = getGetters(o.getClass());
            String getterName = PREFIX_GETTER + upperHeadChar(key);
            Method getter = mapGetters.get(getterName);
            if (getter == null)
                onError(cn, "No getter named \"" + getterName + "\" in "
                        + o.getClass());
            Class<?> vType = getter.getReturnType();
            boolean isFinish = setSimpleValue(vType, o, key, cn, n);
            if (isFinish)
                continue;
            Object co = getter.invoke(o);
            if (co != null)
                validateNode(cn, co.getClass());
            if (co == null)
            {
                HashMap<String,Method> mapSetters = getSetters(o.getClass());
                String setterName = PREFIX_SETTER + upperHeadChar(key);
                Method setter = mapSetters.get(setterName);
                if (setter == null)
                {
                    onError(n, "No setter named \"" + setterName + "()\" in "
                            + o.getClass());
                }
                Class<?> ClsN = alias.get(n.getNodeName());
                co = ClsN.newInstance();
                setter.invoke(o, co);
            }
            parse(co, cn, getter.getGenericReturnType().toString());
        }
    }
    private void parseVec(LinkedList<Object> list, Node n, String T) throws Exception
    {
        //log("parseVec() -------- " + T);
        String[] typeNames = parseGenericTypes(T);
        if (typeNames == null || typeNames.length != 1)
            onError(n, "Unexpected type: " + T.toString());
        Class<?> Cls = loadGenericClass(typeNames[0]);
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node vn = children.item(i);
            if (Node.ELEMENT_NODE != vn.getNodeType())
                continue;
            Object v = parseGenericChild(vn, Cls, typeNames[0]);
            list.add(v);
        }
    }

    private void parseMap(HashMap<Object,Object> map, Node n, String T) throws Exception
    {
        //log("InvarParseXml.parseMap() -------- " + T);
        String[] typeNames = parseGenericTypes(T);
        if (typeNames.length != 2)
            onError(n, "Unexpected type: " + T.toString());
        Class<?> ClsK = loadGenericClass(typeNames[0]);
        Class<?> ClsV = loadGenericClass(typeNames[1]);
        List<Node> children = elementNodes(n);
        int len = children.size();

        if ((0x01 & len) != 0)
            onError(n, "Invaid amount of children: " + len);

        for (int i = 0; i < len; i += 2)
        {
            Node kn = children.get(i);
            Node vn = children.get(i + 1);
            Object k = parseGenericChild(kn, ClsK, typeNames[0]);
            Object v = parseGenericChild(vn, ClsV, typeNames[1]);;
            map.put(k, v);
        }
    }
    private Object parseGenericChild(Node cn, Class<?> Cls, String T) throws Exception
    {
        validateNode(cn, Cls);
        if (isSimpleValue(Cls))
            return parseSimple(Cls, getAttr(cn, ATTR_VALUE));
        else
        {
            Object co = Cls.newInstance();
            parse(co, cn, T);
            return co;
        }
    }
    private Class<?> loadGenericClass(String T) throws ClassNotFoundException
    {
        String name = T;
        if (T.indexOf(GENERIC_LEFT) >= 0)
        {
            name = T.substring(0, T.indexOf(GENERIC_LEFT));
        }
        return ClassLoader.getSystemClassLoader().loadClass(name);
    }

    private Class<?> validateNode(Node n, Class<?> ClsO) throws Exception
    {
        String name = n.getNodeName();
        Class<?> ClsN = alias != null ? alias.get(name) : null;
        if (ClsN == null)
            onError(n, "Node name \"" + name + "\" is invalid.");
        if (ClsN != ClsO)
            onError(n, "Require: " + ClsN + ", Current: " + ClsO);
        return ClsN;
    }

    private HashMap<String,Node> mergeAttrsAndChildren(Node n) throws Exception
    {
        HashMap<String,Node> mapKeyNode = new HashMap<String,Node>();
        NamedNodeMap attrs = n.getAttributes();
        int attrsLen = attrs.getLength();
        for (int i = 0; i < attrsLen; i++)
        {
            Node cn = attrs.item(i);
            mapKeyNode.put(cn.getNodeName(), cn);
        }
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node cn = children.item(i);
            if (Node.ELEMENT_NODE != cn.getNodeType())
                continue;
            String key = getAttr(cn, ATTR_FIELD_NAME);
            if (mapKeyNode.containsKey(key))
                onError(cn, "\"" + key + "\" has been used once.");
            else
                mapKeyNode.put(key, cn);
            cn.getAttributes().removeNamedItem(ATTR_FIELD_NAME);
        }
        return mapKeyNode;
    }
    private List<Node> elementNodes(Node n)
    {
        List<Node> nodes = new ArrayList<Node>();
        NodeList children = n.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++)
        {
            Node cn = children.item(i);
            if (Node.ELEMENT_NODE != cn.getNodeType())
                continue;
            nodes.add(cn);
        }
        return nodes;
    }
    private boolean setSimpleValue(Class<?> vType, Object o, String key, Node vNode, Node n) throws Exception
    {
        if (HashMap.class == vType)
            return false;
        else if (LinkedList.class == vType)
            return false;
        else
        {
        }
        HashMap<String,Method> mapSetters = getSetters(o.getClass());
        String setterName = PREFIX_SETTER + upperHeadChar(key);
        Method setter = mapSetters.get(setterName);
        if (setter == null)
        {
            onError(n, "No setter named \"" + setterName + "()\" in "
                    + o.getClass());
        }
        String s = null;
        if (Node.ATTRIBUTE_NODE == vNode.getNodeType())
            s = vNode.getNodeValue();
        else if (Node.ELEMENT_NODE == vNode.getNodeType())
        {
            s = getAttrOptional(vNode, ATTR_VALUE);
            validateNode(vNode, vType);
        }
        else
            s = "";
        Object arg = parseSimple(vType, s);
        if (arg != null)
        {
            setter.invoke(o, arg);
            return true;
        }
        return false;
    }
    private String getAttr(Node node, String name) throws Exception
    {
        String v = getAttrOptional(node, name);
        if (v.equals(""))
            onError(node, "Attribute '" + name + "' is required.");
        return v;
    }
    private void onError(Node n, String hint) throws Exception
    {
        throw new Exception(hint + "\n" + formatXmlNode(n) + "\n" + path);
    }

    static private final HashMap<Class<?>,HashMap<String,Method>> mapClassSetters = new HashMap<Class<?>,HashMap<String,Method>>();
    static private final HashMap<Class<?>,HashMap<String,Method>> mapClassGetters = new HashMap<Class<?>,HashMap<String,Method>>();

    static private HashMap<String,Method> getSetters(Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassSetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_SETTER))
                    methods.put(method.getName(), method);
            }
            mapClassSetters.put(ClsO, methods);
        }
        return methods;
    }
    static private HashMap<String,Method> getGetters(Class<?> ClsO)
    {
        HashMap<String,Method> methods = mapClassGetters.get(ClsO);
        if (methods == null)
        {
            Method[] meths = ClsO.getMethods();
            methods = new HashMap<String,Method>();
            for (Method method : meths)
            {
                if (method.getName().startsWith(PREFIX_GETTER))
                    methods.put(method.getName(), method);
            }
            mapClassGetters.put(ClsO, methods);
        }
        return methods;
    }
}