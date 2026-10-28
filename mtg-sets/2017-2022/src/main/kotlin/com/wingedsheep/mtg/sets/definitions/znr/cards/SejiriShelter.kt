package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sejiri Shelter {1}{W} // Sejiri Glacier — Zendikar Rising #37
 * Instant // Land · Modal double-faced card
 *
 * Front — Sejiri Shelter: "Target creature you control gains protection from the color of your
 * choice until end of turn."
 * Back — Sejiri Glacier: "This land enters tapped. {T}: Add {W}."
 *
 * The color is chosen on resolution (Mother of Runes' shape).
 */
private val SejiriShelterFront = card("Sejiri Shelter") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gains protection from the color of your choice until end of turn."

    spell {
        val t = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(t))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Darek Zabrocki"
        flavorText = "\"It blocks the windswept salt and frigid air, but the loneliness of Sejiri remains.\"\n—Enwor, Sea Gate Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f25d56f9-aa54-4657-9ac9-e93fbba3e715.jpg?1783929413"
    }
}

private val SejiriGlacierBack = card("Sejiri Glacier") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "This land enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Darek Zabrocki"
        flavorText = "\"This place offers nothing and takes everything.\"\n—Enwor, Sea Gate Expeditionary House"
        imageUri = "https://cards.scryfall.io/normal/back/f/2/f25d56f9-aa54-4657-9ac9-e93fbba3e715.jpg?1783929413"
    }
}

val SejiriShelter: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = SejiriShelterFront,
    backFace = SejiriGlacierBack,
)
