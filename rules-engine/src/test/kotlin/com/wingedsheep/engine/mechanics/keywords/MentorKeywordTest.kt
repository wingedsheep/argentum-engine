package com.wingedsheep.engine.mechanics.keywords

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Mentor (CR 702.134a): "Whenever this creature attacks, put a +1/+1 counter on target attacking
 * creature with lesser power." Lesser power is checked as the trigger goes on the stack and again
 * on resolution (CR 608.2b); a mentor creature that has left the battlefield is compared by its
 * last-known power — including a token, which is swept out of existence (CR 704.5d) before its
 * trigger resolves.
 */
class MentorKeywordTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.attackWithMentor(vararg others: String) {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers((listOf("Mentor Test Knight") + others).associateWith { 2 }).error shouldBe null
    }

    init {
        cardRegistry.register(card("Mentor Test Knight") {
            manaCost = "{2}{W}"
            typeLine = "Creature — Human Knight"
            power = 3
            toughness = 3
            mentor()
        })

        test("only attacking creatures with lesser power are legal targets") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mentor Test Knight")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val elves = game.findPermanent("Llanowar Elves")!!

            // Savannah Lions stays home: lesser power, but not attacking.
            game.attackWithMentor("Grizzly Bears", "Llanowar Elves", "Hill Giant")
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(bears, elves)

            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()
            game.plusOneCounters(bears) shouldBe 1
        }

        test("a target that is no longer smaller on resolution gets no counter") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mentor Test Knight")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Giant Growth")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithMentor("Grizzly Bears")
            if (game.getPendingDecision() != null) game.selectTargets(listOf(bears))
            game.castSpell(1, "Giant Growth", bears).error shouldBe null
            game.resolveStack()

            game.plusOneCounters(bears) shouldBe 0
        }

        test("a mentor token killed in response is compared by its last-known power") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Mentor Test Knight", isToken = true)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val knight = game.findPermanent("Mentor Test Knight")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.attackWithMentor("Grizzly Bears")
            if (game.getPendingDecision() != null) game.selectTargets(listOf(bears))
            game.castSpell(1, "Lightning Bolt", knight).error shouldBe null
            game.resolveStack()

            game.state.getEntity(knight) shouldBe null
            game.plusOneCounters(bears) shouldBe 1
        }
    }
}
