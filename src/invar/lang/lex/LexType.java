package invar.lang.lex;

public enum LexType
{
    HIDDEN_CHARS(true), BLOCK_L(true), BLOCK_R(true), //
    DOC, DOC_LINE, //
    STRUCT_SHARED, STRUCT_ID, STRUCT_NAME, STRUCT_FIELD_DEFT;

    private LexType()
    {
        this.skipped = false;
    }

    private LexType(boolean skipped)
    {
        this.skipped = skipped;
    }

    public boolean isSkipped ()
    {
        return skipped;
    }

    private final boolean skipped;
}
