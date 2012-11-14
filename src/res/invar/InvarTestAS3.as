package invar {

import test.xyz.ConfigRoot;
import flash.display.Sprite;
import flash.utils.getTimer;

[SWF(backgroundColor="#FFFFFF", frameRate="60", width="160", height="100")]
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
			XML.ignoreWhitespace = true;
			XML.ignoreComments = true;
			InvarReadData.aliasBasics = InvarRuntime.aliasBasic();
			InvarReadData.aliasEnums = InvarRuntime.aliasEnums();
			InvarReadData.aliasStructs = InvarRuntime.aliasStructs();
			new InvarReadData('../data/data.xml').parse(o, Xdata);
		}
		catch(error:Error)
		{
			trace(error.getStackTrace());
		}
		trace("InvarTestAS3.InvarTestAS3()", getTimer() - t);
	}

	private var Xdata:XML = // 
	<root revision="235"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:noNamespaceSchemaLocation="data.xsd">
	
	<info>
		<mapEnum>
			<Enumeration key="one" value="1" />
			<Enumeration key="two" value="2" />
			<Enumeration key="ten" value="10" />
		</mapEnum>
		<next numb1="-0x80" numb2="-0x8000" numb3="-0x80000000">
			<numb1 value="0x7F" />
			<numb2 value="0x7FFF" />
			<numb3 value="0x7FFFFFFF" />
			<numb4 value="-0x8000000000000000" />
			<numb4 value="0x7FFFFFFFFFFFFFFF" />
			<numb5 value="0xFF" />
			<numb6 value="0xFFFF" />
			<numb7 value="0xFFFFFFFF" />
			<numb8 value="FFFFFFFFFFFFFFFF" />
			<numb9 value="98.7654321" />
			<numb10 value="987654321.123456789" />
			<bool value="true" />
			<text value="China" />
			<infos>
				<x text="A" numb2="32767" />
				<x text="B" numb8="28" />
				<x text="C" numb8="38" />
			</infos>
			<mapEnum>
				<x key="#&lt;one>" value="1" />
				<x key="#&lt;two>" value="2" />
				<x key="#&lt;ten>" value="10" />
			</mapEnum>
		</next>
	</info>
	<infox><conflictType><bytes>
		<x value="-0x80" />
		<x value="0x7F" />
		<x value="0x70" />
		<x value="0x71" />
		<x value="0x72" />
		<x value="0x73" />
	</bytes></conflictType></infox>
	<revision value="236" />
	</root>
	;
}
}
