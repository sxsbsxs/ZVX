#include "subtitle_timeline.h"

#include <algorithm>
#include <cctype>
#include <cstdint>
#include <iomanip>
#include <limits>
#include <sstream>
#include <string>
#include <vector>

namespace zvx {
namespace {

bool parseUnsigned(const std::string& value, std::int64_t& output) {
    if (value.empty()) return false;
    std::int64_t parsed = 0;
    for (const unsigned char character : value) {
        if (!std::isdigit(character)) return false;
        const int digit = character - '0';
        if (parsed > (std::numeric_limits<std::int64_t>::max() - digit) / 10) return false;
        parsed = parsed * 10 + digit;
    }
    output = parsed;
    return true;
}

bool parseTimestamp(const std::string& token, std::int64_t& totalMs, bool& hasHours, char& decimal) {
    const auto decimalAt = token.find_first_of(".,");
    if (decimalAt == std::string::npos ||
        token.find_first_of(".,", decimalAt + 1) != std::string::npos) return false;

    const std::string clock = token.substr(0, decimalAt);
    const std::string fraction = token.substr(decimalAt + 1);
    if (fraction.size() != 3) return false;

    std::vector<std::string> fields;
    std::size_t begin = 0;
    while (true) {
        const auto colon = clock.find(':', begin);
        fields.push_back(clock.substr(begin, colon == std::string::npos ? colon : colon - begin));
        if (colon == std::string::npos) break;
        begin = colon + 1;
    }
    if (fields.size() != 2 && fields.size() != 3) return false;

    std::int64_t first = 0;
    std::int64_t minutes = 0;
    std::int64_t seconds = 0;
    std::int64_t millis = 0;
    if (!parseUnsigned(fields[0], first) ||
        !parseUnsigned(fields[1], minutes) ||
        !parseUnsigned(fields[fields.size() == 3 ? 2 : 1], seconds) ||
        !parseUnsigned(fraction, millis)) return false;

    hasHours = fields.size() == 3;
    if ((hasHours && minutes > 59) || seconds > 59 || millis > 999) return false;

    const std::int64_t hours = hasHours ? first : 0;
    const std::int64_t totalMinutes = hasHours ? minutes : first;
    constexpr std::int64_t kHourMs = 3'600'000;
    constexpr std::int64_t kMinuteMs = 60'000;
    if (hours > std::numeric_limits<std::int64_t>::max() / kHourMs) return false;
    const auto hourMs = hours * kHourMs;
    if (totalMinutes > (std::numeric_limits<std::int64_t>::max() - hourMs) / kMinuteMs) return false;
    const auto minuteMs = totalMinutes * kMinuteMs;
    if (seconds > (std::numeric_limits<std::int64_t>::max() - hourMs - minuteMs) / 1'000) return false;
    const auto secondMs = seconds * 1'000;
    const auto baseMs = hourMs + minuteMs + secondMs;
    if (millis > std::numeric_limits<std::int64_t>::max() - baseMs) return false;

    totalMs = baseMs + millis;
    decimal = token[decimalAt];
    return true;
}

std::string formatTimestamp(std::int64_t value, bool hasHours, char decimal) {
    const auto wholeSeconds = value / 1'000;
    const auto seconds = wholeSeconds % 60;
    const auto minutes = hasHours ? (wholeSeconds / 60) % 60 : wholeSeconds / 60;
    const auto hours = wholeSeconds / 3'600;
    const auto millis = value % 1'000;

    std::ostringstream output;
    output << std::setfill('0');
    if (hasHours) output << std::setw(2) << hours << ':';
    output << std::setw(2) << minutes << ':' << std::setw(2) << seconds
           << decimal << std::setw(3) << millis;
    return output.str();
}

std::size_t skipWhitespace(const std::string& line, std::size_t position) {
    while (position < line.size() && std::isspace(static_cast<unsigned char>(line[position]))) ++position;
    return position;
}

std::int64_t applyOffset(std::int64_t value, std::int32_t offsetMs) {
    const auto offset = static_cast<std::int64_t>(offsetMs);
    if (offset < 0 && value < -offset) return 0;
    if (offset > 0 && value > std::numeric_limits<std::int64_t>::max() - offset) {
        return std::numeric_limits<std::int64_t>::max();
    }
    return std::max<std::int64_t>(0, value + offset);
}

std::string shiftCueLine(const std::string& line, std::int32_t offsetMs) {
    const auto arrow = line.find("-->");
    if (arrow == std::string::npos) return line;

    const auto startBegin = skipWhitespace(line, 0);
    auto startEnd = arrow;
    while (startEnd > startBegin && std::isspace(static_cast<unsigned char>(line[startEnd - 1]))) --startEnd;
    const auto endBegin = skipWhitespace(line, arrow + 3);
    auto endEnd = endBegin;
    while (endEnd < line.size() && !std::isspace(static_cast<unsigned char>(line[endEnd]))) ++endEnd;
    if (startBegin >= startEnd || endBegin >= endEnd) return line;

    std::int64_t startMs = 0;
    std::int64_t endMs = 0;
    bool startHasHours = false;
    bool endHasHours = false;
    char startDecimal = '.';
    char endDecimal = '.';
    if (!parseTimestamp(line.substr(startBegin, startEnd - startBegin), startMs, startHasHours, startDecimal) ||
        !parseTimestamp(line.substr(endBegin, endEnd - endBegin), endMs, endHasHours, endDecimal)) {
        return line;
    }

    return line.substr(0, startBegin) +
           formatTimestamp(applyOffset(startMs, offsetMs), startHasHours, startDecimal) +
           line.substr(startEnd, endBegin - startEnd) +
           formatTimestamp(applyOffset(endMs, offsetMs), endHasHours, endDecimal) +
           line.substr(endEnd);
}

}  // namespace

std::string shiftSubtitleTimeline(const std::string& content, std::int32_t offsetMs) {
    if (offsetMs == 0 || content.empty()) return content;
    std::string output;
    output.reserve(content.size() + 32);
    std::size_t begin = 0;
    while (begin < content.size()) {
        const auto newline = content.find('\n', begin);
        const auto end = newline == std::string::npos ? content.size() : newline;
        output += shiftCueLine(content.substr(begin, end - begin), offsetMs);
        if (newline == std::string::npos) break;
        output.push_back('\n');
        begin = newline + 1;
    }
    return output;
}
}  // namespace zvx
