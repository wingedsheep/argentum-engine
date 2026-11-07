package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.dmu.cards.VesuvanDuplimancy
import com.wingedsheep.mtg.sets.definitions.j25.cards.FrontlineHeroism
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Whenever you cast a spell that targets …" watches spells that target — instants, sorceries and
 * Auras, the noncreature spells the spells axis is fed by — so the deckbuilder prices it by the
 * deck's pump spells and Auras, as it does an instant-or-sorcery trigger.
 */
class TargetingCastTriggerSynergyTest : FunSpec({

    test("a cast trigger on a spell that targets only a single creature you control rewards spells") {
        SynergyAxes.of(FrontlineHeroism).rewards shouldContain SynergyAxis.SPELLS
        SynergyAxes.of(VesuvanDuplimancy).rewards shouldContain SynergyAxis.SPELLS
    }
})
