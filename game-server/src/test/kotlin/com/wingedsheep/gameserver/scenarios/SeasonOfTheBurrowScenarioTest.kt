package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.BudgetModalDecision
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Season of the Burrow (BLB #29).
 *
 * Season of the Burrow: {3}{W}{W} Sorcery
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Create a 1/1 white Rabbit creature token.
 * {P}{P} — Exile target nonland permanent. Its controller draws a card.
 * {P}{P}{P} — Return target permanent card with mana value 3 or less from your graveyard
 *             to the battlefield with an indestructible counter on it.
 */
class SeasonOfTheBurrowScenarioTest : ScenarioTestBase() {

    private fun submitBudgetModes(game: TestGame, vararg modeIndices: Int) {
        val decision = game.getPendingDecision() as BudgetModalDecision
        game.submitDecision(BudgetModalResponse(decision.id, modeIndices.toList()))
    }

    private fun selectPipelineTarget(game: TestGame, targetName: String) {
        val decision = game.getPendingDecision() as ChooseTargetsDecision
        val targetId = decision.legalTargets[0]!!.first { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.name == targetName
        }
        game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(targetId))))
    }

    init {
        context("Season of the Burrow budget modal") {

            test("mode 0 - create a 1/1 white Rabbit token") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Select token mode once (mode 0, cost 1)
                submitBudgetModes(game, 0)

                game.isOnBattlefield("Rabbit Token") shouldBe true
            }

            test("mode 0 - create multiple Rabbit tokens") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Select token mode 3 times (3 pawprints total)
                submitBudgetModes(game, 0, 0, 0)

                game.findAllPermanents("Rabbit Token").size shouldBe 3
            }

            test("mode 1 - exile target nonland permanent, its controller draws a card") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2HandBefore = game.handSize(2)

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Select exile mode (mode 1, cost 2)
                submitBudgetModes(game, 1)

                // Single nonland permanent → auto-selected
                game.isOnBattlefield("Hill Giant") shouldBe false
                // Opponent's creature was exiled, so opponent draws a card
                game.handSize(2) shouldBe p2HandBefore + 1
            }

            test("mode 1 - exile own permanent, you draw a card") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p1HandBefore = game.handSize(1)

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Select exile mode (mode 1, cost 2)
                submitBudgetModes(game, 1)

                // Only one nonland permanent (ours) → auto-selected
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                // We exiled our own creature, so WE draw (hand went -1 from casting, +1 from draw = net 0)
                game.handSize(1) shouldBe p1HandBefore
            }

            test("mode 2 - return permanent card from graveyard with indestructible counter") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Select return mode (mode 2, cost 3)
                submitBudgetModes(game, 2)

                // Single permanent card in graveyard with MV ≤ 3 → auto-selected
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe false

                // Check indestructible counter
                val bearsId = game.findPermanent("Grizzly Bears")!!
                val counters = game.state.getEntity(bearsId)?.get<CountersComponent>()
                counters shouldNotBe null
                counters!!.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1

                // Verify the keyword is granted via projected state
                game.state.projectedState.hasKeyword(bearsId, Keyword.INDESTRUCTIBLE) shouldBe true
            }

            test("mode 1 - choosing target when multiple nonland permanents") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2HandBefore = game.handSize(2)

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                submitBudgetModes(game, 1)

                // Multiple targets → must choose
                selectPipelineTarget(game, "Hill Giant")

                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.handSize(2) shouldBe p2HandBefore + 1
            }

            test("choosing no modes does nothing") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                submitBudgetModes(game)

                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("combined modes - token + exile") {
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Season of the Burrow")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p2HandBefore = game.handSize(2)

                game.castSpell(1, "Season of the Burrow")
                game.resolveStack()

                // Token (1) + exile (2) = 3 pawprints, executed in printed order
                submitBudgetModes(game, 0, 1)

                // Token creates a Rabbit, then exile needs to choose target
                // (Rabbit Token + Hill Giant are both nonland permanents)
                selectPipelineTarget(game, "Hill Giant")

                game.isOnBattlefield("Rabbit Token") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.handSize(2) shouldBe p2HandBefore + 1
            }
        }
    }
}
