package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Wrap in Flames
 * {3}{R}
 * Sorcery
 * Wrap in Flames deals 1 damage to each of up to three target creatures. Those creatures can't
 * block this turn.
 *
 * Unearthly Blizzard with a ping: one "up to three target creatures" requirement
 * (`count = 3, optional = true`), and [Effects.ForEachTarget] runs the damage and the can't-block
 * once per still-legal target. The can't-block applies to each targeted creature even if its damage
 * is prevented; a target that became illegal is skipped entirely (rulings).
 */
val WrapInFlames = card("Wrap in Flames") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Wrap in Flames deals 1 damage to each of up to three target creatures. " +
        "Those creatures can't block this turn."

    spell {
        target = TargetObject(filter = TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(1, EffectTarget.ContextTarget(0)) then
                Effects.CantBlock(target = EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Véronique Meignaud"
        flavorText = "\"Our pyromancer's flames may not have killed them, but they bought us the time we " +
            "needed.\"\n—*The War Diaries*"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/babdaf80-f9a3-42cb-9df6-10878301f335.jpg?1783941968"
        ruling(
            "2020-01-24",
            "The \"can't block\" effect applies to each of the targeted creatures—and to only those " +
                "creatures—even if Wrap in Flames deals no damage to one or more of them (due to a prevention " +
                "effect, for example) or if Wrap in Flames deals damage to a different creature (due to a " +
                "redirection effect)."
        )
        ruling(
            "2020-01-24",
            "If any of the targeted creatures is an illegal target by the time Wrap in Flames resolves, it " +
                "won't be dealt damage and will be able to block. The other targeted creatures will still be affected."
        )
    }
}
