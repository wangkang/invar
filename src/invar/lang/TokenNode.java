package invar.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TokenNode
{
    private final String             name;
    private final Map<String,String> attrs;
    private final List<TokenNode>    children;
    private String                   filePath;
    private boolean                  isFrozen;

    public TokenNode()
    {
        this("R-O-O-T");
    }

    public TokenNode(final String name)
    {
        this.name = name;
        this.attrs = new LinkedHashMap<String,String>(8);
        this.children = new ArrayList<TokenNode>(24);
        this.isFrozen = false;
        this.filePath = null;
    }

    public boolean hasAttr (String key)
    {
        return this.attrs.containsKey(key);
    }

    public String getAttr (String key)
    {
        return this.attrs.get(key);
    }

    public String getName ()
    {
        return name;
    }

    public String getFilePath ()
    {
        return filePath;
    }

    public void freeze ()
    {
        if (this.isFrozen)
            return;
        this.isFrozen = true;
        for (TokenNode n : this.children)
        {
            n.freeze();
        }
    }

    public int numAttributes ()
    {
        return this.attrs.size();
    }

    public int numChildren ()
    {
        return this.children.size();
    }

    public TokenNode getChild (int index)
    {
        return this.children.get(index);
    }

    public void putAttr (String key, String val)
    {
        if (this.isFrozen)
            return;
        this.attrs.put(key, val);
    }

    public void addChild (TokenNode n)
    {
        if (this.isFrozen)
            return;
        this.children.add(n);
    }

    public void setFilePath (String filePath)
    {
        if (this.isFrozen)
            return;
        this.filePath = filePath;
    }

    public String toStringXml ()
    {
        return this.toStringXml(true);
    }

    public String toStringXml (boolean withChildren)
    {
        StringBuilder code = new StringBuilder();
        code.append('<');
        code.append(this.name);
        Iterator<Entry<String,String>> iter = attrs.entrySet().iterator();
        while (iter.hasNext())
        {
            Entry<String,String> e = iter.next();
            code.append(' ');
            code.append(e.getKey());
            code.append('=');
            code.append('"');
            code.append(e.getValue());
            code.append('"');
        }
        if (!withChildren || this.children.size() <= 0)
        {
            code.append('/');
            code.append('>');
        }
        else
        {
            code.append('>');
            for (TokenNode n : this.children)
            {
                code.append('\n');
                code.append(n.toStringXml(withChildren));
            }
            code.append('\n');
            code.append('<');
            code.append('/');
            code.append(this.name);
            code.append('>');
        }
        return code.toString();
    }

}
