// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Theft of Dreams
 * {2}{U}
 * Sorcery
 * Draw a card for each tapped creature target opponent controls.
 */
val TheftofDreams = card("Theft of Dreams") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = DrawCardsEffect(DynamicAmount.AggregateBattlefield(Player.TargetOpponent, GameObjectFilter.Creature.tapped()))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Adam Rex"
        flavorText = "Energy is never lost, only transformed."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29019e28-4ef8-4732-9972-0a47305fe303.jpg"
    }
}
