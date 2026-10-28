# Windows development and agent tooling

Observed while implementing and verifying beam me up on native Windows, using PowerShell,
Git for Windows, `just`, and Codex command tools. These are environment-specific findings,
not a claim that every Windows installation has the same failures. Workarounds below were
used in that session unless marked as a proposed fix.

## Shell and launcher boundaries

### `just ui-dev` needs `cygpath` on PATH

The shebang recipe failed before starting the app:

```text
could not find `cygpath` executable to translate recipe `ui-dev` shebang interpreter path
```

Git for Windows was installed, but its `usr\bin` directory was absent from the command's
PATH. Adding it for the current PowerShell process made the recipe work:

```powershell
# Adjust this path if Git is installed elsewhere. This does not change the system PATH.
$env:Path = 'C:\Program Files\Git\usr\bin;' + $env:Path
just ui-dev 8187 5187
```

Use free ports belonging to this checkout. After the browser check, verify that its server
and Vite processes have stopped; terminating a launcher alone can leave children running.
If cleanup needs explicit process termination, identify the exact task's command, PID and
parent first. A substring such as a port number can also occur inside an unrelated JVM's
dependency paths or hashes.

### `just test-class` crosses an unsupported native Python execution boundary

The Python wrapper found the test module, then failed with:

```text
OSError: [WinError 193] %1 is not a valid Win32 application
```

[`scripts/test-class`](../scripts/test-class) passes the extensionless Bash script
`scripts/gradle-locked` directly to `subprocess.call`. Native Windows Python does not execute
that script through its shebang. Starting the Python wrapper from Git Bash does not itself
change this subprocess behavior.

The working workaround was a temporary `just` recipe that invokes the same build wrapper
directly, without the Python test-discovery launcher. For example, create
`.windows-check.just` in the repository root:

```just
targeted-check:
    scripts/gradle-locked :rules-engine:test --tests '*MyMechanicTest'
```

Replace the module and test pattern with the actual test, then run:

```powershell
just --justfile .windows-check.just targeted-check
```

Remove the temporary file afterward. Heavy builds must still go through `just`; this is
not permission to run raw Gradle. A repository fix would make `test-class` explicitly launch
Bash on Windows; that fix has not been implemented here.

### Assay has a native Windows launcher

The Unix Assay launcher printed reports, but command completion was sometimes delayed.
The cause of that delay was not established. The generated native launcher successfully
returned an exit code:

```powershell
# Build through the existing recipe first.
just _assay-tool
& .\oracle-assay\build\install\oracle-assay\bin\oracle-assay.bat differential --set TRK
$LASTEXITCODE
```

This uses the CLI already built by `just`, not a second build path. Read its report as well
as its exit code: a differential command can exit successfully while reporting divergences,
and a grammar decline is not independent validation of a card.

## Sandbox access is separate from installation

- **Python aliases:** `python` resolved to a WindowsApps execution alias that could not
  start inside the sandbox. `py` also failed to start its installed Windows Store Python
  there. Running the edit script with `py` through the command tool's normal approved
  escalation succeeded. Inspect resolution with `Get-Command python, python3, py -All`;
  the existence of an alias does not prove the interpreter is runnable in that context.
- **HTTPS with `curl.exe`:** a Scryfall request failed inside the sandbox with
  `curl: (35) schannel: AcquireCredentialsHandle failed: SEC_E_NO_CREDENTIALS`.
  The same request succeeded with approved escalation. This was not evidence of a bad
  Scryfall URL or certificate. Do not disable certificate verification as a workaround.

For an agent, retry a required command through the command tool's documented approval
mechanism when access is denied. Do not switch runtimes or tools to bypass that boundary.

## Build coordination and command completion

[`scripts/gradle-locked`](../scripts/gradle-locked) warns and runs **unlocked** when `shlock`
is unavailable, as it was in this Git for Windows environment. Calling `just` is still
required, but does not guarantee that the machine-global two-slot semaphore is active.
Coordinate builds manually when this warning appears.

Never overlap test runs in the **same checkout**, even when they select different tests.
Running a focused `:ai:test` diagnostic while the full test gate was still using `:ai:test`
caused a collision in `ai/build/test-results/test/binary/in-progress-results-generic.bin`.
A serial rerun removed that infrastructure failure. A temporary recipe does not give a
test run its own output directory.

A command tool can yield a session while a command is still running. A finished-looking
report is not an exit status: poll that original session to completion before launching a
replacement or deleting its logs. Delayed completion was observed here, but was not proven
to be a Windows-specific defect in the command tool.

## Paths, text encoding and diffs

- **PowerShell is not Bash.** Bash-style path braces such as
  `engine/{handlers,mechanics}` caused a PowerShell parser error. Pass explicit paths.
  Likewise, `rg` was given a literal path containing `*.kt` and rejected it; use an existing
  directory plus `rg -g '*.kt'` to filter files instead.
- **Facade allowlists currently compare platform-dependent path strings.**
  `FacadeBoundaryTest` checks `rel.toString().endsWith(...)` against entries with `/`.
  On Windows, `\` separators caused 16 already-allowlisted card entries to be reported as
  violations. The test was left unchanged in the beam-me-up work, with user approval.
  A proposed fix is to normalize the relative path to `/` before matching the allowlist.
  Inspect the reported entries before treating this signature as a card regression; do not
  disable the test or silently mark the full gate as passed.
- **Choose text encoding explicitly.** A Python edit using its Windows default encoding
  corrupted existing UTF-8 punctuation in comments. Specify `encoding="utf-8"` for both
  reads and writes. Windows text writes also produce CRLF; the repository's
  [`.gitattributes`](../.gitattributes) (`* text=auto`) normalizes text files to LF when
  they are staged, so CRLF in the working tree no longer turns into whole-file diffs.
  Scripts run through a shebang or `just` are pinned to LF on checkout, batch files to CRLF.

- **Use safe filenames.** Per-set caches use `~/.cache/scryfall/set-<code>.json`
  to avoid Windows reserved device names like CON. Legacy cache files are renamed
  automatically on access, preserving their contents and freshness metadata.

These notes document observed limitations. They do not waive the verification requirements
or the rule against changing someone else's in-flight work.
