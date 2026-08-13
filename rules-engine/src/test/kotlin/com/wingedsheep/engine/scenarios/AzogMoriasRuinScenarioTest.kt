package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Azog, Moria's Ruin — The Hobbit #61.
 *
 * Pins the linked sequence: optional other target, frozen last-known power, destruction, amass by
 * the creature's controller, and the controller-sensitive draw rider.
 */
class AzogMoriasRuinScenarioTest : ScenarioTestBase() {

    private fun TestGame.goblinArmyControlledBy(playerId: EntityId): EntityId? =
        state.getBattlefield().firstOrNull { id ->
            state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name ==
                "Goblin Army" && state.projectedState.getController(id) == playerId
        }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.castAzog(target: EntityId?) {
        castSpell(1, "Azog, Moria's Ruin").error shouldBe null
        resolveStack()
        if (hasPendingDecision()) {
            selectTargets(listOfNotNull(target)).error shouldBe null
        }
        resolveStack()
    }

    init {
        cardRegistry.register(card("Azog Test Army Alpha") {
            manaCost = "{0}"
            typeLine = "Creature — Zombie Army"
            power = 2
            toughness = 2
        })
        cardRegistry.register(card("Azog Test Army Beta") {
            manaCost = "{0}"
            typeLine = "Creature — Zombie Army"
            power = 2
            toughness = 2
        })

        context("Azog's enters ability") {

            test("uses the opponent creature's projected last-known power and that opponent amasses") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Azog, Moria's Ruin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent("Hill Giant")!!

                game.castAzog(target)

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                val army = game.goblinArmyControlledBy(game.player2Id)
                withClue("Hill Giant was 4/4 under Glorious Anthem before it left") {
                    army shouldBe (army ?: error("opponent did not create a Goblin Army"))
                    game.plusOneCounters(army) shouldBe 4
                }
                game.goblinArmyControlledBy(game.player1Id) shouldBe null
                withClue("Azog's controller did not control the target, so no card is drawn") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("its controller draws after destroying and amassing from their own creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Azog, Moria's Ruin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent("Grizzly Bears")!!

                game.castAzog(target)

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                val army = game.goblinArmyControlledBy(game.player1Id)
                army shouldBe (army ?: error("Azog's controller did not create a Goblin Army"))
                game.plusOneCounters(army) shouldBe 2
                game.handSize(1) shouldBe 1
            }

            test("uses projected control and preserves that controller through destruction") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Threaten")
                    .withCardInHand(1, "Azog, Moria's Ruin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Threaten", target).error shouldBe null
                game.resolveStack()
                withClue("Threaten changes projected control without rewriting base ownership/control") {
                    game.state.projectedState.getController(target) shouldBe game.player1Id
                    game.state.getEntity(target)?.get<ControllerComponent>()?.playerId shouldBe game.player2Id
                }

                game.castAzog(target)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                val army = game.goblinArmyControlledBy(game.player1Id)
                army shouldBe (army ?: error("the stolen creature's last-known controller did not amass"))
                game.plusOneCounters(army) shouldBe 2
                game.goblinArmyControlledBy(game.player2Id) shouldBe null
                game.handSize(1) shouldBe 1
                game.handSize(2) shouldBe 0
            }

            test("choosing no target destroys nothing, amasses nothing, and draws nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Azog, Moria's Ruin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAzog(target = null)

                game.goblinArmyControlledBy(game.player1Id) shouldBe null
                game.goblinArmyControlledBy(game.player2Id) shouldBe null
                game.handSize(1) shouldBe 0
            }

            test("the affected controller chooses which of their Armies receives the counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Azog, Moria's Ruin")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Azog Test Army Alpha")
                    .withCardOnBattlefield(2, "Azog Test Army Beta")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val target = game.findPermanent("Hill Giant")!!
                val alpha = game.findPermanent("Azog Test Army Alpha")!!
                val beta = game.findPermanent("Azog Test Army Beta")!!

                game.castSpell(1, "Azog, Moria's Ruin").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(target)).error shouldBe null
                game.resolveStack()

                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                choice.playerId shouldBe game.player2Id
                choice.options.toSet() shouldBe setOf(alpha, beta)
                game.selectCards(listOf(beta)).error shouldBe null
                game.resolveStack()

                game.plusOneCounters(alpha) shouldBe 0
                game.plusOneCounters(beta) shouldBe 3
            }
        }
    }
}
