package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.ExpeditionEnvoy
import com.wingedsheep.mtg.sets.definitions.bfz.cards.RecklessCohort
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Reckless Cohort (BFZ #152) — "This creature attacks each combat if able unless you control
 * another Ally."
 *
 * The first card to gate [com.wingedsheep.sdk.scripting.MustAttack] behind a
 * `ConditionalStaticAbility`: the requirement is projected only while you control no *other* Ally.
 * The Cohort is an Ally itself, so it must not exempt itself; an opponent's Ally doesn't exempt it;
 * and two Cohorts exempt each other.
 */
class RecklessCohortScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + RecklessCohort + ExpeditionEnvoy)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.readyCohort() =
        putCreatureOnBattlefield(player1, "Reckless Cohort").also { removeSummoningSickness(it) }

    fun GameTestDriver.declareNoAttackers() =
        submit(DeclareAttackers(playerId = player1, attackers = emptyMap()))

    test("with no other Ally, it must attack") {
        val d = driver()
        val cohort = d.readyCohort()
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val none = d.declareNoAttackers()
        withClue("the Cohort is an Ally itself but doesn't count as \"another\" one") {
            none.outcome shouldNotBe Outcome.Done
            none.error shouldContain "must attack"
        }

        d.submit(DeclareAttackers(playerId = d.player1, attackers = mapOf(cohort to d.player2)))
            .outcome shouldBe Outcome.Done
    }

    test("another Ally you control frees it from attacking") {
        val d = driver()
        d.readyCohort()
        d.putCreatureOnBattlefield(d.player1, "Expedition Envoy")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val none = d.declareNoAttackers()
        withClue(none.error ?: "") { none.outcome shouldBe Outcome.Done }
    }

    test("an Ally an opponent controls doesn't free it") {
        val d = driver()
        d.readyCohort()
        d.putCreatureOnBattlefield(d.player2, "Expedition Envoy")
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val none = d.declareNoAttackers()
        none.outcome shouldNotBe Outcome.Done
        none.error shouldContain "must attack"
    }

    test("two Cohorts are each another Ally to the other, so neither must attack") {
        val d = driver()
        d.readyCohort()
        d.readyCohort()
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val none = d.declareNoAttackers()
        withClue(none.error ?: "") { none.outcome shouldBe Outcome.Done }
    }
})
