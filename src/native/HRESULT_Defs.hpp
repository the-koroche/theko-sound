/*
 * Copyright 2025-present Alex Soloviov (aka Theko)
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

#ifdef _WIN32
#include <windows.h>
#include <initguid.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <initguid.h>

#include <unordered_map>
#include <string>

/**
 * Return a human-readable constant name of the given HRESULT.
 * If no mapping is found, nullptr is returned.
 * @param hr The HRESULT to format.
 * @return A human-readable constant name for the given HRESULT,
 *         or nullptr if no mapping is found.
 * @note This function is thread-safe and does not allocate any memory.
 * @note This function is only available on Windows.
 */
static const char* GetHRESULTConstantName(HRESULT hr) {
    static const std::unordered_map<HRESULT, const char*> hr_names = {
        //  General - winerror.h
        {S_OK, "S_OK" },
        {S_FALSE, "S_FALSE" },
        {E_UNEXPECTED, "E_UNEXPECTED" },
        {E_NOTIMPL, "E_NOTIMPL" },
        {E_OUTOFMEMORY, "E_OUTOFMEMORY" },
        {E_INVALIDARG, "E_INVALIDARG" },
        {E_NOINTERFACE, "E_NOINTERFACE" },
        {E_POINTER, "E_POINTER" },
        {E_HANDLE, "E_HANDLE" },
        {E_ABORT, "E_ABORT" },
        {E_FAIL, "E_FAIL" },
        {E_ACCESSDENIED, "E_ACCESSDENIED" },
        {E_PENDING, "E_PENDING" },
        {E_NOTFOUND, "E_NOTFOUND" },

        //  WASAPI - audioclient.h
        {AUDCLNT_E_NOT_INITIALIZED, "AUDCLNT_E_NOT_INITIALIZED" },
        {AUDCLNT_E_ALREADY_INITIALIZED, "AUDCLNT_E_ALREADY_INITIALIZED" },
        {AUDCLNT_E_WRONG_ENDPOINT_TYPE, "AUDCLNT_E_WRONG_ENDPOINT_TYPE" },
        {AUDCLNT_E_DEVICE_INVALIDATED, "AUDCLNT_E_DEVICE_INVALIDATED" },
        {AUDCLNT_E_NOT_STOPPED, "AUDCLNT_E_NOT_STOPPED" },
        {AUDCLNT_E_BUFFER_TOO_LARGE, "AUDCLNT_E_BUFFER_TOO_LARGE" },
        {AUDCLNT_E_OUT_OF_ORDER, "AUDCLNT_E_OUT_OF_ORDER" },
        {AUDCLNT_E_UNSUPPORTED_FORMAT, "AUDCLNT_E_UNSUPPORTED_FORMAT" },
        {AUDCLNT_E_INVALID_SIZE, "AUDCLNT_E_INVALID_SIZE" },
        {AUDCLNT_E_DEVICE_IN_USE, "AUDCLNT_E_DEVICE_IN_USE" },
        {AUDCLNT_E_BUFFER_OPERATION_PENDING, "AUDCLNT_E_BUFFER_OPERATION_PENDING" },
        {AUDCLNT_E_THREAD_NOT_REGISTERED, "AUDCLNT_E_THREAD_NOT_REGISTERED" },
        {AUDCLNT_E_EXCLUSIVE_MODE_NOT_ALLOWED, "AUDCLNT_E_EXCLUSIVE_MODE_NOT_ALLOWED" },
        {AUDCLNT_E_ENDPOINT_CREATE_FAILED, "AUDCLNT_E_ENDPOINT_CREATE_FAILED" },
        {AUDCLNT_E_SERVICE_NOT_RUNNING, "AUDCLNT_E_SERVICE_NOT_RUNNING" },
        {AUDCLNT_E_EVENTHANDLE_NOT_EXPECTED, "AUDCLNT_E_EVENTHANDLE_NOT_EXPECTED" },
        {AUDCLNT_E_EXCLUSIVE_MODE_ONLY, "AUDCLNT_E_EXCLUSIVE_MODE_ONLY" },
        {AUDCLNT_E_BUFDURATION_PERIOD_NOT_EQUAL, "AUDCLNT_E_BUFDURATION_PERIOD_NOT_EQUAL" },
        {AUDCLNT_E_EVENTHANDLE_NOT_SET, "AUDCLNT_E_EVENTHANDLE_NOT_SET" },
        {AUDCLNT_E_INCORRECT_BUFFER_SIZE, "AUDCLNT_E_INCORRECT_BUFFER_SIZE" },
        {AUDCLNT_E_BUFFER_SIZE_ERROR, "AUDCLNT_E_BUFFER_SIZE_ERROR" },
        {AUDCLNT_E_CPUUSAGE_EXCEEDED, "AUDCLNT_E_CPUUSAGE_EXCEEDED" },
        {AUDCLNT_E_BUFFER_ERROR, "AUDCLNT_E_BUFFER_ERROR" },
        {AUDCLNT_E_BUFFER_SIZE_NOT_ALIGNED, "AUDCLNT_E_BUFFER_SIZE_NOT_ALIGNED" },
        {AUDCLNT_E_INVALID_DEVICE_PERIOD, "AUDCLNT_E_INVALID_DEVICE_PERIOD" },
        {AUDCLNT_E_INVALID_STREAM_FLAG, "AUDCLNT_E_INVALID_STREAM_FLAG" },
        {AUDCLNT_E_ENDPOINT_OFFLOAD_NOT_CAPABLE, "AUDCLNT_E_ENDPOINT_OFFLOAD_NOT_CAPABLE" },
        {AUDCLNT_E_OUT_OF_OFFLOAD_RESOURCES, "AUDCLNT_E_OUT_OF_OFFLOAD_RESOURCES" },
        {AUDCLNT_E_OFFLOAD_MODE_ONLY, "AUDCLNT_E_OFFLOAD_MODE_ONLY" },
        {AUDCLNT_E_NONOFFLOAD_MODE_ONLY, "AUDCLNT_E_NONOFFLOAD_MODE_ONLY" },
        {AUDCLNT_E_RESOURCES_INVALIDATED, "AUDCLNT_E_RESOURCES_INVALIDATED" },
        {AUDCLNT_E_RAW_MODE_UNSUPPORTED, "AUDCLNT_E_RAW_MODE_UNSUPPORTED" },
        {AUDCLNT_E_ENGINE_PERIODICITY_LOCKED, "AUDCLNT_E_ENGINE_PERIODICITY_LOCKED" },
        {AUDCLNT_E_ENGINE_FORMAT_LOCKED, "AUDCLNT_E_ENGINE_FORMAT_LOCKED" },

        {AUDCLNT_S_BUFFER_EMPTY, "AUDCLNT_S_BUFFER_EMPTY" },
        {AUDCLNT_S_THREAD_ALREADY_REGISTERED, "AUDCLNT_S_THREAD_ALREADY_REGISTERED" },
        {AUDCLNT_S_POSITION_STALLED, "AUDCLNT_S_POSITION_STALLED" }
    };

    auto it = hr_names.find(hr);
    if (it == hr_names.end()) {
        return nullptr;
    }
    return it->second;
}

#endif // _WIN32