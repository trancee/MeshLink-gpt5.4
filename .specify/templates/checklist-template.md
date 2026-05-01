# [CHECKLIST TYPE] Checklist: [FEATURE NAME]

**Purpose**: [Brief description — e.g., "Pre-merge verification for feature X"]  
**Created**: [DATE]  
**Feature**: `specs/[###-feature-name]/spec.md`

<!-- 
  Replace sample items below with actual items based on feature context.
  Not all categories apply to every feature — delete irrelevant sections.
-->

## Code Quality

- [ ] CHK001 All code passes `./gradlew :meshlink:detekt` with zero issues
- [ ] CHK002 All code formatted by `./gradlew :meshlink:ktfmtCheck`
- [ ] CHK003 `explicitApi()` — all public declarations have explicit visibility + return types
- [ ] CHK004 No `TODO` comments in production source sets
- [ ] CHK005 No `@Suppress` annotations without inline justification

## Testing & Coverage

- [ ] CHK006 `./gradlew :meshlink:jvmTest` — all tests pass
- [ ] CHK007 `./gradlew :meshlink:koverVerify` — 100% line + branch coverage
- [ ] CHK008 Tests use Power-assert (not plain assertEquals for non-collection comparisons)
- [ ] CHK009 Integration tests use `MeshTestHarness` / `VirtualMeshTransport`
- [ ] CHK010 No untestable patterns: `require()` with interpolation, `while(isActive)`, non-exhaustive `when`

## Wire Protocol & Compatibility

- [ ] CHK011 Wire message changes are backward-compatible (old nodes ignore/handle gracefully)
- [ ] CHK012 `spec/schemas/meshlink.fbs` updated if new message types added
- [ ] CHK013 `./gradlew :meshlink:apiCheck` — BCV passes (no unintentional API breaks)
- [ ] CHK014 `.api` file diff reviewed and intentional (JVM + KLib)

## Cryptographic Correctness

- [ ] CHK015 All crypto operations go through `CryptoProvider` interface
- [ ] CHK016 No third-party crypto libraries introduced as runtime deps
- [ ] CHK017 Wycheproof test vectors cover new/modified crypto paths
- [ ] CHK018 Key material is zeroed after use where applicable
- [ ] CHK019 Constant-time comparisons used for secrets (`ConstantTimeEquals`)

## Platform Consistency

- [ ] CHK020 Public API (`MeshLinkApi`) identical across all targets
- [ ] CHK021 Platform differences confined to `expect/actual` in platform source sets
- [ ] CHK022 iOS compiles: `./gradlew :meshlink:compileKotlinIos` (macOS only)
- [ ] CHK023 SKIE generates correct Swift wrappers (sealed → exhaustive enum, Flow → AsyncStream)
- [ ] CHK024 Diagnostic events use same codes/payloads on all platforms

## Performance

- [ ] CHK025 Performance budget from spec satisfied (latency, memory, throughput)
- [ ] CHK026 Benchmark shows <10% regression from baseline: `./gradlew :meshlink:jvmBenchmark`
- [ ] CHK027 No unnecessary allocations in hot paths (verified by benchmark or inspection)
- [ ] CHK028 Power tier constraints respected (scan duty cycle, connection intervals)

## Documentation

- [ ] CHK029 KDoc on all new/modified public declarations
- [ ] CHK030 Documentation updated in `docs/` if user-facing behavior changes
- [ ] CHK031 Every human and automated commit uses a Conventional Commit message (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `perf:`, `chore:`)

## Security

- [ ] CHK032 No secrets, keys, or credentials in source code
- [ ] CHK033 Replay protection covers new message paths
- [ ] CHK034 Trust model constraints respected (TrustStore, TrustMode)
- [ ] CHK035 No information leakage in error messages or diagnostics (peer IDs redactable)

## Final Gate

- [ ] CHK036 Full verification passes:
  ```bash
  ./gradlew :meshlink:jvmTest :meshlink:koverVerify :meshlink:apiCheck :meshlink:detekt :meshlink:ktfmtCheck
  ```
- [ ] CHK037 Feature branch rebased on latest `main`, no conflicts
- [ ] CHK038 All checklist items above addressed (checked or marked N/A with reason)

## Notes

- Delete sections that don't apply (e.g., "Wire Protocol" for internal-only changes)
- Add feature-specific items as needed
- Mark items N/A with brief reason if a category doesn't apply
