package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Dose of Dawnglow.
 *
 * Card reference:
 * - Dose of Dawnglow ({4}{B}): Instant
 *   "Return target creature card from your graveyard to the battlefield.
 *    Then if it isn't your main phase, blight 2. (Put two -1/-1 counters on a creature you control.)"
 *
 * The interesting behavior is the main-phase gate: the blight rider only triggers when the spell
 * is cast (and resolves) outside the controller's main phase — i.e. on an opponent's turn or
 * during combat/upkeep/end step. This exercises the new `IsInPhase` / `IsYourMainPhase` condition.
 */
class DoseOfDawnglowScenarioTest : ScenarioTestBase() {

    init {
        context("Dose of Dawnglow - in your main phase") {

            test("reanimates creature without blight when cast during your precombat main phase") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dose of Dawnglow")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elvish Warrior") // potential blight victim
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val warrior = game.findPermanent("Elvish Warrior")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val castResult = game.castSpellTargetingGraveyardCard(1, "Dose of Dawnglow", listOf(bears))
                withClue("Dose of Dawnglow should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("No pending decision — blight should be skipped in own main phase") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Grizzly Bears should be on the battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                val warriorCounters = game.state.getEntity(warrior)?.get<CountersComponent>()
                withClue("Elvish Warrior should have no -1/-1 counters") {
                    (warriorCounters?.getCount(CounterType.MINUS_ONE_MINUS_ONE) ?: 0) shouldBe 0
                }
            }
        }

        context("Dose of Dawnglow - outside your main phase") {

            test("reanimates creature and forces blight 2 when cast on opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dose of Dawnglow")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3 — survives blight 2 as 0/1
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2) // opponent's turn
                    .withPriorityPlayer(1) // we need priority to cast an instant
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val warrior = game.findPermanent("Elvish Warrior")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                val castResult = game.castSpellTargetingGraveyardCard(1, "Dose of Dawnglow", listOf(bears))
                withClue("Dose of Dawnglow should be cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Blight should prompt for a creature to receive counters") {
                    game.hasPendingDecision() shouldBe true
                }

                val blightResult = game.selectCards(listOf(warrior))
                withClue("Selecting the blight target should succeed: ${blightResult.error}") {
                    blightResult.error shouldBe null
                }

                withClue("Grizzly Bears should have been reanimated") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val warriorCounters = game.state.getEntity(warrior)?.get<CountersComponent>()
                withClue("Elvish Warrior should have 2 -1/-1 counters from blight 2") {
                    warriorCounters.shouldNotBeNull()
                    warriorCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }
            }

            test("blights during opponent's combat phase on your own turn (not a main phase)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Dose of Dawnglow")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Elvish Warrior")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1) // own turn
                    .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT) // but not a main phase
                    .build()

                val warrior = game.findPermanent("Elvish Warrior")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").first()

                game.castSpellTargetingGraveyardCard(1, "Dose of Dawnglow", listOf(bears))
                game.resolveStack()

                withClue("Blight should prompt — combat is not a main phase") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(listOf(warrior))

                val warriorCounters = game.state.getEntity(warrior)?.get<CountersComponent>()
                withClue("Elvish Warrior should have 2 -1/-1 counters") {
                    warriorCounters.shouldNotBeNull()
                    warriorCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2
                }
            }
        }
    }
}
