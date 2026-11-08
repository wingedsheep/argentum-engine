package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Collective Effort (EMN #17) — {1}{W}{W} sorcery, "Escalate—Tap an untapped
 * creature you control", three modes.
 *
 * The first card whose escalate cost is a tap: one untapped creature per mode beyond the first,
 * payable by a summoning-sick creature (2016-07-13 ruling), and the offered mode count is capped
 * by the untapped creatures available.
 */
class CollectiveEffortScenarioTest : ScenarioTestBase() {

    init {
        context("Collective Effort") {

            fun ScenarioTestBase.TestGame.castEffort(
                modes: List<Int>,
                modeTargets: List<List<ChosenTarget>>,
                tapped: List<EntityId> = emptyList(),
            ) = execute(
                CastSpell(
                    playerId = player1Id,
                    cardId = state.getHand(player1Id).first {
                        state.getEntity(it)?.get<CardComponent>()?.name == "Collective Effort"
                    },
                    targets = modeTargets.flatten(),
                    chosenModes = modes,
                    modeTargetsOrdered = modeTargets,
                    additionalCostPayment = AdditionalCostPayment(tappedPermanents = tapped),
                )
            )

            fun ScenarioTestBase.TestGame.isTapped(id: EntityId) =
                state.getEntity(id)?.get<TappedComponent>() != null

            fun ScenarioTestBase.TestGame.plusOneCounters(id: EntityId) =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            test("three modes tap two creatures, a summoning-sick one included, and every mode resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Effort")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true)
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!

                val cast = game.castEffort(
                    modes = listOf(0, 1, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(game.findPermanent("Craw Wurm")!!)),
                        listOf(ChosenTarget.Permanent(game.findPermanent("Glorious Anthem")!!)),
                        listOf(ChosenTarget.Player(game.player1Id)),
                    ),
                    tapped = listOf(bears, lions),
                )
                withClue("three modes owe two tapped creatures: ${cast.error}") { cast.error shouldBe null }
                withClue("the escalate cost is paid on cast") {
                    game.isTapped(bears) shouldBe true
                    game.isTapped(lions) shouldBe true
                }

                game.resolveStack()
                game.findPermanent("Craw Wurm") shouldBe null
                game.findPermanent("Glorious Anthem") shouldBe null
                withClue("the creatures tapped for escalate still get their counters") {
                    game.plusOneCounters(bears) shouldBe 1
                    game.plusOneCounters(lions) shouldBe 1
                }
            }

            test("a second mode without a tapped creature is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Effort")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castEffort(
                    modes = listOf(0, 2),
                    modeTargets = listOf(
                        listOf(ChosenTarget.Permanent(game.findPermanent("Craw Wurm")!!)),
                        listOf(ChosenTarget.Player(game.player1Id)),
                    ),
                )
                withClue("the escalate cost is mandatory once a second mode is chosen") {
                    cast.error shouldNotBe null
                }
            }

            test("the offered mode count is capped by the untapped creatures available") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Collective Effort")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions", tapped = true)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val modal = game.getLegalActions(1)
                    .first { it.description.contains("Collective Effort") }
                    .modalEnumeration
                withClue("one untapped creature pays for exactly one extra mode") {
                    modal?.chooseCount shouldBe 2
                }
                withClue("the client is told what each extra mode costs") {
                    modal?.additionalCostPerExtraMode?.costType shouldBe "TapPermanents"
                }
            }
        }
    }
}
