package com.wingedsheep.engine.gym.contract

/**
 * Contract-schema hash. Python clients compare this against the value they
 * were built with; a mismatch warns (or hard-fails in strict mode) that the
 * DTO shape has drifted.
 *
 * Bump [CURRENT] whenever any `@Serializable` data class in this package
 * changes shape in a way that would break a downstream consumer. The value
 * itself is arbitrary; uniqueness is what matters.
 */
object SchemaHash {
    const val CURRENT: String = "argentum-gym-contract@v1.0"
}
