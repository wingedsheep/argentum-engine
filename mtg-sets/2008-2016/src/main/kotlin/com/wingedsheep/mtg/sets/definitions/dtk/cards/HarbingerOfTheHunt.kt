package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Harbinger of the Hunt
 * {3}{R}{G}
 * Creature — Dragon
 * 5/3
 *
 * Flying
 * {2}{R}: This creature deals 1 damage to each creature without flying.
 * {2}{G}: This creature deals 1 damage to each other creature with flying.
 *
 * The two sweeps are asymmetric on purpose: the red one has no "other", so a Harbinger that has lost
 * flying damages itself; the green one excludes the source (`excludeSelf = true`). Both read flying
 * from projected state at resolution, and the Harbinger is the damage source.
 */
val HarbingerOfTheHunt = card("Harbinger of the Hunt") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 3
    oracleText = "Flying\n" +
        "{2}{R}: This creature deals 1 damage to each creature without flying.\n" +
        "{2}{G}: This creature deals 1 damage to each other creature with flying."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)),
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING), excludeSelf = true),
            Effects.DealDamage(1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Aaron Miller"
        flavorText = "An Atarka dragon's exhale cooks what its inhale consumes."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbeb21b8-8817-48c2-8ae8-25221600ce32.jpg?1783938571"
    }
}
