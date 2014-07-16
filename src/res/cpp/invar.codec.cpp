/*

 * invar.codec.cpp
 *
 *  Created on: Jun 30, 2014
 *      Author: wangkang
 */

#include "invar.codec.h"

namespace invar {

using namespace invar;
using namespace std;

BinaryWriter& BinaryWriter::Write(std::int8_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::int16_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::int32_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::int64_t value)
{
	return *this;
}

BinaryWriter& BinaryWriter::Write(std::uint8_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::uint16_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::uint32_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(std::uint64_t value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(bool value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(float value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(double value)
{
	return *this;
}
BinaryWriter& BinaryWriter::Write(const std::string& value)
{
	return *this;
}

std::int8_t   BinaryReader::ReadByte()
{
	return 0;
}
std::int16_t  BinaryReader::ReadInt16()
{
	return 0;
}
std::int32_t  BinaryReader::ReadInt32()
{
	return 0;
}
std::int64_t  BinaryReader::ReadInt64()
{
	return 0;
}
std::uint8_t  BinaryReader::ReadUByte()
{
	return 0;
}
std::uint16_t BinaryReader::ReadUInt16()
{
	return 0;
}
std::uint32_t BinaryReader::ReadUInt32()
{
	return 0;
}
std::uint64_t BinaryReader::ReadUInt64(){
	return 0;
}
std::string   BinaryReader::ReadString()
{
	return 0;
}
bool   BinaryReader::ReadBoolean()
{
	return 0;
}
float  BinaryReader::ReadSingle()
{
	return 0;
}
double BinaryReader::ReadDouble()
{
	return 0;
}

} //namespace invar
