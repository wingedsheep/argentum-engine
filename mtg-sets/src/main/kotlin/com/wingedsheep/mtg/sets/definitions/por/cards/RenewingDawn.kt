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
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Renewing Dawn
 * {1}{W}
 * Sorcery
 * You gain 2 life for each Mountain target opponent controls.
 */
val RenewingDawn = card("Renewing Dawn") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = GainLifeEffect(DynamicAmount.Multiply(DynamicAmount.AggregateBattlefield(Player.TargetOpponent, GameObjectFilter.Land.withSubtype("Mountain")), 2))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "John Avon"
        flavorText = "Dawn brings a new day, and a new day brings hope."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e56300cb-6b44-47fe-9508-c33ad5670b4b.jpg"
    }
}
