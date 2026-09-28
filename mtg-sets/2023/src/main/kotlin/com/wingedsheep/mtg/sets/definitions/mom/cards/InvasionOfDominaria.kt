package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Invasion of Dominaria // Serra Faithkeeper — March of the Machine #21.
 * {2}{W} · Battle — Siege · defense 5 // Creature — Angel 4/4
 *
 * When this Siege enters, you gain 4 life and draw a card.
 * // Flying, vigilance
 */
private val InvasionOfDominariaFront = card("Invasion of Dominaria") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Battle — Siege"
    startingDefense = 5
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, you gain 4 life and draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(4) then Effects.DrawCards(1)
        description = "When this Siege enters, you gain 4 life and draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Denys Tsiperko"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e784d3d-0c6d-4ce5-beb5-edd2adb32385.jpg?1783917066"
        ruling("2023-04-14", "Sieges each have an intrinsic triggered ability. That ability is \"When the last defense counter is removed from this permanent, exile it, then you may cast it transformed without paying its mana cost.\"")
        ruling("2023-04-14", "As a Siege enters the battlefield, its controller chooses an opponent to be its protector.")
    }
}

private val SerraFaithkeeper = card("Serra Faithkeeper") {
    manaCost = ""
    colorIdentity = "W"
    colorIndicator = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying, vigilance"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Denys Tsiperko"
        flavorText = "\"I was there when Phyrexia doomed Serra's realm with a single touch. I was " +
            "there when Yawgmoth tried and failed to conquer Dominaria. And I will still be here " +
            "when Elesh Norn lies dead.\""
        imageUri = "https://cards.scryfall.io/normal/back/7/e/7e784d3d-0c6d-4ce5-beb5-edd2adb32385.jpg?1783917066"
    }
}

val InvasionOfDominaria: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfDominariaFront,
    backFace = SerraFaithkeeper,
)
