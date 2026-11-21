package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Boros Battleshaper
 * {5}{R}{W}
 * Creature — Minotaur Soldier
 * 5/5
 *
 * At the beginning of each combat, up to one target creature attacks or blocks this combat if able
 * and up to one target creature can't attack or block this combat.
 *
 * Triggers on every player's beginning of combat. "Attacks or blocks this combat if able" on one
 * creature is both combat-scoped marks: only the one fitting its controller's role in this combat
 * can bind it — the active player's creature must attack, anyone else's must block. Both marks, and
 * the restriction, end with this combat phase, so an extra combat later in the turn is unaffected.
 * Choosing the same creature for both targets leaves it unable to attack or block: a restriction
 * excuses a requirement it makes impossible (CR 508.1d / 509.1c).
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val BorosBattleshaper = card("Boros Battleshaper") {
    manaCost = "{5}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Minotaur Soldier"
    power = 5
    toughness = 5
    oracleText = "At the beginning of each combat, up to one target creature attacks or blocks this " +
        "combat if able and up to one target creature can't attack or block this combat."

    triggeredAbility {
        val forced = target(TargetFilter.Creature, optional = true)
        val restrained = target(TargetFilter.Creature, optional = true)
        trigger = Triggers.anyPlayer.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.MarkMustAttackThisCombat(forced) then
            Effects.MarkMustBlockThisCombat(forced) then
            Effects.CantAttackOrBlock(restrained, Duration.EndOfCombat)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Zoltan Boros"
        flavorText = "Leaders shape the minds of their allies. It takes a master to shape the minds of enemies."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c43e449-acf2-4e94-b7cf-8c84d70191da.jpg?1783940032"
    }
}
