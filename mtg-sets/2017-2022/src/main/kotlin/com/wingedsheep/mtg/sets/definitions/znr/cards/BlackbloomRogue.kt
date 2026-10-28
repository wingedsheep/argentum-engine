package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Blackbloom Rogue {2}{B} // Blackbloom Bog — Zendikar Rising #91
 * Creature — Human Rogue 2/3 // Land · Modal double-faced card
 *
 * Front — Blackbloom Rogue: "Menace. This creature gets +3/+0 as long as an opponent has eight or
 * more cards in their graveyard."
 * Back — Blackbloom Bog: "This land enters tapped. {T}: Add {B}."
 *
 * "An opponent has eight or more" is Nimana Skitter-Sneak's per-opponent test: the greatest
 * graveyard among the controller's opponents, not a sum across them.
 */
private val BlackbloomRogueFront = card("Blackbloom Rogue") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "This creature gets +3/+0 as long as an opponent has eight or more cards in their graveyard."
    power = 2
    toughness = 3

    keywords(Keyword.MENACE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 3, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                    Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                8
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Jesper Ejsing"
        flavorText = "\"If you ever want to get back up, do what I say.\"\n—Exena, expedition rogue"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32779721-b021-4bd4-95d1-4a19b78d9faa.jpg?1783929390"
        ruling(
            "2020-09-25",
            "An ability that offers a bonus if an opponent has eight or more cards in their graveyard " +
                "won't provide additional benefits if more than one opponent has eight or more cards in " +
                "their graveyard. There are also no additional benefits no matter how many cards are in " +
                "an opponent's graveyard, as long as there are at least eight."
        )
        ruling(
            "2020-09-25",
            "In a multiplayer game, once an opponent leaves the game, they won't be considered for such " +
                "an effect, no matter how many cards they had in their graveyard before leaving the game."
        )
    }
}

private val BlackbloomBogBack = card("Blackbloom Bog") {
    typeLine = "Land"
    colorIdentity = "B"
    oracleText = "This land enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Jesper Ejsing"
        flavorText = "\"The Raimunza River flows along the branches of jaddi trees for miles before " +
            "plunging down to feed Blackbloom Lake.\"\n—Exena, expedition rogue"
        imageUri = "https://cards.scryfall.io/normal/back/3/2/32779721-b021-4bd4-95d1-4a19b78d9faa.jpg?1783929390"
    }
}

val BlackbloomRogue: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = BlackbloomRogueFront,
    backFace = BlackbloomBogBack,
)
