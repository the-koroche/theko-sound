PROJECT_DIR ?= $(CURDIR)

SRCS = \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_backend.cpp \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_output.cpp \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_input.cpp \
	$(PROJECT_DIR)/src/native/JNI_Entrypoints.cpp

INCLUDES = \
	-I $(PROJECT_DIR)/src/native \
	-I $(PROJECT_DIR)/src/native/cache \
	-I $(PROJECT_DIR)/src/native/backends \
	-I $(PROJECT_DIR)/src/native/backends/wasapi \
	-I "$(JAVA_HOME)/include" \
	-I "$(JAVA_HOME)/include/win32"

COMMON_FLAGS = -shared -static-libgcc -static-libstdc++ \
	-Os -s -fno-rtti -fno-exceptions -ffunction-sections -fdata-sections \
	-D_WIN32_WINNT=0x0601 -DWINVER=0x0601

LIBS = -lole32 -loleaut32 -lavrt -static -lpthread

OUTDIR = $(PROJECT_DIR)/src/main/resources/native
OUT64  = $(OUTDIR)/WASApiShrd64.dll
OUT32  = $(OUTDIR)/WASApiShrd32.dll

ARCH ?= x64

ifeq ($(ARCH),x64)
  CXX = x86_64-w64-mingw32-g++
  OUT = $(OUT64)
endif

ifeq ($(ARCH),x86)
  CXX = i686-w64-mingw32-g++
  OUT = $(OUT32)
endif

# Build
all: x64 x86

x64:
	$(MAKE) ARCH=x64 build

x86:
	$(MAKE) ARCH=x86 build

build: $(OUT)

$(OUT): $(SRCS)
	@if not exist "$(OUTDIR)" mkdir "$(OUTDIR)"
	$(CXX) $(COMMON_FLAGS) $(INCLUDES) -o $@ $^ $(LIBS)

clean:
	rm -f $(OUT64) $(OUT32) $(OUTARM)