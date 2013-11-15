package invar {

import test.opq.ConfigRoot;
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
	<tns:root xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="invar.data data.xsd" xmlns:tns="invar.data">
	<revision value="1.0.2" />
	<revision value="1.0.3" />
	<revision value="1.0.4" />
	<info>
		<next number01="-128" number02="-32768">
			<number01 value="127" />
			<number02 value="32767" />
			<number03 value="2147483647" />
			<number04 value="-9223372036854775808" />
			<number04 value="9223372036854775807" />
			<number05 value="255" />
			<number06 value="65535" />
			<number07 value="4294967295" />
			<number08 value="18223372036854775807" />
			<number09 value="98.7654321" />
			<number10 value="987654321.123456789" />
			<bool value="true" />
			<s value="China" />
			<infos>
				<Info s="A" number02="32767" />
				<Info s="B" number08="28" />
				<Info s="C" number08="38" />
			</infos>
			<mapInfoG>
				<k-Info s="one" />
				<v-Gender value="1" />
				<k-Info s="two" />
				<v-Gender value="2" />
				<k-Info s="ten" />
				<v-Gender value="10" />
			</mapInfoG>
			<mapInfoG>
				<k-Info s="#&lt;one>" />
				<v-Gender value="1" />
				<k-Info s="#&lt;two>" />
				<v-Gender value="2" />
				<k-Info s="#&lt;ten>" />
				<v-Gender value="10" />
			</mapInfoG>
		</next>
	</info>

	<!-- complex data -->
	<infox>
		<conflict>
			<bytes>
				<int8 value="-128" />
				<int8 value="127" />
				<int8 value="100" />
				<int8 value="101" />
				<int8 value="108" />
				<int8 value="120" />
			</bytes>
		</conflict>
		<info2d>
			<vec>
				<Info bool="true" number07="4294967295" />
				<Info bool="false" number07="02" />
			</vec>
			<vec>
				<Info s="AAAAA" />
				<Info s="BBBBB" />
			</vec>
		</info2d>
		<info3d>
			<vec>
				<vec>
					<Info s="000" />
					<Info s="001" />
					<Info s="002" />
				</vec>
				<vec>
					<Info s="010" />
					<Info s="011" />
					<Info s="012" />
				</vec>
				<vec>
					<Info s="020" />
					<Info s="021" />
					<Info s="022" />
				</vec>
			</vec>
			<vec>
				<vec>
					<Info s="100" />
					<Info s="101" />
					<Info s="102" />
				</vec>
				<vec>
					<Info s="110" />
					<Info s="111" />
					<Info s="112" />
				</vec>
			</vec>
		</info3d>
		<infovm>
			<map>
				<Info key="666" bool="true" />
				<Info key="888" bool="true" />
			</map>
			<map>
				<Info key="777" bool="true" />
				<Info key="999" bool="true" />
			</map>
		</infovm>
		<mvei>
			<k-vec>
				<Gender value="1" />
				<Gender value="10" />
			</k-vec>
			<v-Info s="mvei001" number09=".64" />
			<k-vec>
				<Gender value="10" />
				<Gender value="2" />
			</k-vec>
			<v-Info s="mvei002">
				<number10 value=".2" />
			</v-Info>
		</mvei>

		<mive>
			<k-Info s="mive.0" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
			<k-Info s="mive.1" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
			<k-Info s="mive.2" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
		</mive>
		<mive>
			<k-Info s="mive.100" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
			<k-Info s="mive.101" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
			<k-Info s="mive.102" />
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
		</mive>

		<mvive>
			<k-vec>
				<Info s="mvive" />
				<Info s="mvive" />
				<Info s="mvive" />
			</k-vec>
			<v-vec>
				<Gender value="1" />
				<Gender value="2" />
			</v-vec>
		</mvive>

		<vmvive>
			<map>
				<k-vec>
					<Info s="vmvive.a" />
					<Info s="vmvive.a" />
					<Info s="vmvive.a" />
				</k-vec>
				<v-vec>
					<Gender value="1" />
				</v-vec>
				<k-vec>
					<Info s="vmvive.a" />
					<Info s="vmvive.a" />
					<Info s="vmvive.a" />
				</k-vec>
				<v-vec>
					<Gender value="1" />
				</v-vec>
			</map>
			<map>
				<k-vec>
					<Info s="vmvive.b" />
					<Info s="vmvive.b" />
				</k-vec>
				<v-vec>
					<Gender value="2" />
					<Gender value="10" />
				</v-vec>
				<k-vec>
					<Info s="vmvive.b" />
					<Info s="vmvive.b" />
				</k-vec>
				<v-vec>
					<Gender value="2" />
					<Gender value="10" />
				</v-vec>
			</map>
		</vmvive>
	</infox>
	</tns:root>
	
	;
}
}
