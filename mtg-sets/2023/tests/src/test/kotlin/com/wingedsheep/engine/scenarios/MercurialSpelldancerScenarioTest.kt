package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Mercurial Spelldancer (ONE #61) — {1}{U} 2/1 Creature — Phyrexian Rogue.
 *
 * "This creature can't be blocked. Whenever you cast a noncreature spell, put an oil counter on this
 *  creature. Whenever this creature deals combat damage to a player, you may remove two oil counters
 *  from it. If you do, when you next cast an instant or sorcery spell this turn, copy that spell.
 *  You may choose new targets for the copy."
 */
class MercurialSpelldancerScenarioTest : ScenarioTestBase() {

    private fun TestGame.oil(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    private fun TestGame.setOil(id: EntityId, amount: Int) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent(mapOf(CounterType.OIL to amount))) }
    }

    private fun board(): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Mercurial Spelldancer")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Island", 6)
            .withCardsInHand(1, "Divination", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(12) { b = b.withCardInLibrary(1, "Island") }
        repeat(4) { b = b.withCardInLibrary(2, "Island") }
        return b.build()
    }

    /** Attack, let combat damage happen, and answer the may as told. Returns whether it was asked. */
    private fun TestGame.attackAndAnswer(accept: Boolean): Boolean {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Mercurial Spelldancer" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var asked = false
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 20) {
            when (state.pendingDecision) {
                null -> resolveStack()
                is YesNoDecision -> { asked = true; answerYesNo(accept).error shouldBe null }
                else -> error("unexpected decision ${state.pendingDecision}")
            }
        }
        return asked
    }

    private fun TestGame.castDivinationAndResolve() {
        castSpell(1, "Divination").error shouldBe null
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 20) {
            if (state.pendingDecision != null) error("unexpected decision ${state.pendingDecision}")
            resolveStack()
        }
    }

    init {
        test("it can't be blocked") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Mercurial Spelldancer" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Mercurial Spelldancer"))).error shouldNotBe null
        }

        test("casting a noncreature spell adds an oil counter") {
            val game = board()
            val dancer = game.findPermanent("Mercurial Spelldancer")!!
            game.castDivinationAndResolve()
            game.oil(dancer) shouldBe 1
        }

        test("removing two oil counters copies only the next instant or sorcery (one-shot)") {
            val game = board()
            val dancer = game.findPermanent("Mercurial Spelldancer")!!
            game.setOil(dancer, 2)

            withClue("the may was offered") { game.attackAndAnswer(accept = true) shouldBe true }
            game.getLifeTotal(2) shouldBe 18
            game.oil(dancer) shouldBe 0

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val before = game.handSize(1)
            game.castDivinationAndResolve()
            withClue("original + copy drew four") { game.handSize(1) shouldBe before - 1 + 4 }
            withClue("the copy is not cast, so only one oil counter") { game.oil(dancer) shouldBe 1 }

            val mid = game.handSize(1)
            game.castDivinationAndResolve()
            withClue("one-shot: the second spell is not copied") { game.handSize(1) shouldBe mid - 1 + 2 }
        }

        test("declining keeps the counters and copies nothing") {
            val game = board()
            val dancer = game.findPermanent("Mercurial Spelldancer")!!
            game.setOil(dancer, 2)

            game.attackAndAnswer(accept = false) shouldBe true
            game.oil(dancer) shouldBe 2

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val before = game.handSize(1)
            game.castDivinationAndResolve()
            game.handSize(1) shouldBe before - 1 + 2
        }

        test("with only one oil counter the removal can't be paid, so nothing is offered") {
            val game = board()
            val dancer = game.findPermanent("Mercurial Spelldancer")!!
            game.setOil(dancer, 1)

            game.attackAndAnswer(accept = true) shouldBe false
            game.oil(dancer) shouldBe 1

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val before = game.handSize(1)
            game.castDivinationAndResolve()
            game.handSize(1) shouldBe before - 1 + 2
        }
    }
}
