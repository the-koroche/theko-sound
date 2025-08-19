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

static const char* get_hr_name(HRESULT hr) {
    static const std::unordered_map<HRESULT, const char*> hr_names = {
        //  General — winerror.h
        {S_OK, "S_OK"},
        {S_FALSE, "S_FALSE"},
        {E_UNEXPECTED, "E_UNEXPECTED"},
        {E_NOTIMPL, "E_NOTIMPL"},
        {E_OUTOFMEMORY, "E_OUTOFMEMORY"},
        {E_INVALIDARG, "E_INVALIDARG"},
        {E_NOINTERFACE, "E_NOINTERFACE"},
        {E_POINTER, "E_POINTER"},
        {E_HANDLE, "E_HANDLE"},
        {E_ABORT, "E_ABORT"},
        {E_FAIL, "E_FAIL"},
        {E_ACCESSDENIED, "E_ACCESSDENIED"},
        {E_PENDING, "E_PENDING"},
        {E_NOTFOUND, "E_NOTFOUND"},

        //  WASAPI - audioclient.h
        {0x88890001, "AUDCLNT_E_NOT_INITIALIZED"},
        {0x88890002, "AUDCLNT_E_ALREADY_INITIALIZED"},
        {0x88890003, "AUDCLNT_E_WRONG_ENDPOINT_TYPE"},
        {0x88890004, "AUDCLNT_E_DEVICE_INVALIDATED"},
        {0x88890005, "AUDCLNT_E_NOT_STOPPED"},
        {0x88890006, "AUDCLNT_E_BUFFER_TOO_LARGE"},
        {0x88890007, "AUDCLNT_E_OUT_OF_ORDER"},
        {0x88890008, "AUDCLNT_E_UNSUPPORTED_FORMAT"},
        {0x88890009, "AUDCLNT_E_INVALID_SIZE"},
        {0x8889000A, "AUDCLNT_E_DEVICE_IN_USE"},
        {0x8889000B, "AUDCLNT_E_BUFFER_OPERATION_PENDING"},
        {0x8889000C, "AUDCLNT_E_THREAD_NOT_REGISTERED"},
        {0x8889000E, "AUDCLNT_E_EXCLUSIVE_MODE_NOT_ALLOWED"},
        {0x8889000F, "AUDCLNT_E_ENDPOINT_CREATE_FAILED"},
        {0x88890010, "AUDCLNT_E_SERVICE_NOT_RUNNING"},
        {0x88890011, "AUDCLNT_E_EVENTHANDLE_NOT_EXPECTED"},
        {0x88890012, "AUDCLNT_E_EXCLUSIVE_MODE_ONLY"},
        {0x88890013, "AUDCLNT_E_BUFDURATION_PERIOD_NOT_EQUAL"},
        {0x88890014, "AUDCLNT_E_EVENTHANDLE_NOT_SET"},
        {0x88890015, "AUDCLNT_E_INCORRECT_BUFFER_SIZE"},
        {0x88890016, "AUDCLNT_E_BUFFER_SIZE_ERROR"},
        {0x88890017, "AUDCLNT_E_CPUUSAGE_EXCEEDED"},
        {0x88890018, "AUDCLNT_E_BUFFER_ERROR"},
        {0x88890019, "AUDCLNT_E_BUFFER_SIZE_NOT_ALIGNED"},
        {0x88890020, "AUDCLNT_E_INVALID_DEVICE_PERIOD"},
        {0x88890021, "AUDCLNT_E_INVALID_STREAM_FLAG"},
        {0x88890022, "AUDCLNT_E_ENDPOINT_OFFLOAD_NOT_CAPABLE"},
        {0x88890023, "AUDCLNT_E_OUT_OF_OFFLOAD_RESOURCES"},
        {0x88890024, "AUDCLNT_E_OFFLOAD_MODE_ONLY"},
        {0x88890025, "AUDCLNT_E_NONOFFLOAD_MODE_ONLY"},
        {0x88890026, "AUDCLNT_E_RESOURCES_INVALIDATED"},
        {0x88890027, "AUDCLNT_E_RAW_MODE_UNSUPPORTED"},
        {0x88890028, "AUDCLNT_E_ENGINE_PERIODICITY_LOCKED"},
        {0x88890029, "AUDCLNT_E_ENGINE_FORMAT_LOCKED"},
        {0x88890030, "AUDCLNT_E_HEADTRACKING_ENABLED"},
        {0x88890040, "AUDCLNT_E_HEADTRACKING_UNSUPPORTED"},

        //  WASAPI - success codes
        {0x088890001, "AUDCLNT_S_BUFFER_EMPTY"},
        {0x088890002, "AUDCLNT_S_THREAD_ALREADY_REGISTERED"},
        {0x088890003, "AUDCLNT_S_POSITION_STALLED"}
    };

    auto it = hr_names.find(hr);
    if (it == hr_names.end()) {
        return nullptr;
    }
    return it->second;
}

#endif // _WIN32