package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Exile an instant or sorcery spell you control: …" (Nivmagus Elemental) spends the deck's spells
 * as its activation cost, so it rewards [SynergyAxis.SPELLS]; a graveyard exile cost does not.
 */
class SpellExileCostSynergyTest : FunSpec({

    test("an exile-a-spell-you-control cost rewards spells") {
        val nivmagus = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == "Nivmagus Elemental" }
        SynergyAxes.of(nivmagus).rewards shouldContain SynergyAxis.SPELLS
    }

    test("a graveyard exile cost rewards no spells") {
        val probe = card("Graveyard Exile Cost Probe") {
            manaCost = "{1}"
            typeLine = "Creature — Elemental"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.ExileFromGraveyard(1)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            }
        }
        SynergyAxes.of(probe).rewards shouldNotContain SynergyAxis.SPELLS
    }
})
