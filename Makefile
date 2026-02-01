PROJECT_DIR ?= $(CURDIR)

# Compiler settings
CXX = g++
CXXFLAGS = -Wall -Wextra -shared -static-libgcc -static-libstdc++ \
	-Os -s -fno-rtti -fno-exceptions -ffunction-sections -fdata-sections \
	-D_WIN32_WINNT=0x0601 -DWINVER=0x0601

LIBS = -lole32 -loleaut32 -lavrt -static -lpthread

INCLUDES = \
	-I $(PROJECT_DIR)/src/native \
	-I $(PROJECT_DIR)/src/native/cache \
	-I $(PROJECT_DIR)/src/native/backends \
	-I $(PROJECT_DIR)/src/native/backends/wasapi \
	-I "$(JAVA_HOME)/include" \
	-I "$(JAVA_HOME)/include/win32"

# Sources
SRCS = \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_backend.cpp \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_output.cpp \
	$(PROJECT_DIR)/src/native/backends/wasapi/wasapi_shared_input.cpp \
	$(PROJECT_DIR)/src/native/JNI_Entrypoints.cpp

# Output
OUT = $(PROJECT_DIR)/src/main/resources/native/WASApiShrd64.dll

# Rules
all: $(OUT)

$(OUT): $(SRCS)
	$(CXX) $(CXXFLAGS) $(INCLUDES) -o $@ $^ $(LIBS)

clean:
ifeq ($(OS),Windows_NT)
	del /Q "$(subst /,\,$(OUT))" 2>nul
else
	rm -f $(OUT)
endif
