package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Invasion of Xerex // Vertex Paladin — March of the Machine #242.
 * {2}{W}{U} · Battle — Siege · defense 4 // Creature — Angel Knight, star/star
 *
 * When this Siege enters, return up to one target creature to its owner's hand.
 * // Flying; Vertex Paladin's power and toughness are each equal to the number of creatures you control.
 */
private val InvasionOfXerexFront = card("Invasion of Xerex") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, return up to one target creature to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ReturnToHand(creature)
        description = "When this Siege enters, return up to one target creature to its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c8c9c33-569c-4b67-a18d-4cd0f18d01f4.jpg?1783916949"
    }
}

private val VertexPaladin = card("Vertex Paladin") {
    manaCost = ""
    colorIdentity = "WU"
    colorIndicator = "WU"
    typeLine = "Creature — Angel Knight"
    oracleText = "Flying\nVertex Paladin's power and toughness are each equal to the number of creatures you control."

    keywords(Keyword.FLYING)
    dynamicStats(DynamicAmounts.creaturesYouControl())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Fajareka Setiawan"
        flavorText = "The literal-minded Phyrexians made the greatest mistake possible on Xerex: they " +
            "tried to understand what they were seeing."
        imageUri = "https://cards.scryfall.io/normal/back/6/c/6c8c9c33-569c-4b67-a18d-4cd0f18d01f4.jpg?1783916949"
    }
}

val InvasionOfXerex: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfXerexFront,
    backFace = VertexPaladin,
)
