package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Invasion of Amonkhet // Lazotep Convert — March of the Machine #231.
 * {1}{U}{B} · Battle — Siege · defense 4 // Creature — Zombie 4/4
 *
 * When this Siege enters, each player mills three cards, then each opponent discards a card and
 * you draw a card.
 * // You may have this creature enter as a copy of any creature card in a graveyard, except it's a
 * //   4/4 black Zombie in addition to its other colors and types.
 *
 * Lazotep Convert is a graveyard-sourced [EntersAsCopy] like Superior Spider-Man, with the colour
 * rider on `additionalColors`. It keeps the copied card's name (no name override), and — unlike
 * Superior Spider-Man — leaves the copied card where it is.
 */
private val InvasionOfAmonkhetFront = card("Invasion of Amonkhet") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, each player mills three cards, then each opponent discards a card " +
        "and you draw a card. (To mill three cards, a player puts the top three cards of their " +
        "library into their graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each)) then
            Effects.Discard(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.DrawCards(1)
        description = "When this Siege enters, each player mills three cards, then each opponent " +
            "discards a card and you draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Jokubas Uogintas"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/981f091a-17f8-412e-9752-69070ebed4ea.jpg?1783916953"
        ruling("2023-04-14", "To resolve Invasion of Amonkhet's enters-the-battlefield ability, first each player mills three cards. Then the next opponent in turn order (or, if it's an opponent's turn, the opponent whose turn it is) chooses a card in hand and sets it aside without revealing it. Then each other opponent in turn order does the same. Finally, all chosen cards are revealed and discarded at the same time. Finally, you draw a card.")
        ruling("2023-04-14", "Sieges each have an intrinsic triggered ability. That ability is \"When the last defense counter is removed from this permanent, exile it, then you may cast it transformed without paying its mana cost.\"")
        ruling("2023-04-14", "As a Siege enters the battlefield, its controller chooses an opponent to be its protector.")
    }
}

private val LazotepConvert = card("Lazotep Convert") {
    manaCost = ""
    colorIdentity = "UB"
    colorIndicator = "UB"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 4
    oracleText = "You may have this creature enter as a copy of any creature card in a graveyard, " +
        "except it's a 4/4 black Zombie in addition to its other colors and types."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature,
            copyFromZone = Zone.GRAVEYARD,
            additionalSubtypes = listOf("Zombie"),
            additionalColors = setOf(Color.BLACK),
            powerOverride = 4,
            toughnessOverride = 4,
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Jokubas Uogintas"
        flavorText = "As the lazotep covered his body, he heard Norn's voice fade from a deafening " +
            "clarion to a distant whisper."
        imageUri = "https://cards.scryfall.io/normal/back/9/8/981f091a-17f8-412e-9752-69070ebed4ea.jpg?1783916953"
        ruling("2023-04-14", "Lazotep Convert has received an update to its official rules text to clarify that it keeps its other colors and types as part of its copy effect.")
        ruling("2023-04-14", "Lazotep Convert copies exactly what was printed on the original creature card, with the noted exceptions.")
        ruling("2023-04-14", "Any enters-the-battlefield abilities of the copied creature card will trigger when Lazotep Convert enters the battlefield. Any \"as [this creature] enters the battlefield\" or \"[this creature] enters the battlefield with\" abilities of the chosen creature card will also work.")
    }
}

val InvasionOfAmonkhet: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfAmonkhetFront,
    backFace = LazotepConvert,
)
