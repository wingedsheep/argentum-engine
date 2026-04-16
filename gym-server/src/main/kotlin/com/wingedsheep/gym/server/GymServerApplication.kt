package com.wingedsheep.gym.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * HTTP transport for the [com.wingedsheep.gym.service.MultiEnvService].
 *
 * Exposes env lifecycle, stepping, decisions, fork/snapshot/restore and
 * metadata (schema hash, health) so a Python training loop can drive many
 * concurrent environments in parallel. The service layer itself lives in
 * `:gym` and remains transport-agnostic — this module is a thin
 * Spring shell, not a place for game logic.
 *
 * ## Intentionally out of scope for the scaffold
 *
 * - **Auth**: no bearer tokens, API keys, or network ACLs. Run bound to
 *   localhost until you add one.
 * - **Env lifetime / TTL**: crashed trainers leak envs forever. A reaper
 *   thread + heartbeat header is the next step.
 * - **Byte-based snapshots**: `SnapshotHandle.Slot` is still in-process.
 *   Distributed MCTS will want a `SnapshotHandle.Bytes` variant.
 * - **Metrics / observability**: no Prometheus, no structured logs.
 */
@SpringBootApplication
class GymServerApplication

fun main(args: Array<String>) {
    runApplication<GymServerApplication>(*args)
}
