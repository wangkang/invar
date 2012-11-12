package invar{

import flash.utils.Dictionary;
import flash.utils.describeType;
import flash.utils.getDefinitionByName;

final public class InvarReadData
{
	static public var verbose:Boolean = false;
	static public var aliasBasics:Dictionary = null;
	static public var aliasEnums:Dictionary = null;
	static public var aliasStructs:Dictionary = null;


	public function InvarReadData(path:String)
	{
		XML.ignoreWhitespace = true;
		XML.ignoreComments = true;
		this.path = path;
		aliasBasics = InvarRuntime.aliasBasic();
		aliasEnums = InvarRuntime.aliasEnums();
		aliasStructs = InvarRuntime.aliasStructs();
	}

	public function parse(o:Object, n:XML):void
	{
		parseObject(o, n, '', '');
	}


	private var path:String;


	private function parseObject(o:Object, n:XML, T:String, debug:String):void
	{
		var alias:String = n.name();
		if (ALIAS_VEC == alias)
			parseVec(Vector.<*>(o), n, T, debug);
		else if (ALIAS_MAP == alias)
			parseMap(Dictionary(o), n, T, debug);
		else
			parseStruct(o, n, debug);
	}

	private function parseStruct(o:Object, n:XML, debug:String):void
	{
		var ClsO:Class = aliasStructs[n.name()];
		if (!ClsO)
			onError('No Class matches this XML', n);
		if (!(o is ClsO))
			onError('Object does not match this XML', n);
		var key:String = null;
		var attrs:XMLList = n.attributes();
		var x:XML = null;
		var v:Object;
		var T:String = '';
		for each (x in attrs)
		{
			key = x.name();
			if (ATTR_FIELD_NAME == key)
				continue;
			T = getFieldT(ClsO, key, n);
			var ClsF:Class = getFieldType(ClsO, key, n);
			v = parseSimple(ClsF, x.toXMLString(), T, debug + '.' + key, n);
			invokeSetter(v, key, o, n);
		}
		var children:XMLList = n.children();
		for each (x in children)
		{
			key = getAttr(x, ATTR_FIELD_NAME);
			T = getFieldT(ClsO, key, n);
			var alias:String = x.name();
			var ClsX:Class = getClassByAlias(alias);
			if (isSimple(alias))
			{
				v = parseSimple(ClsX, x.attribute(ATTR_VALUE), T, debug + '.' + key, x);
				invokeSetter(v, key, o, x);
			}
			else
			{
				T = getFieldT(ClsO, key, n);
				var co:Object = invokeGetter(key, o, x);
				if (co == null)
				{
					co = new ClsX();
					invokeSetter(co, key, o, x);
				}
				parseObject(co, x, T, debug + '.' + key);
			}
		}
	}

	private function parseVec(vec:Vector.<*>, n:XML, T:String, debug:String):void
	{
		var typeNames:Array = parseGenericTypes(T);
		if (typeNames == null || typeNames.length != 1)
			onError('Unexpected type: ' + T, n);
		var Cls:Class = loadGenericClass(typeNames[0]);
		var children:XMLList = n.children();
		for each (var x:XML in children)
		{
			var v:Object = parseGenericChild(x, Cls, typeNames[0], debug + "[" + vec.length + "]");
			vec.push(v);
		}
	}

	private function parseMap(map:Dictionary, n:XML, T:String, debug:String):void
	{
		var typeNames:Array = parseGenericTypes(T);
		if (typeNames == null || typeNames.length != 2)
			onError('Unexpected type: ' + T, n);
		var ClsK:Class = loadGenericClass(typeNames[0]);
		var ClsV:Class = loadGenericClass(typeNames[1]);
		var children:XMLList = n.children();
		var k:Object;
		var v:Object;
		if (isSimpleType(ClsK))
		{
			for each (var x:XML in children)
			{
				var s:String = getAttr(x, ATTR_MAP_KEY);
				k = parseSimple(ClsK, s, typeNames[0], debug + '.k', x);
				v = parseGenericChild(x, ClsV, typeNames[1], debug + '.v');
				map[k] = v;
			}
		}
		else
		{
			var len:int = children.length();
			if ((0x01 & len) != 0)
				onError('Invaid amount of children: ' + len, n);
			for (var i:int = 0; i < len; i += 2)
			{
				var kn:XML = children[i];
				var vn:XML = children[i + 1];
				k = parseGenericChild(kn, ClsK, typeNames[0], debug + '.k');
				v = parseGenericChild(vn, ClsV, typeNames[1], debug + '.v');
				map[k] = v;
			}
		}
	}

	private function parseGenericChild(x:XML, Cls:Class, T:String, debug:String):Object
	{
		if (isSimple(x.name()))
		{
			return parseSimple(Cls, getAttr(x, ATTR_VALUE), T, debug, x);
		}
		else
		{
			var co:Object = new Cls();
			parseObject(co, x, T, debug);
			return co;
		}
	}

	private function parseSimple(Cls:Class, s:String, T:String, debug:String, x:XML):Object
	{
		var arg:* = null;
		if (String == Cls)
			arg = s;
		else if (int == Cls)
			arg = parseInt(s);
		else if (uint == Cls)
			arg = parseInt(s);
		else if (Number == Cls)
			arg = parseFloat(s);
		else if (Boolean == Cls)
			arg = (s == 'true' ? true : false);
		else if (isEnum(Cls))
			arg = Cls['convert'](parseInt(s));
		else
		{
		}
		if (verbose)
			log(fixedLen(32, debug) + ' : ' + fixedLen(24, T) + ' : ' + arg);
		if ('int8' == T)
			checkNumber(arg, -0x80, 0x7F, debug, x);
		else if ('int16' == T)
			checkNumber(arg, -0x8000, 0x7FFF, debug, x);
		else if ('int32' == T)
			checkNumber(arg, -0x80000000, 0x7FFFFFFF, debug, x);
		else if ('uint8' == T)
			checkNumber(arg, 0, 0xFF, debug, x);
		else if ('uint16' == T)
			checkNumber(arg, 0, 0xFFFF, debug, x);
		else if ('uint32' == T)
			checkNumber(arg, 0, 0xFFFFFFFF, debug, x);
		else
		{
		}
		return arg;
	}

	private function checkNumber(arg:Number, min:Number, max:Number, debug:String, x:XML):void
	{
		if (arg != arg)
			onError(debug + ': Number is NaN', x);
		if (arg < min || arg > max)
			onError(debug + ': Number is out of range, [' + min + ', ' + max + ']', x);
	}

	private function getFieldType(ClsO:Class, key:String, n:XML):Class
	{
		var map:Dictionary = getGetters(ClsO);
		var x:XML = map[key];
		if (!x)
			onError('No getter named "' + key + '" in ' + ClsO, n);
		return getDefinitionByName(x.@type) as Class;
	}

	private function getFieldT(ClsO:Class, key:String, n:XML):String
	{
		var map:Dictionary = getGetters(ClsO);
		var x:XML = map[key];
		if (!x)
			onError('No getter named "' + key + '" in ' + ClsO, n);
		var T:String = x.@type;
		var gene:XMLList = x.elements('metadata').(@name == 'InvarGenerics');
		if (gene.toXMLString() == "")
			return T;
		var Tx:XMLList = gene.elements('arg').(@key == 'T');
		T = Tx[0].@value;
		return T;
	}

	private function invokeGetter(key:String, o:Object, n:XML):Object
	{
		var name:String = key;
		if (!o.hasOwnProperty(name))
			onError('No getter named "' + name + '" in ' + o, n);
		return o[name];
	}

	private function invokeSetter(value:Object, key:String, o:Object, n:XML):void
	{
		var name:String = PREFIX_SETTER + upperHeadChar(key);
		if (!o.hasOwnProperty(name))
			onError('No setter named "' + name + '" in ' + o, n);
		o[name](value);
	}

	private function onError(hint:String, n:XML):void
	{
		var s:String = '\n' + hint;
		if (n)
			s += '\n' + n.toXMLString();
		throw new Error(s + '\n' + path);
	}

	private function getAttr(x:XML, name:String):String
	{
		var v:String = x.attribute(name);
		if (!v)
			onError('Attribute "' + name + '" is required.', x);
		return v;
	}


	static private var GENERIC_LEFT:String = '<';
	static private var GENERIC_RIGHT:String = '>';
	static private var GENERIC_SPLIT:String = ',';
	static private var ATTR_FIELD_NAME:String = 'invar';
	static private var ATTR_MAP_KEY:String = 'key';
	static private var ATTR_VALUE:String = 'value';
	static private var PREFIX_SETTER:String = 'set';
	static private var ALIAS_VEC:String = 'vec';
	static private var ALIAS_MAP:String = 'map';
	//
	static private var mapClassSetters:Dictionary = new Dictionary();
	static private var mapClassGetters:Dictionary = new Dictionary();


	static private function reflect(ClsO:Class):void
	{
		var x:XML = describeType(ClsO);
		var tName:String = x.@name;
		var xGets:XMLList = x.descendants('accessor');
		var xSets:XMLList = x.descendants('method');
		var dictGets:Dictionary = new Dictionary();
		var dictSets:Dictionary = new Dictionary();
		var key:String = null;
		for each (var xGet:XML in xGets)
		{
			if (tName != xGet.@declaredBy)
				continue;
			key = xGet.@name;
			dictGets[key] = xGet;
		}
		for each (var xSet:XML in xSets)
		{
			if (tName != xSet.@declaredBy)
				continue;
			key = xSet.@name;
			dictSets[key] = xSet;
		}
		mapClassSetters[ClsO] = dictSets;
		mapClassGetters[ClsO] = dictGets;
	}

	static private function getClassByAlias(name:String):Class
	{
		var ClsN:Class = aliasBasics[name];
		if (ClsN == null)
			ClsN = aliasEnums[name];
		if (ClsN == null)
			ClsN = aliasStructs[name];
		return ClsN;
	}

	static private function getGetters(ClsO:Class):Dictionary
	{
		var map:Dictionary = mapClassGetters[ClsO];
		if (!map)
			reflect(ClsO);
		return mapClassGetters[ClsO];
	}

	static private function parseGenericTypes(T:String):Array
	{
		var iBegin:int = T.indexOf(GENERIC_LEFT) + 1;
		var iEnd:int = T.lastIndexOf(GENERIC_RIGHT);
		if (iBegin > 0 && iEnd > iBegin)
		{
			var  substr:String = T.substring(iBegin, iEnd);
			return substr.split(GENERIC_SPLIT);
		}
		return null;
	}

	static private function upperHeadChar(s:String):String
	{
		return s.substring(0, 1).toUpperCase() + s.substring(1, s.length);
	}

	static private function fixedLen(len:int, str:String):String
	{
		var blank:String = ' ';
		var delta:int = len - str.length;
		if (delta > 0)
			for (var i:int = 0; i < delta; i++)
				str += blank;
		return str;
	}

	static private function isEnum(Cls:Class):Boolean
	{
		for each (var c:Class in aliasEnums)
			if (c == Cls)
				return true;
		return false;
	}

	static private function isSimpleType(Cls:Class):Boolean
	{
		if (String == Cls)
			return true;
		else if (int == Cls)
			return true;
		else if (uint == Cls)
			return true;
		else if (Number == Cls)
			return true;
		else if (Boolean == Cls)
			return true;
		else
			return isEnum(Cls);
	}

	static private function isSimple(alias:String):Boolean
	{
		if (alias == ALIAS_VEC)
			return false;
		else if (alias == ALIAS_MAP)
			return false;
		else
		{
			var Cls:Class = aliasBasics[alias];
			if (!Cls)
				Cls = aliasEnums[alias];
			if (!Cls)
				return false;
			return true;
		}
	}

	static private function loadGenericClass(T:String):Class
	{
		var name:String = T;
		if (T.indexOf(GENERIC_LEFT) >= 0)
		{
			name = T.substring(0, T.indexOf(GENERIC_LEFT));
		}
		var Cls:Class = aliasBasics[name];
		if (!Cls)
			Cls = getDefinitionByName(name) as Class;
		return Cls;
	}

	static private function log(txt:Object):void
	{
		trace(txt);
	}
}
}