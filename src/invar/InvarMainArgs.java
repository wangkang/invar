package invar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

final public class InvarMainArgs
{
    private HashMap<String,List<String>> userArgs;
    private HashMap<String,List<String>> defaultArgs;

    public InvarMainArgs()
    {
        userArgs = new HashMap<String,List<String>>();
        defaultArgs = new HashMap<String,List<String>>();
    }

    public Boolean has (String key)
    {
        return userArgs.get(key) != null;
    }

    public List<String> getAll (String key)
    {
        List<String> list = userArgs.get(key);
        if (list == null || list.size() == 0)
            list = defaultArgs.get(key);
        return list;
    }

    public String get (String key)
    {
        List<String> list = getAll(key);
        return list == null ? null : list.get(0);
    }

    public void addDefault (String key, String value)
    {
        List<String> list = defaultArgs.get(key);
        if (list == null)
        {
            list = new ArrayList<String>();
            defaultArgs.put(key, list);
        }
        list.add(value);
    }

    public void parseArguments (String[] args)
    {
        userArgs = new HashMap<String,List<String>>();
        List<String> listCurrent = null;
        for (String arg : args)
        {
            if (arg.charAt(0) == '-')
            {
                listCurrent = new ArrayList<String>();
                userArgs.put(arg.substring(1), listCurrent);
            }
            else if (listCurrent != null)
            {
                listCurrent.add(arg);
            }
        }
    }

}
