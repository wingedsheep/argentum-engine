package com.wingedsheep.engine.gym.service

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Opaque identifier for an environment managed by [MultiEnvService].
 *
 * Wraps a string so the identifier crosses HTTP boundaries cleanly. Callers
 * should treat the value as opaque — format can evolve without breaking
 * consumers.
 */
@JvmInline
@Serializable
value class EnvId(val value: String) {
    override fun toString(): String = value

    companion object {
        fun generate(): EnvId = EnvId(UUID.randomUUID().toString())
    }
}
