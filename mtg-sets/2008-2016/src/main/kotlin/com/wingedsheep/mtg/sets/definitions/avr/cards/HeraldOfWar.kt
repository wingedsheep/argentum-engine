package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Herald of War — Avacyn Restored #24
 * {3}{W}{W} · Creature — Angel · 3/3
 * Flying
 * Whenever this creature attacks, put a +1/+1 counter on it.
 * Angel spells and Human spells you cast cost {1} less to cast for each +1/+1 counter on this creature.
 *
 * The discount is Animar, Soul of Elements' shape: [CostReductionSource.Dynamic] over the source's
 * own +1/+1 counters (all of them, not only the ones its attack trigger put there), reducing generic
 * mana only. A spell that is both an Angel and a Human matches the union filter once, so it is
 * discounted once.
 */
val HeraldOfWar = card("Herald of War") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever this creature attacks, put a +1/+1 counter on it.\n" +
        "Angel spells and Human spells you cast cost {1} less to cast for each +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(
                GameObjectFilter.Any.withSubtype("Angel") or GameObjectFilter.Any.withSubtype("Human")
            ),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE))
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77e92bbb-c22d-4879-9437-b87a3ff70a2d.jpg?1783940731"
        ruling("2012-05-01", "The cost reduction is based on the total number of +1/+1 counters on Herald of War, not just the ones put on it by its own ability.")
        ruling("2012-05-01", "A creature spell that's both an Angel and a Human will cost {1} less to cast for each +1/+1 counter on Herald of War.")
    }
}
