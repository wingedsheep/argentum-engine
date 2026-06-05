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
 * Fruition
 * {G}
 * Sorcery
 * You gain 1 life for each Forest on the battlefield.
 */
val Fruition = card("Fruition") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = GainLifeEffect(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Land.withSubtype("Forest")))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Steve Luke"
        flavorText = "Come to these woods and suffer no pain; you've burdens to lose and new life to gain."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/147082a3-1408-44f9-ab39-f069cee5c710.jpg"
    }
}
