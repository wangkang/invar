
#ifndef INVAR_CODEC_H_
#define INVAR_CODEC_H_

#include <cstdint>
#include <string>

namespace invar {

class BinaryWriter
{
    public:
	BinaryWriter& Write(std::int8_t value);
	BinaryWriter& Write(std::int16_t value);
	BinaryWriter& Write(std::int32_t value);
	BinaryWriter& Write(std::int64_t value);
	BinaryWriter& Write(std::uint8_t value);
	BinaryWriter& Write(std::uint16_t value);
	BinaryWriter& Write(std::uint32_t value);
	BinaryWriter& Write(std::uint64_t value);
	BinaryWriter& Write(bool value);
	BinaryWriter& Write(float value);
	BinaryWriter& Write(double value);
	BinaryWriter& Write(const std::string& value);

	bool HasError();
	void OnError(std:string hint);
};

class BinaryReader
{
    public:
	std::int8_t   ReadSByte();
	std::int16_t  ReadInt16();
	std::int32_t  ReadInt32();
	std::int64_t  ReadInt64();
	std::uint8_t  ReadByte();
	std::uint16_t ReadUInt16();
	std::uint32_t ReadUInt32();
	std::uint64_t ReadUInt64();
    std::string   ReadString();
    bool   ReadBoolean();
    float  ReadSingle();
    double ReadDouble();

    bool HasError();
    void OnError(std:string hint);
};

}; //namespace:invar

#endif /* INVAR_CODEC_H_ */
