---
applyTo: "**/*.cpp, **/*.h, **/shaders/**"
---

# C++ / OpenGL ES 3.0 — Coding Conventions

## Target

- **OpenGL ES 3.0** only — do not use ES 3.1/3.2 features (compute shaders, SSBO, imageStore)
- NDK with CMake 3.22+
- GLSL `#version 300 es`

## Style

- Indentation: 4 spaces
- Naming: camelCase (local variables/functions), PascalCase (classes), m_ prefix (member variables)
- Max line length: 120
- Header guards: `#pragma once` or include guards

## OpenGL ES 3.0 Rules

- Always use `#version 300 es` in shaders — no legacy `attribute`/`varying`
- Use `in`/`out` qualifiers, `layout(location = N)` for attributes
- Texture sampling: `texture()` only — no `texture2D()` (ES 2.0 legacy)
- Framebuffer: use `glGenFramebuffers` / `glBindFramebuffer` — always check `glCheckFramebufferStatus`
- No compute shaders — use fragment shader passes for GPU compute (Jacobi, divergence, etc.)
- No SSBO — use textures as data buffers
- No `glDispatchCompute` — not available in ES 3.0
- MRT (Multiple Render Targets): supported via `gl_FragData` or multiple `out` variables (max 4)
- Instancing: `glDrawArraysInstanced` / `glDrawElementsInstanced` available
- Transform feedback: available but avoid unless necessary (perf varies on mobile)

## Shader Conventions (this project)

- Shaders embedded as C string literals in `.h` files (not separate `.glsl` files)
- Physics shaders: `app/src/main/cpp/shaders/physics/canonical/`
- PostFx shaders: `app/src/main/cpp/shaders/postfx/`
- Render shaders: `app/src/main/cpp/shaders/render/`
- Use `uniform` for per-frame data, `in`/`out` for vertex↔fragment communication
- Precision: `precision highp float;` as default — use `mediump` only for color/UV where acceptable

## FBO Management

- Use DoubleFBO (ping-pong) for iterative solvers (pressure Jacobi, advection)
- Always bind back to default FBO (0) after off-screen rendering
- Check `GL_FRAMEBUFFER_COMPLETE` after attaching textures
- Prefer `GL_RGBA16F` for simulation data (half-float), `GL_RGBA8` for display
- Use `FormatProbe` to detect device support before choosing formats

## Memory & Performance

- Delete GL resources in `destroy()` — never leak textures/FBOs/programs
- Batch uniform uploads — minimize `glUniform*` calls per frame
- Avoid `glReadPixels` in hot path (causes GPU→CPU sync stall)
- Use `glFinish()` only for debugging/profiling — never in production code
- Minimize state changes: sort draw calls by program/texture when possible

## JNI Bridge

- JNI functions in `native-lib.cpp` — keep thin (delegate to engine classes)
- Use `extern "C"` + `JNIEXPORT` for JNI entry points
- Never throw C++ exceptions across JNI boundary — catch and return error codes
- String conversion: `GetStringUTFChars` → use → `ReleaseStringUTFChars` (always release)
- Array access: `GetFloatArrayElements` or `GetFloatArrayRegion` (prefer Region for small arrays)

## Error Handling

- Check `glGetError()` in debug builds only (via `#ifndef NDEBUG`)
- Use `__android_log_print` for native logging (ANDROID_LOG_ERROR / ANDROID_LOG_DEBUG)
- Shader compile errors: always log `glGetShaderInfoLog` before returning failure
- Program link errors: always log `glGetProgramInfoLog`

## Anti-patterns

- ❌ Do not use `glGetError()` in release builds (performance drain)
- ❌ Do not allocate textures/FBOs per frame — allocate once, reuse
- ❌ Do not use `GL_FLOAT` textures without checking `OES_texture_float` (use `GL_RGBA16F` instead)
- ❌ Do not assume uniform locations are stable across recompiles — query or use `layout(location)`
- ❌ Do not use `discard` in fragment shaders unless absolutely necessary (breaks early-Z on mobile)
