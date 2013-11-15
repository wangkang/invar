using System;

namespace Invar {

[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public class InvarRule : Attribute
{
    public String T {
        get { return _invarType; }
        set { _invarType = T; }
    }

    public String S {
        get { return _shortName; }
        set { _shortName = S; }
    }

    public InvarRule (String T, String S)
    {
        _invarType = T;
        _shortName = S;
    }

    private String _invarType;
    private String _shortName;
}
}