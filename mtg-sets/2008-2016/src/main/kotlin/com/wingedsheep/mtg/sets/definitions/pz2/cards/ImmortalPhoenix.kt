package com.wingedsheep.mtg.sets.definitions.pz2.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Immortal Phoenix (PZ2 #70773)
 * {4}{R}{R}  Creature — Phoenix  5/3
 *
 * Flying (This creature can't be blocked except by creatures with flying or reach.)
 * When this creature dies, return it to its owner's hand.
 */
val ImmortalPhoenix = card("Immortal Phoenix") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phoenix"
    oracleText = "Flying (This creature can't be blocked except by creatures with flying or reach.)\nWhen this creature dies, return it to its owner's hand."
    power = 5
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70773"
        artist = "Daarken"
        flavorText = "Their fires burn quietly, allowing the phoenixes to glide silently through the canyon."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3836560e-a64b-42af-93fa-3cf830c15845.jpg"
    }
}
