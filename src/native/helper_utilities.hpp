/*
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
#include <cstdio>

#include <jni.h>
#include <stringapiset.h>

#ifdef _WIN32
#include <combaseapi.h> // CoTaskMemAlloc / CoTaskMemFree
#include "winhr_defs.hpp"
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

static char* memalloc_literal_utf8(const char* str) {
    if (!str) return NULL;
    size_t len = strlen(str) + 1;
    char* result = (char*)malloc(len);
    strcpy_s(result, len, str);
    return result; // caller must free using 'free'
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

static char* com_memalloc_literal_utf8(const char* str) {
    if (!str) return NULL;
    size_t len = strlen(str) + 1;
    char* result = (char*)CoTaskMemAlloc(len);
    strcpy_s(result, len, str);
    return result; // caller must free using 'CoTaskMemFree'
}

static wchar_t* memalloc_literal_utf16(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)malloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'free'
}

static wchar_t* com_memalloc_literal_utf16(const wchar_t* str) {
    if (!str) return NULL;
    size_t len = wcslen(str) + 1;
    wchar_t* result = (wchar_t*)CoTaskMemAlloc(len * sizeof(wchar_t));
    wcscpy_s(result, len, str);
    return result; // caller must free using 'CoTaskMemFree'
}

static char* format_hr_msg(HRESULT hr) {
    const char* msgBuf = get_hr_name(hr);
    char* result = format("%s (HRESULT: 0x%08X)", msgBuf, hr);
    return result; // caller must free using 'free'
}
#endif