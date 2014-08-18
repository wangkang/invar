package invar.lang.lex;

public final class LexToken
{
    private final LexType      type;
    private final CharSequence code;
    private final int          from;
    private final int          dest;

    public LexToken(LexType type, CharSequence code, int from, int dest)
    {
        this.type = type;
        this.code = code;
        this.from = from;
        this.dest = dest;
    }

    public String content ()
    {
        return code.subSequence(from, dest).toString();
    }

    public CharSequence sequence ()
    {
        return code.subSequence(from, dest);
    }

    public LexType getType ()
    {
        return type;
    }
}
