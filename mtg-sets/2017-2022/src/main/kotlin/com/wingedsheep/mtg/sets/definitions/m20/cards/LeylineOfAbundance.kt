package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayBeginGameOnBattlefield
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leyline of Abundance — Core Set 2020 #179
 * {2}{G}{G} · Enchantment
 *
 * If this card is in your opening hand, you may begin the game with it on the battlefield.
 * Whenever you tap a creature for mana, add an additional {G}.
 * {6}{G}{G}: Put a +1/+1 counter on each creature you control.
 *
 * The mana rider is Badgermole Cub's triggered mana ability: [AdditionalManaOnSourceTap] over
 * creatures you control, which only fires for a mana ability with {T} in its cost (the 2019-07-12
 * ruling).
 */
val LeylineOfAbundance = card("Leyline of Abundance") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If this card is in your opening hand, you may begin the game with it on the battlefield.\n" +
        "Whenever you tap a creature for mana, add an additional {G}.\n" +
        "{6}{G}{G}: Put a +1/+1 counter on each creature you control."

    mayBeginGameOnBattlefield()

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Creature.youControl(),
            color = Color.GREEN
        )
    }

    activatedAbility {
        cost = Costs.Mana("{6}{G}{G}")
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "{6}{G}{G}: Put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c68e8342-78d2-4826-a287-64c371b97d19.jpg?1783932962"
        ruling("2019-07-12", "An ability that triggers \"whenever you tap a creature for mana\" triggers only if you activate a mana ability of a creature including {T} in its cost. Mana abilities that don't include the {T} symbol and instead say \"Tap an untapped creature you control\" or similar won't cause Leyline of Abundance's second ability to trigger. Similarly, it won't trigger if you tap a creature to activate a mana ability of another object (even if that mana ability also includes {T}).")
    }
}
