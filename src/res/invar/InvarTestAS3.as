package invar{

import test.xyz.ConfigRoot;
import flash.display.Sprite;
import flash.utils.getTimer;

[SWF(backgroundColor="#FFFFFF", frameRate="31", width="160", height="100")]
public class InvarTestAS3 extends Sprite
{
	public function InvarTestAS3()
	{
		var t:int = getTimer();
		InvarReadData.verbose = true;
		InvarReadData.aliasBasics = InvarRuntime.aliasBasic();
		InvarReadData.aliasEnums = InvarRuntime.aliasEnums();
		InvarReadData.aliasStructs = InvarRuntime.aliasStructs();
		var o:Object = new ConfigRoot();
		try
		{
			new InvarReadData('../data/data.xml').parse(o, Xdata);
		}
		catch(error:Error)
		{
			trace(error.getStackTrace());
		}
		trace("InvarTestAS3.InvarTestAS3()", getTimer() - t);
	}


	private var Xdata:XML = // 
	<root revision="235">
	<info invar="info">
		<map invar="mapEnum">
			<Enumeration key="one" value="1" />
			<Enumeration key="two" value="2" />
			<Enumeration key="ten" value="10" />
		</map>
		<info invar="next" numb1="-0x80">
			<int8 invar="numb1" value="0x7F" />
			<int16 invar="numb2" value="-0x8000" />
			<int16 invar="numb2" value="0x7FFF" />
			<int32 invar="numb3" value="-0x80000000" />
			<int32 invar="numb3" value="0x7FFFFFFF" />
			<int64 invar="numb4" value="-0x8000000000000000" />
			<int64 invar="numb4" value="0x7FFFFFFFFFFFFFFF" />
			<uint8 invar="numb5" value="0xFF" />
			<uint16 invar="numb6" value="0xFFFF" />
			<uint32 invar="numb7" value="0xFFFFFFFF" />
			<uint64 invar="numb8" value="FFFFFFFFFFFFFFFF" />
			<float invar="numb9" value="98.7654321" />
			<double invar="numb10" value="987654321.123456789" />
			<bool invar="bool" value="true" />
			<string invar="text" value="China" />
			<vec invar="infos">
				<info text="A" numb2="32767" />
				<info text="B" numb8="28" />
				<info text="C" numb8="38" />
			</vec>
			<map invar="mapEnum">
				<Enumeration key="#&lt;one>" value="1" />
				<Enumeration key="#&lt;two>" value="2" />
				<Enumeration key="#&lt;ten>" value="10" />
			</map>
		</info>
	</info>
	<infox invar="infox">
		<conflict invar="conflictType">
			<vec invar="bytes">
				<int8 value="-0x80" />
				<int8 value="0x7F" />
				<int8 value="0x70" />
				<int8 value="0x71" />
				<int8 value="0x72" />
				<int8 value="0x73" />
			</vec>
		</conflict>
	</infox>
	<string invar="revision" value="236" />
	</root>
	;
}
}
