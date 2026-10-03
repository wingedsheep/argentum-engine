package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Fell the Profane {2}{B}{B} // Fell Mire
 * Instant
 * Destroy target creature or planeswalker.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {B}.
 */
private val FellTheProfaneFront = card("Fell the Profane") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Yeong-Hao Han"
        flavorText = "\"I can think of no fate more fitting for one so foul as you.\"\n—Aryel, knight of Windgrace"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3cb782d-c459-468d-9779-9b5669abc337.jpg?1783911234"
    }
}

private val FellMireBack = card("Fell Mire") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Yeong-Hao Han"
        flavorText = "\"The Nethermire is all the salvation you deserve.\"\n—Aryel, knight of Windgrace"
        imageUri = "https://cards.scryfall.io/normal/back/a/3/a3cb782d-c459-468d-9779-9b5669abc337.jpg?1783911234"
    }
}

val FellTheProfane: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = FellTheProfaneFront,
    backFace = FellMireBack,
)
