package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ths.cards.MeletisCharlatan
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Meletis Charlatan (THS #54) — "{2}{U}, {T}: The controller of target instant or sorcery spell
 * copies it. That player may choose new targets for the copy."
 *
 * The copy belongs to the targeted spell's controller, not to the Charlatan's: copying an
 * opponent's Lightning Bolt gives *them* a second Bolt and *them* the retarget decision.
 */
class MeletisCharlatanScenarioTest : FunSpec({

    val abilityId = MeletisCharlatan.script.activatedAbilities.single().id

    /** Player 2 is active and casts Lightning Bolt at player 1, who copies it with the Charlatan. */
    fun opponentBoltCopied(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MeletisCharlatan)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.player1
        val opp = d.player2
        val charlatan = d.putPermanentOnBattlefield(me, "Meletis Charlatan")
        d.removeSummoningSickness(charlatan)

        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.castSpellWithTargets(opp, bolt, listOf(ChosenTarget.Player(me))).error shouldBe null
        d.passPriority(opp)
        d.priorityPlayer shouldBe me

        d.giveMana(me, Color.BLUE, 3)
        d.submit(ActivateAbility(me, charlatan, abilityId, targets = listOf(ChosenTarget.Spell(bolt))))
            .error shouldBe null
        d.bothPass() // the Charlatan's ability resolves
        return Triple(d, charlatan, bolt)
    }

    fun GameTestDriver.copyOnStack() = state.stack.single { state.getEntity(it)?.has<CopyOfComponent>() == true }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("the spell's controller gets the copy and the retarget decision") {
        val (d, _, _) = opponentBoltCopied()
        val decision = d.pendingDecision
        withClue("the opponent, who controls the Bolt, chooses the copy's new targets") {
            (decision is ChooseTargetsDecision) shouldBe true
            (decision as ChooseTargetsDecision).playerId shouldBe d.player2
        }
        d.submitTargetSelection(d.player2, emptyList()).error shouldBe null

        val copy = d.copyOnStack()
        withClue("the copy is controlled by the Bolt's controller, keeping its target") {
            d.state.getEntity(copy)!!.get<SpellOnStackComponent>()!!.casterId shouldBe d.player2
            d.state.getEntity(copy)!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Player(d.player1))
        }
        d.resolveAll()
        withClue("both Bolts hit the Charlatan's controller") {
            d.getLifeTotal(d.player1) shouldBe 14
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("the spell's controller may aim the copy somewhere new") {
        val (d, charlatan, _) = opponentBoltCopied()
        d.submitTargetSelection(d.player2, listOf(charlatan)).error shouldBe null
        d.resolveAll()
        withClue("the copy killed the Charlatan and the original still hit its controller") {
            d.findPermanent(d.player1, "Meletis Charlatan") shouldBe null
            d.getLifeTotal(d.player1) shouldBe 17
        }
    }
})
