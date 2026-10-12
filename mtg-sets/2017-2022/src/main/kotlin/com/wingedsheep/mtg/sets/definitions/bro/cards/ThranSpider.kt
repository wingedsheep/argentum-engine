package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Thran Spider
 * {3}
 * Artifact Creature — Spider
 * 2/4
 * Reach
 * When this creature enters, you and target opponent each create a tapped Powerstone token.
 * {7}: Look at the top four cards of your library. You may reveal an artifact card from among
 * them and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * "You and target opponent each" is two creations: one for the controller, one redirected to the
 * targeted opponent via [Effects.CreatePowerstone]'s `controller`.
 */
val ThranSpider = card("Thran Spider") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Spider"
    power = 2
    toughness = 4
    oracleText = "Reach\n" +
        "When this creature enters, you and target opponent each create a tapped Powerstone token.\n" +
        "{7}: Look at the top four cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreatePowerstone(tapped = true) then
            Effects.CreatePowerstone(tapped = true, controller = opponent)
    }

    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card from among them and put it into your hand"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "254"
        artist = "Joshua Cairos"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e72e0ddb-3dd4-4db3-aa05-dec8691432a0.jpg?1783920010"
    }
}
