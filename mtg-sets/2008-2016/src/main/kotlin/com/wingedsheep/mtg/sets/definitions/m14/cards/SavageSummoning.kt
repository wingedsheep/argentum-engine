package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Savage Summoning
 * {G}
 * Instant
 *
 * This spell can't be countered.
 * The next creature spell you cast this turn can be cast as though it had flash. That spell can't
 * be countered. That creature enters with an additional +1/+1 counter on it.
 *
 * Three one-shot next-spell riders over the same filter, so the first creature spell cast this turn
 * spends all three — whether or not it used the flash permission (a creature cast in your main
 * phase is still "the next creature spell"). The counter is owed on the spell and placed as the
 * creature enters; a countered-anyway or otherwise removed spell gets nothing.
 */
val SavageSummoning = card("Savage Summoning") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "The next creature spell you cast this turn can be cast as though it had flash. " +
        "That spell can't be countered. That creature enters with an additional +1/+1 counter on it."

    cantBeCountered = true

    spell {
        effect = Effects.GrantFlashToNextSpell(GameObjectFilter.Creature) then
            Effects.MakeNextSpellUncounterable(GameObjectFilter.Creature) then
            Effects.GrantNextSpellEntryCounters(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5346ed7-2e17-4d8c-9c4b-b5efdd26380d.jpg?1783939900"
    }
}
