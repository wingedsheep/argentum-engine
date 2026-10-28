package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cybernetic Specialist
 * {2}
 * Artifact Creature — Human Scientist
 * 2/1
 *
 * When this creature enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 * {1}, {T}: Put target card from your graveyard on the bottom of your library.
 */
val CyberneticSpecialist = card("Cybernetic Specialist") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Human Scientist"
    oracleText = "When this creature enters, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)\n{1}, {T}: Put target card from your graveyard on the bottom of your library."
    power = 2
    toughness = 1
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Surveil(1)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val card = target(TargetFilter.CardInGraveyard.ownedByYou())
        effect = Effects.PutOnBottomOfLibrary(card)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "262"
        artist = "Joan Piqué Llorens"
        flavorText = "What Airiam lost made what she kept so much more precious."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b147f16e-a2d6-4d43-9213-9a2723b0d3ef.jpg?1786005495"
    }
}
