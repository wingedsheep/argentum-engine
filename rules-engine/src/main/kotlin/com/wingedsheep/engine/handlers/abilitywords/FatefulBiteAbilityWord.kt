package com.wingedsheep.engine.handlers.abilitywords

import com.wingedsheep.sdk.core.Keyword

/**
 * Catalog entry for the Fateful Bite ability word (CR 207.2c).
 *
 * Ability words have no rules meaning — they are flavor prefixes only. This object
 * exists so that the engine's ability-word catalog is explicit and future cards can
 * attach the Fateful Bite prefix without requiring additional engine changes.
 *
 * Resolution of any ability tagged with this prefix is identical to the same ability
 * without the prefix.
 */
object FatefulBiteAbilityWord {
    val keyword: Keyword = Keyword.FATEFUL_BITE
}
