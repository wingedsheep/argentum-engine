package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Whenever you cast a spell from your graveyard or activate an ability of a card in your
 * graveyard" (Neerdiv, Devious Diver; Secrets of the Dead for the cast half) pays off the
 * flashback, escape and embalm cards a stocked graveyard holds — a graveyard payoff, so the deck's
 * self-mill and surveil feed it.
 */
class GraveyardUseTriggerReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    test("a cast-from-graveyard or graveyard-activation trigger rewards the graveyard axis") {
        val neerdiv = card("Neerdiv, Devious Diver")
        GraveyardSynergy.isPayoff(neerdiv) shouldBe true
        SynergyAxes.of(neerdiv).rewards shouldContain SynergyAxis.GRAVEYARD
        withClue("both of Neerdiv's triggered abilities read fully") {
            AiReadingAudit.read(neerdiv).abilities.filter { it.checked }.all { it.readFully } shouldBe true
        }
        withClue("Secrets of the Dead: 'whenever you cast a spell from your graveyard'") {
            SynergyAxes.of(card("Secrets of the Dead")).rewards shouldContain SynergyAxis.GRAVEYARD
        }
    }

    test("a cast trigger from any other zone is not a graveyard payoff") {
        withClue("Young Pyromancer: a plain 'whenever you cast an instant or sorcery spell'") {
            SynergyAxes.of(card("Young Pyromancer")).rewards shouldNotContain SynergyAxis.GRAVEYARD
        }
    }
})
