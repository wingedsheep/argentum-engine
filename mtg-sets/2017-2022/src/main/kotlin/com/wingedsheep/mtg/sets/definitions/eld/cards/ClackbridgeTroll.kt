package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Clackbridge Troll
 * {3}{B}{B}
 * Creature — Troll
 * 8/8
 *
 * Trample, haste
 * When this creature enters, target opponent creates three 0/1 white Goat creature tokens.
 * At the beginning of combat on your turn, any opponent may sacrifice a creature of their choice.
 * If a player does, tap this creature, you gain 3 life, and you draw a card.
 *
 * The combat trigger follows Desecration Demon: an [AnyPlayerMayPay] offer scoped to the
 * controller's opponents, whose consequence runs once if at least one opponent sacrificed.
 */
val ClackbridgeTroll = card("Clackbridge Troll") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Troll"
    power = 8
    toughness = 8
    oracleText = "Trample, haste\n" +
        "When this creature enters, target opponent creates three 0/1 white Goat creature tokens.\n" +
        "At the beginning of combat on your turn, any opponent may sacrifice a creature of their choice. " +
        "If a player does, tap this creature, you gain 3 life, and you draw a card."

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Goat"),
            count = 3,
            controller = opponent,
            imageUri = "https://cards.scryfall.io/normal/front/2/1/213be25c-88db-440b-83c3-973586a4f320.jpg?1783932483",
        )
        description = "When this creature enters, target opponent creates three 0/1 white Goat creature tokens."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.AnyPlayerMayPay(
            cost = Costs.pay.Sacrifice(GameObjectFilter.Creature, count = 1),
            consequence = Effects.Tap(EffectTarget.Self) then
                Effects.GainLife(3) then
                Effects.DrawCards(1),
            eligiblePlayers = Player.EachOpponent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85929131-4df6-415c-b592-aefb2943c477.jpg?1783932641"
    }
}
