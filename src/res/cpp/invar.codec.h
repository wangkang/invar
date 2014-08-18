
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
};

class BinaryReader
{
    public:
	std::int8_t   ReadByte() const;
	std::int16_t  ReadInt16() const;
	std::int32_t  ReadInt32() const;
	std::int64_t  ReadInt64() const;
	std::uint8_t  ReadUByte() const;
	std::uint16_t ReadUInt16() const;
	std::uint32_t ReadUInt32() const;
	std::uint64_t ReadUInt64() const;
    std::string   ReadString() const;
    bool          ReadBoolean() const;
    float         ReadSingle() const;
    double        ReadDouble() const;
};

template <typename T>
T* CheckSet (T *dest, const T *from)
{
	if (from != NULL && dest != NULL) {
		if (from != dest) {
			*dest = *from;
		}
	} else if (from != NULL && dest == NULL) {
        dest = new T(*from);
	} else if  (from == NULL && dest != NULL) {
		delete dest;
		dest = NULL;
	}
	else {
	}
	return dest;
}

template <typename T>
T* CheckSet (T *dest, T from)
{
	if (dest != NULL) {
		*dest = from;
	}
	else {
        dest = new T(from);
	}
	return dest;
}

template <typename T>
bool NewOrDel (T *dest, bool new_)
{
	if (new_ && dest == NULL) {
        dest = new T();
	} else if (!new_ && dest != NULL) {
		delete dest;
		dest = NULL;
	}
	else {
	}
	return dest != NULL;
}

}; //namespace:invar

#endif /* INVAR_CODEC_H_ */
