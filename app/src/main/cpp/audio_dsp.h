#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>
namespace zvx {
enum AudioToneProfile : std::int32_t { kToneFlat = 0, kToneWarm = 1, kToneVocal = 2, kToneBright = 3 };
class AudioDsp {
public:
    AudioDsp(std::int32_t sampleRate, std::int32_t channelCount);
    void reset();
    void process(std::int16_t* interleavedSamples, std::size_t sampleCount, std::int32_t profile);
private:
    struct ChannelState { float low = 0.0F; float lowMid = 0.0F; };
    std::int32_t channelCount_;
    float lowAlpha_;
    float highAlpha_;
    std::vector<ChannelState> channelStates_;
};
}  // namespace zvx
