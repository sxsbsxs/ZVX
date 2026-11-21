#include "audio_dsp.h"
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <iostream>
#include <iterator>
#include <vector>

int main(int argc, char** argv) {
    if (argc != 4) return 2;
    const int sample_rate = std::atoi(argv[1]);
    const int channels = std::atoi(argv[2]);
    const int profile = std::atoi(argv[3]);
    if (sample_rate < 8'000 || sample_rate > 384'000 || channels < 1 || channels > 8) return 2;
    std::istreambuf_iterator<char> input(std::cin);
    const std::istreambuf_iterator<char> end;
    const std::vector<char> bytes(input, end);
    if ((bytes.size() % sizeof(std::int16_t)) != 0U) return 2;
    std::vector<std::int16_t> samples(bytes.size() / sizeof(std::int16_t));
    if (!bytes.empty()) std::memcpy(samples.data(), bytes.data(), bytes.size());
    if (samples.size() % static_cast<std::size_t>(channels) != 0U) return 2;
    zvx::AudioDsp processor(sample_rate, channels);
    processor.process(samples.data(), samples.size(), profile);
    if (!samples.empty()) {
        std::cout.write(reinterpret_cast<const char*>(samples.data()),
                        static_cast<std::streamsize>(samples.size() * sizeof(std::int16_t)));
    }
    return std::cout.good() ? 0 : 3;
}
