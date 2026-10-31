package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mythos of Vadrok — Ikoria: Lair of Behemoths #127
 * {2}{R}{R} · Sorcery
 *
 * Mythos of Vadrok deals 5 damage divided as you choose among any number of target creatures
 * and/or planeswalkers. If {W}{U} was spent to cast this spell, until your next turn, those
 * permanents can't attack or block and their activated abilities can't be activated.
 *
 * The division is Chandra, Flameshaper's −4 shape: "any number" of targets, capped at 5 because
 * each must be assigned at least 1 damage, with the split chosen as the spell is cast. The {W}{U}
 * rider reads the mana actually spent (a copy gets nothing) and then walks every still-legal
 * target — "those permanents" — applying Academic Probation's until-your-next-turn lock to each.
 */
val MythosOfVadrok = card("Mythos of Vadrok") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "RUW"
    typeLine = "Sorcery"
    oracleText = "Mythos of Vadrok deals 5 damage divided as you choose among any number of target " +
        "creatures and/or planeswalkers. If {W}{U} was spent to cast this spell, until your next turn, " +
        "those permanents can't attack or block and their activated abilities can't be activated."

    spell {
        val damaged = targets(
            TargetFilter(GameObjectFilter.CreatureOrPlaneswalker),
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.fixed(5),
        )
        effect = Effects.DividedDamage(total = 5, minTargets = 1, maxTargets = 5, among = damaged) then
            Effects.If(
                condition = Conditions.ManaSpentToCastIncludes(requiredWhite = 1, requiredBlue = 1),
                then = Effects.ForEachTarget(
                    Effects.CantAttackOrBlock(EffectTarget.ContextTarget(0), Duration.UntilYourNextTurn) then
                        Effects.GrantStaticAbility(
                            ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                            target = EffectTarget.ContextTarget(0),
                            duration = Duration.UntilYourNextTurn,
                        )
                ),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2bbe99a7-3b58-4f23-bf86-e1e35b0bec2e.jpg"
        ruling(
            "2020-04-17",
            "If an effect copies the Mythos spell, no mana was spent to cast the copy, so the copy won't " +
                "receive the bonus."
        )
        ruling(
            "2020-04-17",
            "The ability checks what mana was actually spent to cast a spell. If an effect allows you to " +
                "spend mana \"as though it were mana\" of any color or type, that allows you to spend mana " +
                "you couldn't otherwise spend, but it doesn't change what mana you spent to cast the spell."
        )
    }
}
