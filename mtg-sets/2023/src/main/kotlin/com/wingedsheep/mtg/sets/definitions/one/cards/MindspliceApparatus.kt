package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
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
 * Mindsplice Apparatus
 * {3}{U}
 * Artifact
 *
 * Flash
 * At the beginning of your upkeep, put an oil counter on this artifact.
 * Instant and sorcery spells you cast cost {1} less to cast for each oil counter on this artifact.
 *
 * The discount is a battlefield-sourced [SpellCostTarget.YouCast] modifier whose amount reads the
 * artifact's own oil counters at cast time ([CostReductionSource.Dynamic] over
 * [DynamicAmounts.countersOnSelf]). Only generic mana is reduced (CR 118.7a).
 */
val MindspliceApparatus = card("Mindsplice Apparatus") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "Flash\n" +
        "At the beginning of your upkeep, put an oil counter on this artifact.\n" +
        "Instant and sorcery spells you cast cost {1} less to cast for each oil counter on this artifact."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.AddCounters(CounterType.OIL, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.InstantOrSorcery),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.countersOnSelf(CounterType.OIL))
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "63"
        artist = "Ovidio Cartagena"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f4ad1cb-4bbb-4485-b322-0b003f06d034.jpg?1783918059"
    }
}
