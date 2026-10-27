package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vine Gecko — Zendikar Rising #219
 * {1}{G} · Creature — Elemental Lizard · 2/2
 *
 * The first kicked spell you cast each turn costs {1} less to cast.
 * Whenever you cast a kicked spell, put a +1/+1 counter on this creature.
 *
 * The discount is [CostGating.NthOfTypePerTurn] narrowed to casts that declared their kicker
 * ([ChoiceSlot.KICKED]): a kicked spell cast earlier this turn — even before the Gecko was on the
 * battlefield — uses up "the first", while unkicked spells don't count.
 */
val VineGecko = card("Vine Gecko") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental Lizard"
    power = 2
    toughness = 2
    oracleText = "The first kicked spell you cast each turn costs {1} less to cast.\n" +
        "Whenever you cast a kicked spell, put a +1/+1 counter on this creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.NthOfTypePerTurn(1, declaredCost = ChoiceSlot.KICKED),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "219"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a6f9984-dec7-4a1f-91d6-1e85b5df5e4c.jpg?1783929326"
    }
}
