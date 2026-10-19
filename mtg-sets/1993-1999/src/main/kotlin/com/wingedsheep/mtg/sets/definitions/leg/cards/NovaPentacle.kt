package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser

/**
 * Nova Pentacle — Legends #289
 * {4} · Artifact
 *
 * {3}, {T}: The next time a source of your choice would deal damage to you this turn, that damage
 * is dealt to target creature of an opponent's choice instead.
 *
 * The opponent picks the creature as the ability is activated ([TargetChooser.Opponent]) — any
 * creature, including one of yours; the source is chosen on resolution, the same shield Beacon of
 * Destiny and Jade Monolith build, here pointed at the targeted creature.
 */
val NovaPentacle = card("Nova Pentacle") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: The next time a source of your choice would deal damage to you this turn, " +
        "that damage is dealt to target creature of an opponent's choice instead."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target(TargetFilter.Creature, chooser = TargetChooser.Opponent)
        effect = Effects.RedirectDamageFromChosenSource(
            protectedTarget = EffectTarget.Controller,
            redirectTo = creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Richard Thomas"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc60e209-aa29-48aa-9128-9bb175403c0c.jpg?1783948027"
    }
}
