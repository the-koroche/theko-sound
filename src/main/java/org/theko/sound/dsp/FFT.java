package org.theko.sound.dsp;

public class FFT {
    private FFT () {
    }

    // Прямое быстрое преобразование Фурье (FFT)
    public static void fft(float[] real, float[] imag) {
        int n = real.length;
        int logN = Integer.numberOfTrailingZeros(n); // log2(n)

        // Битово-инверсное переставление
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - logN);
            if (i < j) {
                swap(real, i, j);
                swap(imag, i, j);
            }
        }

        // Итеративное выполнение FFT
        for (int size = 2; size <= n; size *= 2) {
            int halfSize = size / 2;
            float wAngle = (float) (-2 * Math.PI / size);
            float wReal = (float) Math.cos(wAngle);
            float wImag = (float) Math.sin(wAngle);

            for (int start = 0; start < n; start += size) {
                float uReal = 1.0f;
                float uImag = 0.0f;

                for (int i = 0; i < halfSize; i++) {
                    int evenIndex = start + i;
                    int oddIndex = start + i + halfSize;

                    float tReal = uReal * real[oddIndex] - uImag * imag[oddIndex];
                    float tImag = uReal * imag[oddIndex] + uImag * real[oddIndex];

                    real[oddIndex] = real[evenIndex] - tReal;
                    imag[oddIndex] = imag[evenIndex] - tImag;
                    real[evenIndex] += tReal;
                    imag[evenIndex] += tImag;

                    float tmpReal = uReal * wReal - uImag * wImag;
                    uImag = uReal * wImag + uImag * wReal;
                    uReal = tmpReal;
                }
            }
        }
    }

    public static void ifft(float[] real, float[] imag) {
        int n = real.length;

        // Инвертируем мнимую часть
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }

        fft(real, imag);

        // Делим на N и снова инвертируем мнимую часть
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] /= n;
            imag[i] = -imag[i];
        }
    }

    private static void swap(float[] arr, int i, int j) {
        float temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
