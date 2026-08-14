package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Belligerent of the Ball
 * {2}{R}
 * Creature — Ogre Warrior
 * 3/3
 *
 * Celebration — At the beginning of combat on your turn, if two or more nonland permanents entered
 * the battlefield under your control this turn, target creature you control gets +1/+0 and gains
 * menace until end of turn.
 *
 * The triggered half of the Celebration ability word (CR 207.2c — italic flavor, no rules meaning),
 * same shape as [PestsOfHonor]: an intervening-'if' clause (CR 603.4), so [Conditions.Celebration]
 * is checked both when the begin-combat step starts and again as the ability resolves.
 *
 * The bonus targets any creature you control — including this one — so the pump and the menace
 * grant share a single target requirement chosen when the ability goes on the stack.
 */
val BelligerentOfTheBall = card("Belligerent of the Ball") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 3
    toughness = 3
    oracleText = "Celebration — At the beginning of combat on your turn, if two or more nonland " +
        "permanents entered the battlefield under your control this turn, target creature you " +
        "control gets +1/+0 and gains menace until end of turn."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        triggerCondition = Conditions.Celebration
        val creature = target(
            "target creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.Composite(
            Effects.ModifyStats(power = 1, toughness = 0, target = creature),
            Effects.GrantKeyword(Keyword.MENACE, creature),
        )
        description = "At the beginning of combat on your turn, if two or more nonland permanents " +
            "entered the battlefield under your control this turn, target creature you control " +
            "gets +1/+0 and gains menace until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Pascal Quidault"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6658398a-46a5-4f41-9b1b-4a47f2822cf8.jpg?1783915098"
    }
}
