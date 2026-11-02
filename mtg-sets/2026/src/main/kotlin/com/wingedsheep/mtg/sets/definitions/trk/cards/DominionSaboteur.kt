package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopyEntryCounters
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Dominion Saboteur — Star Trek #54.
 *
 * A Clone whose counter rider mirrors the copied permanent (`CopyEntryCounters.sameAsCopied()`):
 * counters aren't copiable values, so only the rider brings them over, kind for kind, and only when
 * a copy is actually made.
 */
val DominionSaboteur = card("Dominion Saboteur") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 3
    toughness = 3
    oracleText = "You may have this creature enter as a copy of any artifact or creature on the battlefield, " +
        "except it isn't legendary. If you do, it enters with additional counters on it equal to the same " +
        "number and kinds of counters the copied permanent has on it."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.CreatureOrArtifact,
            exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
            entersWithCounters = listOf(CopyEntryCounters.sameAsCopied()),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Alex Negrea"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/207c4369-220a-4eff-81dc-65872d6fc264.jpg?1785981397"
    }
}
