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

std::int8_t   BinaryReader::ReadByte() const
{
	return 0;
}
std::int16_t  BinaryReader::ReadInt16() const
{
	return 0;
}
std::int32_t  BinaryReader::ReadInt32() const
{
	return 0;
}
std::int64_t  BinaryReader::ReadInt64() const
{
	return 0;
}
std::uint8_t  BinaryReader::ReadUByte() const
{
	return 0;
}
std::uint16_t BinaryReader::ReadUInt16() const
{
	return 0;
}
std::uint32_t BinaryReader::ReadUInt32() const
{
	return 0;
}
std::uint64_t BinaryReader::ReadUInt64() const
{
	return 0;
}
std::string   BinaryReader::ReadString() const
{
	return 0;
}
bool   BinaryReader::ReadBoolean() const
{
	return 0;
}
float  BinaryReader::ReadSingle() const
{
	return 0;
}
double BinaryReader::ReadDouble() const
{
	return 0;
}

} //namespace invar
