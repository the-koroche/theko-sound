#pragma once

#include <string.h>
#include <stdarg.h>
#include <malloc.h>
#include <cstdio>

#include <jni.h>
#include <stringapiset.h>

#ifdef _WIN32
#include <combaseapi.h> // CoTaskMemAlloc / CoTaskMemFree
#endif

static char* format(const char* fmt, ...) {
    va_list args;
    va_start(args, fmt);
    va_list args_copy;
    va_copy(args_copy, args);
    int len = vsnprintf(NULL, 0, fmt, args_copy);
    if (len < 0) return NULL;
    va_end(args_copy);

    char* buf = (char*)malloc(len + 1);
    if (!buf) {
        va_end(args);
        return NULL;
    }

    vsnprintf(buf, len + 1, fmt, args);
    va_end(args);
    return buf; // caller must free using 'free'
}

static char* formatv(const char* fmt, va_list args) {
    va_list args_copy;
    va_copy(args_copy, args);
    int len = vsnprintf(NULL, 0, fmt, args_copy);
    va_end(args_copy);
    if (len < 0) return NULL;

    char* buf = (char*)malloc(len + 1);
    if (!buf) return NULL;

    vsnprintf(buf, len + 1, fmt, args);
    return buf; // caller must free using 'free'
}

#ifdef _WIN32
static wchar_t* utf8_to_utf16(const char* utf8) {
    if (!utf8) return NULL;

    int len = MultiByteToWideChar(CP_UTF8, 0, utf8, -1, NULL, 0);
    if (len <= 0) return NULL;

    wchar_t* utf16 = (wchar_t*)malloc(len * sizeof(wchar_t));
    if (!utf16) return NULL;

    MultiByteToWideChar(CP_UTF8, 0, utf8, -1, utf16, len);
    return utf16; // caller must free using 'free'
}

static char* utf16_to_utf8(const wchar_t* utf16) {
    if (!utf16) return NULL;

    int len = WideCharToMultiByte(CP_UTF8, 0, utf16, -1, NULL, 0, NULL, NULL);
    if (len <= 0) return NULL;

    char* utf8 = (char*)malloc(len);
    if (!utf8) return NULL;

    WideCharToMultiByte(CP_UTF8, 0, utf16, -1, utf8, len, NULL, NULL);
    return utf8; // caller must free using 'free'
}
#endif

static wchar_t* memalloc_literal(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)malloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'free'
}

#ifdef _WIN32
static wchar_t* com_memalloc_literal(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)CoTaskMemAlloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'CoTaskMemFree'
}
#endif