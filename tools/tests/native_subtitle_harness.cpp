#include "subtitle_timeline.h"

#include <cerrno>
#include <cstdint>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include <string>

int main(int argc, char** argv) {
    if (argc != 2) return 2;
    errno = 0;
    char* end = nullptr;
    const long parsed = std::strtol(argv[1], &end, 10);
    if (errno != 0 || end == argv[1] || *end != '\0' ||
        parsed < INT32_MIN || parsed > INT32_MAX) return 2;

    std::ostringstream input;
    input << std::cin.rdbuf();
    std::cout << zvx::shiftSubtitleTimeline(input.str(), static_cast<std::int32_t>(parsed));
    return 0;
}
