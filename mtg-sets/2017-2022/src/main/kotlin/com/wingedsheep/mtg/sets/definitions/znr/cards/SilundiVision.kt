package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder

/**
 * Silundi Vision {2}{U} // Silundi Isle — Zendikar Rising #80
 * Instant // Land · Modal double-faced card
 *
 * Front — Silundi Vision: "Look at the top six cards of your library. You may reveal an instant or
 * sorcery card from among them and put it into your hand. Put the rest on the bottom of your library
 * in a random order."
 * Back — Silundi Isle: "This land enters tapped. {T}: Add {U}."
 */
private val SilundiVisionFront = card("Silundi Vision") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top six cards of your library. You may reveal an instant or sorcery card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 6,
            filter = GameObjectFilter.InstantOrSorcery,
            prompt = "You may reveal an instant or sorcery card from among them and put it into your hand",
            restOrder = CardOrder.Random,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Randy Vargas"
        flavorText = "\"Feel the current? Rough waters lie ahead.\"\n—Jori En, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11568cdf-6148-494c-8b98-f5ca5797d775.jpg?1783929393"
    }
}

private val SilundiIsleBack = card("Silundi Isle") {
    typeLine = "Land"
    colorIdentity = "U"
    oracleText = "This land enters tapped.\n{T}: Add {U}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Randy Vargas"
        flavorText = "\"From the hedron fields of Agadeem to the Faduun of Jwar Isle, the islands of the Silundi " +
            "Sea offer one mystery after another.\"\n—Jori En, expedition leader"
        imageUri = "https://cards.scryfall.io/normal/back/1/1/11568cdf-6148-494c-8b98-f5ca5797d775.jpg?1783929393"
    }
}

val SilundiVision: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SilundiVisionFront,
    backFace = SilundiIsleBack,
)
