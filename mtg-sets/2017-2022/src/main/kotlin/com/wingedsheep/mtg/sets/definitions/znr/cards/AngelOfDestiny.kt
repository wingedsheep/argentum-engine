package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angel of Destiny — Zendikar Rising #2
 * {3}{W}{W} · Creature — Angel Cleric · 2/6
 *
 * Flying, double strike
 * Whenever a creature you control deals combat damage to a player, you and that player each
 * gain that much life.
 * At the beginning of your end step, if you have at least 15 life more than your starting life
 * total, each player this creature attacked this turn loses the game.
 *
 * Rulings honoured:
 * - A creature you control dealing combat damage to *you* (redirected) makes you gain twice that
 *   much life as a single life-gaining event — hence the `TriggeringPlayerIs(You)` branch.
 * - Only players the Angel attacked directly lose; its attack record belongs to this object, so
 *   an Angel that left and returned has attacked nobody, and one that left can't trigger at all.
 */
val AngelOfDestiny = card("Angel of Destiny") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Cleric"
    oracleText = "Flying, double strike\n" +
        "Whenever a creature you control deals combat damage to a player, you and that player each " +
        "gain that much life.\n" +
        "At the beginning of your end step, if you have at least 15 life more than your starting " +
        "life total, each player this creature attacked this turn loses the game."
    power = 2
    toughness = 6

    keywords(Keyword.FLYING, Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        val damage = DynamicAmounts.triggerDamageAmount()
        effect = Effects.If(
            condition = Conditions.TriggeringPlayerIs(Player.You),
            then = Effects.GainLife(damage * 2),
            otherwise = Effects.GainLife(damage) then
                Effects.GainLife(damage, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.LifeAboveStartingBy(15)
        effect = Effects.ForEachPlayer(
            Player.Each,
            Effects.If(
                condition = Conditions.SourceAttackedPlayerThisTurn(Player.You),
                then = Effects.LoseGame(EffectTarget.Controller),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9897074a-0ac4-4d1a-9aac-e77830cc5c78.jpg?1783929425"
    }
}
