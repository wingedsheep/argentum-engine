// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Starlight
 * {1}{W}
 * Sorcery
 * You gain 3 life for each black creature target opponent controls.
 */
val Starlight = card("Starlight") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = GainLifeEffect(DynamicAmount.Multiply(DynamicAmount.AggregateBattlefield(Player.TargetOpponent, GameObjectFilter.Creature.withColor(Color.BLACK)), 3))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "John Avon"
        flavorText = "Stars are like coins dropped into the night: their light buys safe passage."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6992524-6921-473b-8301-cb63fe502600.jpg"
    }
}
