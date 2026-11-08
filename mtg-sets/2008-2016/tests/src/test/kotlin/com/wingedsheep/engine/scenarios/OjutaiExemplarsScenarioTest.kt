package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.OjutaiExemplars
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ojutai Exemplars (DTK) — "Whenever you cast a noncreature spell, choose one — tap target
 * creature; or this creature gains first strike and lifelink until end of turn; or exile this
 * creature, then return it to the battlefield tapped under its owner's control."
 *
 * Each mode is exercised off a Giant Growth cast on the Exemplars. The blink mode is the one that
 * depends on engine execution: the returned Exemplars is tapped and a new object (CR 400.7), so the
 * Giant Growth still on the stack loses its target and doesn't pump it.
 */
class OjutaiExemplarsScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + OjutaiExemplars)
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Casts Giant Growth on [exemplars] and returns the trigger's mode choice. */
    fun GameTestDriver.castGrowthOn(exemplars: EntityId): ChooseOptionDecision {
        val growth = putCardInHand(player1, "Giant Growth")
        giveMana(player1, Color.GREEN)
        castSpell(player1, growth, listOf(exemplars)).error shouldBe null
        var guard = 0
        while (guard++ < 10 && pendingDecision !is ChooseOptionDecision) {
            check(pendingDecision == null) { "unexpected decision $pendingDecision" }
            bothPass()
        }
        return pendingDecision as ChooseOptionDecision
    }

    fun GameTestDriver.chooseMode(decision: ChooseOptionDecision, text: String) {
        val index = decision.options.indexOfFirst { it.contains(text) }
        check(index >= 0) { "mode '$text' not offered; options=${decision.options}" }
        submitDecision(player1, OptionChosenResponse(decision.id, index)).error shouldBe null
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (guard++ < 20 && pendingDecision == null && stackSize > 0) bothPass()
    }

    test("tap mode taps the target creature") {
        val d = driver()
        val exemplars = d.putCreatureOnBattlefield(d.player1, "Ojutai Exemplars")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")

        d.chooseMode(d.castGrowthOn(exemplars), "Tap target creature")
        (d.pendingDecision as ChooseTargetsDecision).legalTargets.getValue(0) shouldContain courser
        d.submitTargetSelection(d.player1, listOf(courser)).error shouldBe null
        d.resolveStack()

        d.isTapped(courser) shouldBe true
        withClue("only the target is tapped") { d.isTapped(exemplars) shouldBe false }
    }

    test("first strike and lifelink mode lasts until end of turn") {
        val d = driver()
        val exemplars = d.putCreatureOnBattlefield(d.player1, "Ojutai Exemplars")

        d.chooseMode(d.castGrowthOn(exemplars), "first strike and lifelink")
        d.resolveStack()

        val projected = d.state.projectedState
        projected.hasKeyword(exemplars, Keyword.FIRST_STRIKE) shouldBe true
        projected.hasKeyword(exemplars, Keyword.LIFELINK) shouldBe true
        withClue("Giant Growth resolved on the same object") { projected.getPower(exemplars) shouldBe 7 }

        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.hasKeyword(exemplars, Keyword.FIRST_STRIKE) shouldBe false
        d.state.projectedState.hasKeyword(exemplars, Keyword.LIFELINK) shouldBe false
    }

    test("blink mode returns the Exemplars tapped as a new object, so Giant Growth loses its target") {
        val d = driver()
        val exemplars = d.putCreatureOnBattlefield(d.player1, "Ojutai Exemplars")
        d.isTapped(exemplars) shouldBe false

        d.chooseMode(d.castGrowthOn(exemplars), "Exile Ojutai Exemplars")
        d.resolveStack()

        val returned = d.findPermanent(d.player1, "Ojutai Exemplars")
        withClue("the Exemplars is back on the battlefield") { returned shouldNotBe null }
        d.getExileCardNames(d.player1) shouldNotContain "Ojutai Exemplars"
        withClue("it returns tapped") { d.isTapped(returned!!) shouldBe true }
        withClue("Giant Growth's target is gone, so the new Exemplars isn't pumped") {
            d.state.projectedState.getPower(returned!!) shouldBe 4
        }
        d.getGraveyardCardNames(d.player1) shouldContain "Giant Growth"
    }
})
