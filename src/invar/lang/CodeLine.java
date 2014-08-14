package invar.lang;

class CodeLine
{
    private final String code; // source code file text content
    private final int    from; // begin index of this line
    private final int    dest; // end index of this line
    private final int    index; //

    public CodeLine(int from, int dest, String code, int index)
    {
        // [from, dest)
        this.from = from < 0 ? 0 : from;
        this.dest = dest < 0 ? 0 : dest;
        this.code = code;
        this.index = index;
    }

    public int getFrom ()
    {
        return from;
    }

    public int getDest ()
    {
        return dest;
    }

    public int numChars ()
    {
        return dest - from;
    }

    public char charAt (int i)
    {
        return code.charAt(from + i);
    }

    public String toString ()
    {
        StringBuilder s = new StringBuilder();
        s.append((index + 1) + " : ");
        if (numChars() > 0)
            s.append(code.substring(from, dest));
        s.append('\n');
        return s.toString();
    }
}
