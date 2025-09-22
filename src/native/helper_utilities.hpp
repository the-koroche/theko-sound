/**
 * Copyright 2025 Alex Soloviov (aka Theko)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <string.h>
#include <stdarg.h>
#include <malloc.h>
#include <array>
#include <cstdio>
#include <vector>
#include <inttypes.h>

#ifdef _WIN32
#include <combaseapi.h> // CoTaskMemAlloc / CoTaskMemFree
#include "HRESULT_Defs.hpp"
#endif

class PtrStr {
    std::array<char, 2 + sizeof(uintptr_t)*2 + 1> buf;
    
public:
    explicit PtrStr(const void* p) {
        snprintf(buf.data(), buf.size(), "0x%0*" PRIXPTR, 
                static_cast<int>(sizeof(uintptr_t)*2), 
                reinterpret_cast<uintptr_t>(p));
    }
    
    const char* c_str() const { return buf.data(); }
    operator const char*() const { return buf.data(); }
};

#define FORMAT_PTR(p) PtrStr(p)

/**
 * Format a string using va_list and return the result as a std::string.
 * If the underlying vsnprintf call fails (returns a negative value), an empty string is returned.
 * @param fmt The format string to use.
 * @param ... The arguments to pass to vsnprintf.
 * @return The formatted string, or an empty string if vsnprintf fails.
 */
static std::string format(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);

    va_list args_copy;
    va_copy(args_copy, args);
    int len = vsnprintf(nullptr, 0, fmt, args_copy);
    va_end(args_copy);

    if (len < 0) {
        va_end(args);
        return {};
    }
 
    std::vector<char> buf(len + 1);
    vsnprintf(buf.data(), buf.size(), fmt, args);
    va_end(args);

    return std::string(buf.data(), len);
}

/**
 * Format a string using va_list and return the result as a std::string.
 * If the underlying vsnprintf call fails (returns a negative value), an empty string is returned.
 * @param fmt The format string to use.
 * @param args The arguments to pass to vsnprintf.
 * @return The formatted string, or an empty string if vsnprintf fails.
 */
static std::string formatv(const char* fmt, va_list args) {
    va_list args_copy;
    va_copy(args_copy, args);

    int len = vsnprintf(nullptr, 0, fmt, args_copy);
    va_end(args_copy);

    if (len < 0) return {};

    std::vector<char> buf(len + 1);

    va_copy(args_copy, args);
    vsnprintf(buf.data(), buf.size(), fmt, args_copy);
    va_end(args_copy);

    return std::string(buf.data(), len);
}

/**
 * Allocate a buffer to store a literal UTF-8 string and copy the contents of the given string into it.
 * The caller is responsible for freeing the returned buffer using 'free()'
 * @param str The string to copy into the allocated buffer.
 * @return A pointer to the allocated buffer containing a copy of the given string, or NULL if an error occurs.
 */
static char* memalloc_literal_utf8(const char* str) {
    if (!str) return NULL;
    size_t len = strlen(str) + 1;
    char* result = (char*)malloc(len);
    strcpy_s(result, len, str);
    return result; // caller must free using 'free()'
}

#ifdef _WIN32
/**
 * Allocate a buffer to store a literal UTF-16 string and copy the contents of the given UTF-8 string into it.
 * The caller is responsible for freeing the returned buffer using 'free()'
 * @param utf8 The UTF-8 string to copy into the allocated buffer.
 * @return A pointer to the allocated buffer containing a copy of the given string, or NULL if an error occurs.
 */
static wchar_t* utf8_to_utf16(const char* utf8) {
    if (!utf8) return NULL;

    int len = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
    if (len <= 0) return NULL;

    wchar_t* utf16 = (wchar_t*)malloc(len * sizeof(wchar_t));
    if (!utf16) return NULL;

    MultiByteToWideChar(CP_UTF8, 0, utf8, -1, utf16, len);
    return utf16; // caller must free using 'free()'
}

/**
 * Converts a UTF-16 string to a UTF-8 string and returns it.
 * If the input string is null, an empty string is returned.
 * The returned string is guaranteed to be null-terminated.
 * If the conversion fails, an empty string is returned.
 * @param utf16 The UTF-16 string to convert.
 * @return The converted UTF-8 string, or an empty string if the conversion fails.
 */
static std::string utf16_to_utf8(const wchar_t* utf16) {
    if (!utf16) return {};

    int len = WideCharToMultiByte(CP_UTF8, 0, utf16, -1, nullptr, 0, nullptr, nullptr);
    if (len <= 0) return {};

    std::string result(len, 0);
    WideCharToMultiByte(CP_UTF8, 0, utf16, -1, result.data(), len, nullptr, nullptr);

    if (!result.empty() && result.back() == '\0') result.pop_back();

    return result;
}

/**
 * Allocate a buffer to store a literal UTF-8 string and copy the contents of the given string into it.
 * The caller is responsible for freeing the returned buffer using 'CoTaskMemFree()'
 * @param str The string to copy into the allocated buffer.
 * @return A pointer to the allocated buffer containing a copy of the given string, or NULL if an error occurs.
 */
static char* com_memalloc_literal_utf8(const char* str) {
    if (!str) return NULL;
    size_t len = strlen(str) + 1;
    char* result = (char*)CoTaskMemAlloc(len);
    strcpy_s(result, len, str);
    return result; // caller must free using 'CoTaskMemFree()'
}

/**
 * Allocate a buffer to store a literal UTF-16 string and copy the contents of the given string into it.
 * The caller is responsible for freeing the returned buffer using 'free()'
 * @param str The string to copy into the allocated buffer.
 * @return A pointer to the allocated buffer containing a copy of the given string, or NULL if an error occurs.
 */
static wchar_t* memalloc_literal_utf16(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)malloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'free()'
}

/**
 * Allocate a buffer to store a literal UTF-16 string and copy the contents of the given string into it.
 * The caller is responsible for freeing the returned buffer using 'CoTaskMemFree()'
 * @param str The string to copy into the allocated buffer.
 * @return A pointer to the allocated buffer containing a copy of the given string, or NULL if an error occurs.
 */
static wchar_t* com_memalloc_literal_utf16(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)CoTaskMemAlloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'CoTaskMemFree()'
}

/**
 * Formats a human-readable string for the given HRESULT.
 * If no mapping is found, nullptr is returned.
 * @param hr The HRESULT to format.
 * @return A human-readable string for the given HRESULT, or nullptr if no mapping is found.
 * @note This function is thread-safe and does not allocate any memory.
 * @note This function is only available on Windows.
 */
static std::string formatHRMessage(HRESULT hr) {
    const char* msgBuf = GetHRESULTConstantName(hr);
    return format("%s (HRESULT: 0x%08X)", msgBuf, hr);
} 
#endif