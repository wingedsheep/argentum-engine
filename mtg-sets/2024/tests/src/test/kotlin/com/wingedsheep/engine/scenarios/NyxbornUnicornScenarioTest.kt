package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class NyxbornUnicornScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.bestowOnto(host: EntityId) {
        execute(
            CastSpell(
                playerId = player1Id,
                cardId = findCardsInHand(1, "Nyxborn Unicorn").single(),
                targets = listOf(ChosenTarget.Permanent(host)),
                useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.BESTOW
            )
        ).error shouldBe null
        resolveStack()
    }

    init {
        test("cast as a creature, it mentors an attacking creature with lesser power") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nyxborn Unicorn")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val elves = game.findPermanent("Llanowar Elves")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Nyxborn Unicorn" to 2, "Llanowar Elves" to 2)).error shouldBe null
            if (game.getPendingDecision() != null) game.selectTargets(listOf(elves))
            game.resolveStack()

            game.plusOneCounters(elves) shouldBe 1
        }

        test("bestowed, the enchanted creature gets +2/+2 and mentors with its own power") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Nyxborn Unicorn")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.bestowOnto(bears)

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            projected.hasKeyword(bears, Keyword.MENTOR) shouldBe true

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            // The 4-power Bears can mentor the 3-power Giant; nothing else attacks.
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(giant)
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.plusOneCounters(giant) shouldBe 1
        }

        test("a mentor host that leaves in response is compared by its last-known power") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Nyxborn Unicorn")
                .withCardInHand(1, "Unsummon")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            game.bestowOnto(bears)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.selectTargets(listOf(giant)).error shouldBe null
            // Bounced, the Bears' printed power (2) is below the Giant's — only its last-known 4 qualifies.
            game.castSpell(1, "Unsummon", bears).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.plusOneCounters(giant) shouldBe 1
        }
    }
}
