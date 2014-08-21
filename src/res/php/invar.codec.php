<?php
// ===----------------------------* PHP 5 *-------------------------------===//
//
// THIS FILE IS GENERATED BY INVAR. DO NOT EDIT !!!
//
// ===----------------------------------------------------------------------===//
namespace invar;

final class BinaryWriter {
	static public function writeInt08($v, &$bytes) {
		$bytes .= pack ( 'c', $v );
	}
	static public function writeInt16($v, &$bytes) {
		$bytes .= pack ( 'v', $v ); // big endian 'n'
	}
	static public function writeInt32($v, &$bytes) {
		$bytes .= pack ( 'V', $v ); // big endian 'N'
	}
	static public function writeUInt08($v, &$bytes) {
		$bytes .= pack ( 'c', $v );
	}
	static public function writeUInt16($v, &$bytes) {
		$bytes .= pack ( 'v', $v );
	}
	static public function writeUInt32($v, &$bytes) {
		$bytes .= pack ( 'V', $v );
	}
	static public function writeFloat32($v, &$bytes) {
		// float (machine dependent size and representation)
		$bytes .= pack ( 'f', $v );
	}
	static public function writeFloat64($v, &$bytes) {
		// double (machine dependent size and representation)
		$bytes .= pack ( 'd', $v );
	}
	static public function writeBoolean($v, &$bytes) {
		$bytes .= ($v == TRUE) ? "\x01" : "\x00";
	}
	static public function writeUTF($v, &$bytes) {
		$bytes .= pack ( 'VA*', strlen ( $v ), $v );
	}
	static public function writeInt64($v, &$bytes) {
		$hi = $v >> 32;
		$lo = $v & 0x00000000ffffffff;
		$bytes .= pack ( 'VV', $lo, $hi );
	}
	static public function writeUInt64($v, &$bytes) {
		$hi = $v >> 32;
		$lo = $v & 0x00000000ffffffff;
		$bytes .= pack ( 'VV', $lo, $hi );
	}
}
final class 

