package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.Step

/**
 * Sami, Ship's Engineer
 * {2}{R}{W}
 * Legendary Creature — Human Artificer
 * 2/4
 *
 * At the beginning of your end step, if you control two or more tapped creatures,
 * create a tapped 2/2 colorless Robot artifact creature token.
 */
val SamiShipsEngineer = card("Sami, Ship's Engineer") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your end step, if you control two or more tapped creatures, " +
        "create a tapped 2/2 colorless Robot artifact creature token."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.tapped()).count(),
            ComparisonOperator.GTE,
            2
        )
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = emptySet(),
            creatureTypes = setOf("Robot"),
            artifactToken = true,
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c46f9a07-005c-44b7-8057-b2f00b274dd6.jpg?1756281130"
        )
        description = "create a tapped 2/2 colorless Robot artifact creature token"
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Zara Alfonso"
        flavorText = "Whenever Sami was down on their luck, they returned to an old comfort: the clinks " +
            "and clanks of tinkering."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75c38cc9-07de-46d4-8195-f04b2b7e0fee.jpg?1752947477"
    }
}
