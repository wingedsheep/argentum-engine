package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.StaticPrison
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Static Prison (MH3) — "When this enchantment enters, exile target nonland permanent an opponent
 * controls until this enchantment leaves the battlefield. You get {E}{E}. / At the beginning of your
 * first main phase, sacrifice this enchantment unless you pay {E}."
 */
class StaticPrisonScenarioTest : FunSpec({
    fun newDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + StaticPrison)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    /** Casts Static Prison exiling the opponent's Grizzly Bears; returns the prison's id. */
    fun GameTestDriver.castPrisonOnBears(): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val bears = putCreatureOnBattlefield(player2, "Grizzly Bears")
        val prison = putCardInHand(player1, "Static Prison")
        giveMana(player1, Color.WHITE, 1)
        castSpell(player1, prison).error shouldBe null
        bothPass() // resolve the enchantment; ETB trigger goes on the stack
        (pendingDecision as? ChooseTargetsDecision)?.let { submitTargetSelection(player1, listOf(bears)) }
        bothPass() // resolve the ETB trigger
        return prison to bears
    }

    /** Advance from player1's turn-1 main phase to player1's next first main phase. */
    fun GameTestDriver.toNextOwnFirstMain() {
        passPriorityUntil(Step.UPKEEP) // opponent's upkeep
        passPriorityUntil(Step.END)
        passPriorityUntil(Step.UPKEEP) // our upkeep
        activePlayer shouldBe player1
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        bothPass() // first-main trigger resolves
    }

    test("entering exiles the opponent's permanent and gets two energy") {
        val d = newDriver()
        val (_, bears) = d.castPrisonOnBears()
        d.getExile(d.player2) shouldContain bears
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
        d.energy() shouldBe 2
    }

    test("paying one energy at the next first main phase keeps the prison and the exile") {
        val d = newDriver()
        val (prison, _) = d.castPrisonOnBears()
        d.toNextOwnFirstMain()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        question.playerId shouldBe d.player1
        d.submitDecision(d.player1, YesNoResponse(question.id, true)).error shouldBe null
        d.energy() shouldBe 1
        d.getPermanents(d.player1) shouldContain prison
        d.findPermanent(d.player2, "Grizzly Bears") shouldBe null
    }

    test("declining to pay sacrifices the prison and the exiled card returns") {
        val d = newDriver()
        val (prison, _) = d.castPrisonOnBears()
        d.toNextOwnFirstMain()
        val question = d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitDecision(d.player1, YesNoResponse(question.id, false)).error shouldBe null
        d.bothPass() // resolve the leaves-the-battlefield return trigger
        d.energy() shouldBe 2
        d.getPermanents(d.player1) shouldNotContain prison
        d.getGraveyardCardNames(d.player1) shouldContain "Static Prison"
        (d.findPermanent(d.player2, "Grizzly Bears") != null) shouldBe true
    }

    test("with no energy it is simply sacrificed") {
        val d = GameTestDriver().also {
            it.registerCards(TestCards.all + StaticPrison)
            it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
            it.passPriorityUntil(Step.UPKEEP)
        }
        val prison = d.putPermanentOnBattlefield(d.player1, "Static Prison")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.bothPass()
        d.pendingDecision shouldBe null
        d.getPermanents(d.player1) shouldNotContain prison
        d.getGraveyardCardNames(d.player1) shouldContain "Static Prison"
    }
})
