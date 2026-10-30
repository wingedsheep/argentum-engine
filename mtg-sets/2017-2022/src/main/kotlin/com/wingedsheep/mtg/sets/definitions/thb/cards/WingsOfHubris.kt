package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wings of Hubris
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature has flying.
 * Sacrifice this Equipment: Equipped creature can't be blocked this turn. Sacrifice it at the
 * beginning of the next end step.
 * Equip {1}
 *
 * The Equipment is sacrificed as the cost, so on resolution "equipped creature" is the creature it
 * was attached to as it last existed on the battlefield (CR 608.2h). The ability gathers that host
 * once ([CardSource.Entity] over [EffectTarget.EquippedCreature]) and runs the rest per gathered
 * creature — Push // Pull's shape — so the delayed end-step sacrifice is bound to *that* creature
 * through the loop's [EffectTarget.IterationEntity], not re-read off an Equipment that is long gone.
 * The sacrifice only happens if you control the creature then ([Effects.SacrificeTarget] sacrifices
 * only the resolving player's permanents), matching the ruling for a host you don't control.
 */
val WingsOfHubris = card("Wings of Hubris") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has flying.\n" +
        "Sacrifice this Equipment: Equipped creature can't be blocked this turn. Sacrifice it at the " +
        "beginning of the next end step.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.Pipeline {
            val host = gather(CardSource.Entity(EffectTarget.EquippedCreature), name = "wingedCreature")
            run(
                Effects.ForEachInCollection(
                    host,
                    Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.IterationEntity) then
                        Effects.CreateDelayedTrigger(
                            step = Step.END,
                            effect = Effects.SacrificeTarget(EffectTarget.IterationEntity),
                        ),
                ),
            )
        }
        description = "Sacrifice this Equipment: Equipped creature can't be blocked this turn. Sacrifice it " +
            "at the beginning of the next end step."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "241"
        artist = "Josh Hass"
        flavorText = "\"Dalakos's invention worked . . . until it didn't.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3ef8f96-3e3c-4b1b-8180-3b38c7deaaff.jpg?1783931512"
        ruling(
            "2020-01-24",
            "If Wings of Hubris becomes attached to a creature you don't control, you can activate the ability " +
                "to make that creature unblockable, but you won't be able to sacrifice the creature at the " +
                "beginning of the next end step."
        )
        ruling(
            "2020-01-24",
            "If you gain control of a creature \"until end of turn,\" you control it during your end step. If " +
                "you use Wings of Hubris to make this creature fly too close to the sun, you'll sacrifice it."
        )
    }
}
