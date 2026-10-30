package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vial Smasher the Fierce (C16 #49) — {1}{B}{R} Legendary Creature — Goblin Berserker, 2/3.
 *
 *   "Whenever you cast your first spell each turn, choose an opponent at random. Vial Smasher
 *    deals damage equal to that spell's mana value to that player or a planeswalker that player
 *    controls."
 *
 * The opponent is drawn by the engine (no decision); the controller is then asked about a
 * planeswalker only when that opponent controls one, and may decline to hit the player instead.
 */
class VialSmasherTheFierceScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyalty(id: EntityId): Int =
        state.getEntity(id)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY)

    init {
        context("Vial Smasher the Fierce") {

            test("first spell each turn deals its mana value to the opponent; the second spell doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vial Smasher the Fierce")
                    .withCardInHand(1, "Hill Giant") // {3}{R}, mana value 4
                    .withCardInHand(1, "Raging Goblin") // {R}, mana value 1
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                withClue("The sole opponent is picked without a decision") {
                    game.hasPendingDecision() shouldBe false
                }
                game.getLifeTotal(2) shouldBe 16
                game.getLifeTotal(1) shouldBe 20

                game.castSpell(1, "Raging Goblin").error shouldBe null
                game.resolveStack()
                withClue("The second spell this turn doesn't trigger Vial Smasher") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("opponent with a planeswalker: controller chooses it and the damage goes there") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vial Smasher the Fierce")
                    .withCardOnBattlefield(2, "Liliana Vess") // loyalty 5
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val liliana = game.findPermanent("Liliana Vess")!!

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                decision.playerId shouldBe game.player1Id
                decision.legalTargets[0] shouldBe listOf(liliana)
                withClue("The player remains a choice: the planeswalker may be declined") {
                    decision.targetRequirements.single().minTargets shouldBe 0
                }

                game.selectTargets(listOf(liliana)).error shouldBe null
                game.resolveStack()

                game.loyalty(liliana) shouldBe 1
                game.getLifeTotal(2) shouldBe 20
            }

            test("opponent with a planeswalker: declining it deals the damage to the player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vial Smasher the Fierce")
                    .withCardOnBattlefield(2, "Liliana Vess")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val liliana = game.findPermanent("Liliana Vess")!!

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
                game.loyalty(liliana) shouldBe 5
            }

            test("a planeswalker the caster controls is never offered") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vial Smasher the Fierce")
                    .withCardOnBattlefield(1, "Liliana Vess")
                    .withCardInHand(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val liliana = game.findPermanent("Liliana Vess")!!

                game.castSpell(1, "Hill Giant").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.getLifeTotal(2) shouldBe 16
                game.loyalty(liliana) shouldBe 5
            }

            test("multiplayer: only the randomly chosen opponent's planeswalker is offered, and declining hits that opponent") {
                val d = GameTestDriver()
                d.registerCards(TestCards.all)
                val (me, oppA, oppB) = d.initMultiplayer(
                    decks = List(3) { Deck.of("Mountain" to 40) },
                    startingPlayer = 0,
                )
                d.passPriorityUntil(Step.PRECOMBAT_MAIN)
                d.putCreatureOnBattlefield(me, "Vial Smasher the Fierce")
                val walkerA = d.putPermanentOnBattlefield(oppA, "Liliana Vess")
                val walkerB = d.putPermanentOnBattlefield(oppB, "Liliana Vess")
                for (walker in listOf(walkerA, walkerB)) {
                    d.addComponent(walker, CountersComponent(mapOf(CounterType.LOYALTY to 5)))
                }
                repeat(4) { d.putLandOnBattlefield(me, "Mountain") }
                val giant = d.putCardInHand(me, "Hill Giant")

                d.castSpell(me, giant).error shouldBe null
                var guard = 0
                while (d.pendingDecision == null && d.stackSize > 0 && guard++ < 20) d.passPriority(d.state.priorityPlayerId!!)

                val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val offered = decision.legalTargets[0]!!
                withClue("exactly one planeswalker — the drawn opponent's — is offered, never both") {
                    offered.size shouldBe 1
                }
                val (chosen, other) = if (offered.single() == walkerA) oppA to oppB else oppB to oppA
                offered.single() shouldBe (if (chosen == oppA) walkerA else walkerB)

                d.submitTargetSelection(me, emptyList()).error shouldBe null
                guard = 0
                while (d.stackSize > 0 && guard++ < 20) d.passPriority(d.state.priorityPlayerId!!)

                withClue("declining sends the damage to the opponent who was drawn") {
                    d.getLifeTotal(chosen) shouldBe 16
                    d.getLifeTotal(other) shouldBe 20
                }
                d.state.getEntity(walkerA)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 5
                d.state.getEntity(walkerB)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 5
            }

            test("an X spell uses the chosen X for its mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vial Smasher the Fierce")
                    .withCardInHand(1, "Mind Spring") // {X}{U}{U}
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Mind Spring", xValue = 3).error shouldBe null
                game.resolveStack()

                withClue("Mind Spring with X=3 has mana value 5") {
                    game.getLifeTotal(2) shouldBe 15
                }
            }
        }
    }
}
