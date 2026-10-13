package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Coral Colony
 * {1}{U}
 * Creature — Wall
 * 1/4
 * Defender
 * {1}{U}, {T}: Target player mills X cards, where X is the number of creatures you control with defender.
 */
val CoralColony = card("Coral Colony") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 1
    toughness = 4
    oracleText = "Defender\n" +
        "{1}{U}, {T}: Target player mills X cards, where X is the number of creatures you control with defender. " +
        "(To mill a card, a player puts the top card of their library into their graveyard.)"

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{U}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)).count(),
            player
        )
    }

    metadata {
        ruling("2022-09-09", "The number of creatures with defender you control is counted when the ability resolves.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Campbell White"
        flavorText = "The seafloor is strewn with the wreckage of Vodalia's would-be conquerors."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c0e3882-1f97-48eb-b9e3-2f9683ff5897.jpg?1783921355"
    }
}
