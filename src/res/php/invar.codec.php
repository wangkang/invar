//===----------------------------*  PHP 5  *-------------------------------===//
//
//             THIS FILE IS GENERATED BY INVAR. DO NOT EDIT !!!
//
//===----------------------------------------------------------------------===//
<?php

namespace invar;

final class BinaryWriter {
	static public function writeInt08($v, &$bytes) {
		$bytes .= pack ( 'c', $v );
	}
	static public function writeInt16($v, &$bytes) {
		$bytes .= pack ( 'n', $v );
	}
	static public function writeInt32($v, &$bytes) {
		$bytes .= pack ( 'N', $v );
	}
	static public function writeInt64($v, &$bytes) {
		$bytes .= pack ( 'NN', $v, 0x00000000 );
	}
	static public function writeUInt08($v, &$bytes) {
		$bytes .= pack ( 'c', $v );
	}
	static public function writeUInt16($v, &$bytes) {
		$bytes .= pack ( 'n', $v );
	}
	static public function writeUInt32($v, &$bytes) {
		$bytes .= pack ( 'N', $v );
	}
	static public function writeUInt64($v, &$bytes) {
		$bytes .= pack ( 'NN', $v, 0x00000000 );
	}
	static public function writeFloat32($v, &$bytes) {
		$bytes .= pack ( 'f', $v );
	}
	static public function writeFloat64($v, &$bytes) {
		$bytes .= pack ( 'd', $v );
	}
	static public function writeBoolean($v, &$bytes) {
		$bytes .= $v == TRUE ? '\x01' : '\x00';
	}
	static public function writeUTF($v, &$bytes) {
		$bytes .= pack ( 'nA*', strlen ( $v ), $name );
	}
}
final class BinaryReader {
	private $data = NULL;
	private $bytes = NULL;
	private $bytesPos = 1;
	private $bytesLen = 0;
	function __construct(&$data) {
		// $data The packed data.
		$this->data = $data;
		$this->bytes = unpack ( "C*", $data ); // index begin from 1.
		$this->bytesLen = count ( $bytes );
	}
	function __destruct() {
		// print "Destroying DataInput...\n";
		$this->bytes = NULL;
		$this->bytesLen = 0;
		$this->bytesPos = 1;
	}
	function checkAvailable($offset) {
		if ($this->bytesPos + $offset > $this->bytesLen) {
			throw new Exception ( 'EOF Error' );
		}
	}
	public function readInt08() {
		$this->checkAvailable ( 1 );
		$result = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return $result;
	}
	public function readInt16() {
		$this->checkAvailable ( 2 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return ($b1 << 8) | $b2;
	}
	public function readInt32() {
		$this->checkAvailable ( 4 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b3 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b4 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return ($b1 << 24) | ($b2 << 16) | ($b3 << 8) | $b4;
	}
	public function readInt64() {
		$this->checkAvailable ( 8 );
		$i = 0;
		$result = '';
		while ( $i < 8 ) {
			$bi = $this->bytes [$this->bytesPos];
			$this->bytesPos ++;
			$result .= dechex ( $bi );
		}
		return $result;
	}
	public function readUInt08() {
		$this->checkAvailable ( 1 );
		$result = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
	}
	public function readUInt16() {
		$this->checkAvailable ( 2 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return ($b1 << 8) | $b2;
	}
	public function readUInt32() {
		$this->checkAvailable ( 4 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b3 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b4 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return ($b1 << 24) | ($b2 << 16) | ($b3 << 8) | $b4;
	}
	public function readUInt64() {
		$this->checkAvailable ( 8 );
		$result = '';
		$i = 0;
		while ( $i < 8 ) {
			$bi = $this->bytes [$this->bytesPos];
			$this->bytesPos ++;
			$result .= dechex ( $bi );
		}
		return $result;
	}
	public function readBoolean() {
		$this->checkAvailable ( 1 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return $b1 == 0 ? FALSE : TRUE;
	}
	public function readFloat32() {
		/* s（sign）；e（exponent）；m （mantissa） */
		$bits = $this->readInt32 ();
		$s = ($bits >> 31) == 0 ? 1 : - 1;
		$e = ($bits >> 23) & 0xff;
		$m = ($e == 0) ? ($bits & 0x007fffff) << 1 : ($bits & 0x007fffff) | 0x800000;
		return $s * $m * pow ( 2, $e - 150 );
	}
	public function readFloat64() {
		return $this->readInt64();
	}
	public function readUTF() {
	 	$len = $this->readUInt16 ();
		$this->checkAvailable ( $len );
		$s = substr($this->data, $this->bytesPos, $len);
		return unpack('A*', $s)[1];
	}
}
