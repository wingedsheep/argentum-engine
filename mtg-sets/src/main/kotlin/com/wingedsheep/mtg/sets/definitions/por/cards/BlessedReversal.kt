// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Blessed Reversal
 * {1}{W}
 * Instant
 * You gain 3 life for each creature attacking you.
 */
val BlessedReversal = card("Blessed Reversal") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    spell {
        effect = GainLifeEffect(DynamicAmount.Multiply(DynamicAmount.AggregateBattlefield(Player.Opponent, GameObjectFilter.Creature.attacking()), 3))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899ecc19-8106-4e5a-bb25-aaea9684ba0e.jpg"
    }
}
