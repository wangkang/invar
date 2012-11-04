package invar;

import invar.io.WriteOutputCode;
import invar.model.TypeEnum;
import invar.model.TypeStruct;

final public class InvarWriteAS3 extends WriteOutputCode
{

    @Override
    protected Boolean beforeWrite(InvarContext ctx)
    {
        return true;
    }

    @Override
    protected String codeStruct(TypeStruct type)
    {
        return "";
    }

    @Override
    protected String codeEnum(TypeEnum type)
    {
        return "";
    }

}
