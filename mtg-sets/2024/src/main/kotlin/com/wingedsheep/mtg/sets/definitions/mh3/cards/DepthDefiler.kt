package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.mode

val DepthDefiler = card("Depth Defiler") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 5
    oracleText = "Devoid (This card has no color.)\nKicker {C} (You may pay an additional {C} as you cast this spell.)\nWhen you cast this spell, choose one. If it was kicked, choose both instead.\n• Return target creature to its owner's hand.\n• Target player draws two cards, then discards a card."

    keywords(Keyword.DEVOID)
    keywordAbility(KeywordAbility.kicker("{C}"))

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val count = DynamicAmounts.conditional(Conditions.WasKicked, 2, 1)
        effect = Effects.Modal(
            modes = listOf(
                mode("Return target creature to its owner's hand") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.ReturnToHand(creature)
                },
                mode("Target player draws two cards, then discards a card") {
                    val player = target(Targets.Player)
                    effect = Effects.DrawCards(2, player) then Effects.Discard(1, player)
                }
            ),
            dynamicChooseCount = count,
            dynamicMinChooseCount = count
        )
    }

    metadata {
        ruling("2024-06-07", "The triggered ability will resolve before Depth Defiler does. If Depth Defiler is countered or otherwise leaves the stack in response to its triggered ability, the triggered ability will still resolve as normal. If Depth Defiler was kicked, you'll still follow the instructions of both modes.")
        ruling("2024-06-07", "Each chosen mode is performed in the order specified. Abilities that trigger while one mode is being performed won't be put onto the stack until Depth Defiler's triggered ability has finished resolving.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37684797-7503-460d-9f74-047b1ab2fac3.jpg?1783911292"
    }
}
