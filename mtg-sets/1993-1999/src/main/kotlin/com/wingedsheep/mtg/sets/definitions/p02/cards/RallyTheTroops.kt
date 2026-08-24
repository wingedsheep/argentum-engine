package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rally the Troops
 * {W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Untap all creatures you control.
 *
 * The Portal "combat trick" timing pair: [com.wingedsheep.sdk.dsl.SpellBuilder.castOnlyDuring] plus
 * `castOnlyIf(YouWereAttackedThisStep)`. "All creatures you control" is [Effects.ForEachInGroup]
 * with the untap aimed at [EffectTarget.Self] — the current iteration entity.
 */
val RallyTheTroops = card("Rally the Troops") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText =
        "Cast this spell only during the declare attackers step and only if you've been attacked this step.\n" +
        "Untap all creatures you control."

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.Untap(EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61d92577-c3e2-4129-8974-89eb896cdc2d.jpg"
    }
}
