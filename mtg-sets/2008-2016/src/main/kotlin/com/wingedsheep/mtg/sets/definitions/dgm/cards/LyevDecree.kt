package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lyev Decree — Dragon's Maze #3
 * {1}{W} · Sorcery
 *
 * Detain up to two target creatures your opponents control. (Until your next turn, those
 * creatures can't attack or block and their activated abilities can't be activated.)
 *
 * Detain is spelled out per target, the same until-your-next-turn lock Academic Probation and
 * Mythos of Vadrok apply: can't attack or block, plus a granted activation ban.
 */
val LyevDecree = card("Lyev Decree") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Detain up to two target creatures your opponents control. (Until your next turn, " +
        "those creatures can't attack or block and their activated abilities can't be activated.)"

    spell {
        targets(TargetFilter.CreatureOpponentControls, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.CantAttackOrBlock(EffectTarget.ContextTarget(0), Duration.UntilYourNextTurn) then
                Effects.GrantStaticAbility(
                    ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                    target = EffectTarget.ContextTarget(0),
                    duration = Duration.UntilYourNextTurn,
                )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Kev Walker"
        flavorText = "The Azorius have so many codes and statutes that you're always in violation of one of them."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/773cf2aa-4337-4d14-8a8e-ff8b1fdec1b5.jpg"
    }
}