BinaryReader {
	private $data = NULL;
	private $bytes = NULL;
	private $bytesPos = 1;
	private $bytesLen = 0;
	function __construct(&$data) {
		if (is_null ( $data )) {
			throw new \Exception ( '$data is null' );
		}
		// $data The packed data.
		$this->data = $data;
		$this->bytes = unpack ( "C*", $data ); // index begin from 1.
		$this->bytesLen = count ( $this->bytes );
	}
	function __destruct() {
		// print "Destroying DataInput...\n";
		$this->bytes = NULL;
		$this->bytesLen = 0;
		$this->bytesPos = 1;
	}
	function checkAvailable($offset) {
		if ($this->bytesPos + $offset > $this->bytesLen + 1) {
			throw new \Exception ( 'EOF Error' );
		}
	}
	public function readInt08() {
		$this->checkAvailable ( 1 );
		$bits = $this->bytes [$this->bytesPos];
		$sign = ($bits >> 7) == 0 ? 1 : - 1;
		$this->bytesPos ++;
		return $sign < 0 ? ((~ ($bits - 1)) & 0xFF) * $sign : $bits;
	}
	public function readInt16() {
		$this->checkAvailable ( 2 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$sign = ($b2 >> 7) == 0 ? 1 : - 1;
		$bits = ($b2 << 8) | $b1;
		return $sign < 0 ? ((~ ($bits - 1)) & 0xFFFF) * $sign : $bits;
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
		$sign = ($b4 >> 7) == 0 ? 1 : - 1;
		$bits = ($b4 << 24) | ($b3 << 16) | ($b2 << 8) | $b1;
		return $sign < 0 ? (~ ($bits - 1)) * $sign : $bits;
	}
	public function readUInt08() {
		$this->checkAvailable ( 1 );
		$result = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return $result;
	}
	public function readUInt16() {
		$this->checkAvailable ( 2 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		$b2 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return ($b2 << 8) | $b1;
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
		return ($b4 << 24) | ($b3 << 16) | ($b2 << 8) | $b1;
	}
	public function readBoolean() {
		$this->checkAvailable ( 1 );
		$b1 = $this->bytes [$this->bytesPos];
		$this->bytesPos ++;
		return $b1 == 0 ? FALSE : TRUE;
	}
	public function readInt64() {
		$this->checkAvailable ( 8 );
		$packed = substr ( $this->data, $this->bytesPos - 1, 8 );
		list ( $lower, $higher ) = array_values ( unpack ( 'V2', $packed ) );
		$this->bytesPos += 8;
		return $higher << 32 | $lower;
	}
	public function readUInt64() {
		$this->checkAvailable ( 8 );
		$packed = substr ( $this->data, $this->bytesPos - 1, 8 );
		list ( $lower, $higher ) = array_values ( unpack ( 'V2', $packed ) );
		$this->bytesPos += 8;
		return $higher << 32 | $lower;
	}
	public function readUTF() {
		$len = $this->readInt32 ();
		$this->checkAvailable ( $len );
		$s = substr ( $this->data, $this->bytesPos - 1, $len );
		$a = unpack ( 'A*', $s );
		$this->bytesPos += $len;
		return $a [1];
	}
	public function readFloat32() {
		// IEEE 754-1985
		/* s（sign 1）；e（exponent 8）；m（mantissa 23）-127 */
		$bits = $this->readUInt32 ();
		$s = ($bits >> 31) == 0 ? 1 : - 1;
		$e = ($bits >> 23) & 0xff;
		$m23 = ($bits & 0x0007ffff);
		if ($m23 == 0) {
			if ($e == 0) { // +0, -0
				return 0;
			} else if ($e == 0x7f) { // +1, -1
				return 1 * $s;
			} else if ($e == 0xff) { // +∞, -∞
				return INF * $s;
			} else {
			}
		} else {
			if ($e == 0xff) { // NaN
				return NAN;
			}
			if ($e == 0) { // Denormalized
				$e = $e + 1;
			} else { // Normalized
			}
		}
		$m = 1.0;
		$val = 0.0;
		for($i = - 23; $i < 0; ++ $i) {
			$val = ($m23 & 0x01) == 0 ? 0 : pow ( 2, i );
			$m += $val;
			$m23 = $m23 >> 1;
		}
		return $s * $m * pow ( 2, $e - 127 );
		// $bits = $this->readUInt32 ();
		// $s = ($bits >> 31) == 0 ? 1 : - 1;
		// $e = ($bits >> 23) & 0xff;
		// $m = ($e == 0) ? ($bits & 0x007fffff) << 1 : ($bits & 0x007fffff) | 0x00800000;
		// return $s * $m * pow ( 2, $e - 150 );
	}
	public function readFloat64() {
		// IEEE 754-1985
		/* s（sign 1）；e（exponent 11）；m（mantissa 52）-1023 */
		$lo = $this->readUInt32 ();
		$hi = $this->readUInt32 ();
		$s = ($hi >> 31) == 0 ? 1 : - 1;
		$e = ($hi >> 20) & 0x07ff;
		$m20 = ($hi & 0x000fffff);
		$m32 = $lo;
		if ($e == 0 && $m20 == 0 && $m32 == 0) {
			return $s * 0;
		}
		if ($m20 == 0 && $m32 == 0) {
			if ($e == 0)
				return 0 * $s; // +0, -0
			else if ($e == 0x03ff)
				return 1 * $s; // +1, -1
		} else {
			if ($e == 0) { // Denormalized
				$e = $e + 1;
			} else { // Normalized
			}
		}
		$m = 1.0;
		$val = 0.0;
		for($i = - 32; $i < 0; ++ $i) {
			$val = ($m32 & 0x01) == 0 ? 0 : pow ( 2, i );
			$m += $val;
			$m32 = $m32 >> 1;
		}
		for($i = - 52; $i < - 32; ++ $i) {
			$val = ($m20 & 0x01) == 0 ? 0 : pow ( 2, i );
			$m += $val;
			$m20 = $m20 >> 1;
		}
		return $s * $m * pow ( 2, $e - 1023 );
	}
}