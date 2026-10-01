package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Noxious Assault — Phyrexia: All Will Be One #176
 * {3}{G}{G}
 * Sorcery
 * Creatures you control get +2/+2 until end of turn. Whenever a creature blocks this turn, its
 * controller gets a poison counter.
 *
 * The rider is a filter-scoped delayed trigger over `Triggers.a(Creature).blocks()`: it fans out one
 * trigger per declared blocker, so [EffectTarget.ControllerOfTriggeringEntity] names each blocker's
 * controller — any creature, whoever controls it, for the rest of the turn.
 */
val NoxiousAssault = card("Noxious Assault") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Creatures you control get +2/+2 until end of turn. Whenever a creature blocks this turn, its controller gets a poison counter."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity)
        ) then Effects.CreateDelayedTrigger(
            effect = Effects.AddCounters(CounterType.POISON, 1, EffectTarget.ControllerOfTriggeringEntity),
            trigger = Triggers.a(GameObjectFilter.Creature).blocks(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Billy Christian"
        flavorText = "Even as they brought the beast down, they could feel its poison working into their blood, altering their bodies, and filling their minds with the thrill of the eternal hunt."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9649de6c-a9f7-4f0a-8bf7-cacaea60ed54.jpg?1783918012"
    }
}
