package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Nim Devourer — Mirrodin #70
 * {3}{B}{B} · Creature — Zombie · 4/1
 *
 * This creature gets +1/+0 for each artifact you control.
 * {B}{B}: Return this card from your graveyard to the battlefield, then sacrifice a creature.
 * Activate only during your upkeep.
 *
 * The power boost is the shared Nim continuous effect ([NimShrieker], [NimLasher]) — a
 * [GrantDynamicStatsEffect] scoped to the source, recomputed from the live artifact count.
 * The Devourer is not itself an artifact, so it never counts toward its own bonus.
 *
 * The recursion is an *activated ability that functions from the graveyard*
 * (`activateFromZone = Zone.GRAVEYARD`), and the return and the sacrifice are both parts of the
 * **effect**, not the cost: the {B}{B} is all you pay to put it on the stack. That ordering
 * matters — the Devourer is back on the battlefield by the time the edict resolves, so it is a
 * legal (and, with an otherwise empty board, the only) sacrifice, which is exactly how the
 * printed card behaves. The sacrifice is `Player.You` choosing, not a targeted permanent, so it
 * follows the "sacrifice a creature" template rather than "sacrifice this creature".
 *
 * "Activate only during your upkeep" composes the two timing gates the engine already has:
 * `DuringStep(UPKEEP)` bounds the step and `OnlyDuringYourTurn` bounds whose upkeep it is —
 * either alone would let it fire on an opponent's upkeep or during your own later steps.
 */
val NimDevourer = card("Nim Devourer") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 1
    oracleText = "This creature gets +1/+0 for each artifact you control.\n" +
        "{B}{B}: Return this card from your graveyard to the battlefield, then sacrifice a creature. " +
        "Activate only during your upkeep."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{B}{B}")
        effect = Effects.Composite(
            listOf(
                Effects.PutOntoBattlefield(EffectTarget.Self),
                Effects.Sacrifice(
                    filter = GameObjectFilter.Creature,
                    count = 1,
                    target = EffectTarget.PlayerRef(Player.You)
                )
            )
        )
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.DuringStep(Step.UPKEEP),
            ActivationRestriction.OnlyDuringYourTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a037ef49-8849-41aa-aa6e-3ac6ee34cdad.jpg?1783944547"
    }
}
