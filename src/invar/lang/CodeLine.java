package invar.lang;

final class CodeLine
{
    static public int          maxLines;

    private final CharSequence code;    // source code file text content
    private final int          index;   // line number
    private final int          from;    // begin index of this line
    private final int          dest;    // end index of this line

    public CodeLine(int index, int iFrom, int iDest, final CodeFile file)
    {
        this.index = index;
        this.code = file.getCode();
        // [from, dest)
        this.from = iFrom < 0 ? 0 : iFrom;
        this.dest = iDest < iFrom ? iFrom : iDest;

        int fromValid = this.from;
        int destValid = this.dest;
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
            s.append(code.subSequence(from, dest));
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
