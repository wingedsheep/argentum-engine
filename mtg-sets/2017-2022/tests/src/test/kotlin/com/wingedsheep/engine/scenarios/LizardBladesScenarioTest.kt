package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.LizardBlades
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lizard Blades — {1}{R} Artifact Creature — Equipment Lizard 1/1
 * Double strike. Equipped creature has double strike. Reconfigure {2}.
 */
class LizardBladesScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + LizardBlades)
        initMirrorMatch(deck = Deck.of("Mountain" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("a double-striking creature that reconfigures onto another creature and back") {
        val d = driver()
        val blades = d.putPermanentOnBattlefield(d.player1, LizardBlades.name)
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.state.projectedState.isCreature(blades) shouldBe true
        d.state.projectedState.hasKeyword(blades, Keyword.DOUBLE_STRIKE) shouldBe true

        val (attach, unattach) = LizardBlades.activatedAbilities
        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, blades, attach.id, listOf(ChosenTarget.Permanent(bears))))
            .outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.projectedState.isCreature(blades) shouldBe false
        d.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true

        d.giveColorlessMana(d.player1, 2)
        d.submit(ActivateAbility(d.player1, blades, unattach.id)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.state.projectedState.isCreature(blades) shouldBe true
        d.state.projectedState.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
    }
})
