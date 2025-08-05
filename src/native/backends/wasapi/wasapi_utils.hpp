#pragma once

#ifdef _WIN32
#include <windows.h>
#include <initguid.h>
#include <mmdeviceapi.h>
#include <audioclient.h>
#include <functiondiscoverykeys.h>
#include <Functiondiscoverykeys_devpkey.h>
#include <cstdio>

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

static IMMDeviceCollection* getDevicesList(IMMDeviceEnumerator* pEnum, EDataFlow flow) {
    IMMDeviceCollection* pCollection = nullptr;
    HRESULT hr = pEnum->EnumAudioEndpoints(
        flow,
        DEVICE_STATEMASK_ALL,
        &pCollection
    );
    return SUCCEEDED(hr) ? pCollection : nullptr;
}

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