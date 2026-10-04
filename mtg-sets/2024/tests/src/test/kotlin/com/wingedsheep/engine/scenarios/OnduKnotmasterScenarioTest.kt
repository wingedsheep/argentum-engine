package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ondu Knotmaster // Throw a Line (MH3 #196).
 *
 *   Lifelink
 *   Whenever another modified creature you control dies, put two +1/+1 counters on this creature.
 *   Adventure — Throw a Line {W}{B}: Distribute two +1/+1 counters among one or two target creatures.
 */
class OnduKnotmasterScenarioTest : ScenarioTestBase() {

    init {
        context("Ondu Knotmaster") {

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            fun board(creature: String? = "Grizzly Bears", knotmaster: Boolean = true) = scenario()
                .withPlayers("Player", "Opponent")
                .apply { if (knotmaster) withCardOnBattlefield(1, "Ondu Knotmaster") }
                .apply { creature?.let { withCardOnBattlefield(1, it) } }
                .withCardInHand(1, "Doom Blade")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            test("has lifelink") {
                val game = board()
                game.state.projectedState.hasKeyword(game.findPermanent("Ondu Knotmaster")!!, Keyword.LIFELINK) shouldBe true
            }

            test("another modified creature dying puts two +1/+1 counters on it") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.castSpell(1, "Doom Blade", bears).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.plusOnes(game.findPermanent("Ondu Knotmaster")!!) shouldBe 2
            }

            test("an unmodified creature dying does nothing") {
                val game = board()
                game.castSpell(1, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.plusOnes(game.findPermanent("Ondu Knotmaster")!!) shouldBe 0
            }

            test("a modified creature an opponent controls dying does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ondu Knotmaster")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInHand(1, "Doom Blade")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val courser = game.findPermanent("Centaur Courser")!!
                game.state = game.state.updateEntity(courser) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                game.castSpell(1, "Doom Blade", courser).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Centaur Courser") shouldBe true
                game.plusOnes(game.findPermanent("Ondu Knotmaster")!!) shouldBe 0
            }

            test("Throw a Line splits the counters between two targets as chosen, then exiles the card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ondu Knotmaster")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val card = game.findCardsInHand(1, "Ondu Knotmaster").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = card,
                        targets = listOf(bears, courser).map { ChosenTarget.Permanent(it) },
                        faceIndex = 0,
                    )
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                decision.totalAmount shouldBe 2
                game.submitDecision(DistributionResponse(decision.id, mapOf(bears to 1, courser to 1))).error shouldBe null
                game.resolveStack()

                game.plusOnes(bears) shouldBe 1
                game.plusOnes(courser) shouldBe 1
                game.isInExile(1, "Ondu Knotmaster") shouldBe true
            }

            test("Throw a Line puts both counters on a single target") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Ondu Knotmaster")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val card = game.findCardsInHand(1, "Ondu Knotmaster").first()

                game.execute(
                    CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears)), faceIndex = 0)
                ).error shouldBe null
                game.resolveStack()

                game.plusOnes(bears) shouldBe 2
                game.isInExile(1, "Ondu Knotmaster") shouldBe true
            }
        }
    }
}
