package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Carbonize
 * {2}{R}
 * Instant
 * Carbonize deals 3 damage to any target. If it's a creature, it can't be
 * regenerated this turn, and if it would die this turn, exile it instead.
 */
val Carbonize = card("Carbonize") {
    manaCost = "{2}{R}"
    typeLine = "Instant"
    oracleText = "Carbonize deals 3 damage to any target. If it's a creature, it can't be regenerated this turn, and if it would die this turn, exile it instead."

    spell {
        val t = target("target", AnyTarget())
        effect = CantBeRegeneratedEffect(t) then
                MarkExileOnDeathEffect(t) then
                DealDamageEffect(3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Glen Angus"
        flavorText = "\"Some of this world's most potent substances are also the most volatile.\"\n—Riptide Project research notes"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f565fa1-a1a0-4dd0-b7f4-df65a807d156.jpg?1562530228"
    }
}
