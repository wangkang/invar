package invar.lang;

class CodeLine
{
    static public int    maxLines;

    private final String code;    // source code file text content
    private final int    index;   // line number
    private final int    realFrom; // begin index of this line
    private final int    realDest; // end index of this line
    private final int    from;    // begin index,  white spcace at beginning excluded
    private final int    dest;    // end index, white spcace at ending excluded

    public CodeLine(int index, int iFrom, int iDest, final String code)
    {
        this.code = code;
        this.index = index;
        // [from, dest)
        this.realFrom = iFrom < 0 ? 0 : iFrom;
        this.realDest = iDest < iFrom ? iFrom : iDest;
        int fromValid = this.realFrom;
        int destValid = this.realDest;
        for (int i = fromValid; i < iDest; ++i)
        {
            char c = code.charAt(i);
            if (isEmpty(c))
                ++fromValid;
            else
                break;
        }
        for (int i = destValid - 1; i >= fromValid; --i)
        {
            char c = code.charAt(i);
            if (isEmpty(c))
                --destValid;
            else
                break;
        }
        this.from = fromValid;
        this.dest = destValid;
    }

    public int numChars ()
    {
        return dest - from;
    }

    public char charAt (int i)
    {
        return code.charAt(from + i);
    }

    public boolean validIndex (int i)
    {
        return i >= 0 && i < numChars();
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder(numChars() + 64);
        s.append('\n');
        s.append(index + 1);
        if (true)
        {
            int lenPad = String.valueOf(code.length() - 1).length();
            s.append(" | ");
            s.append("[");
            s.append(stringPad('0', lenPad, String.valueOf(this.from), false));
            s.append(",");
            s.append(stringPad('0', lenPad, String.valueOf(this.dest), false));
            s.append(")");
        }
        s.append(" | ");
        if (numChars() > 0)
        {
            s.append(code.substring(from, dest));
        }
        return s.toString();
    }

    static private boolean isEmpty (char c)
    {
        return ' ' == c || '\t' == c;
    }

    static private String stringPad (char pad, int len, String str, boolean alignLeft)
    {
        int delta = len - str.length();
        if (delta <= 0)
        {
            return str;
        }
        else
        {
            StringBuilder s = new StringBuilder(len);
            if (alignLeft)
            {
                s.append(str);
                for (int i = 0; i < delta; i++)
                {
                    s.append(pad);
                }
            }
            else
            {
                for (int i = 0; i < delta; i++)
                {
                    s.append(pad);
                }
                s.append(str);
            }
            return s.toString();
        }
    }
}
