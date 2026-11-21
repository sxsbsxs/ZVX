#include "audio_dsp.h"
#include <algorithm>
#include <cmath>
#include <cstdint>
namespace zvx {
namespace {
constexpr float kPi = 3.14159265358979323846F;
float lowPassAlpha(float frequencyHz, float sampleRate) {
    const float safeFrequency = std::clamp(frequencyHz, 20.0F, sampleRate * 0.42F);
    return 1.0F - std::exp((-2.0F * kPi * safeFrequency) / sampleRate);
}
struct ToneGains { float low; float mid; float high; };
ToneGains gainsForProfile(std::int32_t profile) {
    switch (profile) {
        case kToneWarm: return {1.24F, 0.98F, 0.82F};
        case kToneVocal: return {0.82F, 1.22F, 1.02F};
        case kToneBright: return {0.90F, 1.00F, 1.24F};
        case kToneFlat:
        default: return {1.0F, 1.0F, 1.0F};
    }
}
}  // namespace
AudioDsp::AudioDsp(std::int32_t sampleRate, std::int32_t channelCount)
    : channelCount_(channelCount),
      lowAlpha_(lowPassAlpha(240.0F, static_cast<float>(sampleRate))),
      highAlpha_(lowPassAlpha(3'600.0F, static_cast<float>(sampleRate))),
      channelStates_(static_cast<std::size_t>(channelCount)) {}
void AudioDsp::reset() {
    std::fill(channelStates_.begin(), channelStates_.end(), ChannelState{});
}
void AudioDsp::process(std::int16_t* samples, std::size_t sampleCount, std::int32_t profile) {
    if (samples == nullptr || sampleCount == 0U || channelCount_ <= 0 ||
        sampleCount % static_cast<std::size_t>(channelCount_) != 0U || profile == kToneFlat) return;
    const ToneGains gains = gainsForProfile(profile);
    for (std::size_t index = 0; index < sampleCount; ++index) {
        ChannelState& state = channelStates_[index % static_cast<std::size_t>(channelCount_)];
        const float input = static_cast<float>(samples[index]) / 32768.0F;
        state.low += lowAlpha_ * (input - state.low);
        state.lowMid += highAlpha_ * (input - state.lowMid);
        const float lowBand = state.low;
        const float midBand = state.lowMid - state.low;
        const float highBand = input - state.lowMid;
        const float output = lowBand * gains.low + midBand * gains.mid + highBand * gains.high;
        const float bounded = std::clamp(output, -1.0F, 0.999969482421875F);
        const auto quantized = static_cast<std::int32_t>(std::lround(bounded * 32768.0F));
        samples[index] = static_cast<std::int16_t>(std::clamp(quantized, -32768, 32767));
    }
}
}  // namespace zvx
