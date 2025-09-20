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
#include <cstdio>

/**
 * Retrieves the IMMDeviceEnumerator COM object responsible for
 * enumerating audio endpoints (e.g. speakers, microphones) on the
 * system.
 * 
 * The returned pointer is owned by the caller and should be released
 * when it is no longer needed.
 * 
 * @return A pointer to the IMMDeviceEnumerator object, or nullptr if
 * the call fails.
 */
static IMMDeviceEnumerator* getDeviceEnumerator() {
    IMMDeviceEnumerator* pEnum = nullptr;
    HRESULT hr = CoCreateInstance(
        __uuidof(MMDeviceEnumerator),
        nullptr,
        CLSCTX_ALL,
        __uuidof(IMMDeviceEnumerator),
        (void**)&pEnum
    );
    return SUCCEEDED(hr) ? pEnum : nullptr;
}

/**
 * Retrieves a list of audio endpoints (e.g. speakers, microphones) on
 * the system based on the specified data flow (input or output).
 * 
 * The returned pointer is owned by the caller and should be released
 * when it is no longer needed.
 * 
 * @param pEnum The IMMDeviceEnumerator object responsible for
 *        enumerating audio endpoints on the system.
 * @param flow The data flow type to filter audio endpoints by.
 * @return A pointer to the IMMDeviceCollection object containing the
 *         filtered list of audio endpoints, or nullptr if the call fails.
 */
static IMMDeviceCollection* getDevicesList(IMMDeviceEnumerator* pEnum, EDataFlow flow) {
    IMMDeviceCollection* pCollection = nullptr;
    HRESULT hr = pEnum->EnumAudioEndpoints(
        flow,
        DEVICE_STATEMASK_ALL,
        &pCollection
    );
    return SUCCEEDED(hr) ? pCollection : nullptr;
}

/**
 * Retrieves the WAVEFORMATEX object containing the audio format information
 * associated with the specified audio device.
 * 
 * The returned pointer is owned by the caller and should be released
 * when it is no longer needed using CoTaskMemFree(format).
 * 
 * @param device The IMMDevice object associated with the audio device
 *        to retrieve the audio format information from.
 * @return A pointer to the WAVEFORMATEX object containing the audio format
 *         information associated with the specified audio device, or nullptr if the
 *         call fails.
 */
static WAVEFORMATEX* getMixFormat(IMMDevice* device) {
    if (!device) return nullptr;

    IAudioClient* audioClient = nullptr;
    HRESULT hr = device->Activate(__uuidof(IAudioClient), CLSCTX_ALL, nullptr, (void**)&audioClient);
    if (FAILED(hr) || !audioClient) return nullptr;

    WAVEFORMATEX* format = nullptr;
    hr = audioClient->GetMixFormat(&format);
    audioClient->Release();

    if (FAILED(hr)) {
        if (format) CoTaskMemFree(format);
        return nullptr;
    }

    return format; // Caller need to free CoTaskMemFree(format)
}
#endif // _WIN32