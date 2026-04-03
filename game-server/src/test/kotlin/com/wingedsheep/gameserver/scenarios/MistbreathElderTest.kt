package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MistbreathElderTest : ScenarioTestBase() {

    init {
        context("Mistbreath Elder — upkeep trigger with another creature") {
            test("bounces another creature and gets +1/+1 counter") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mistbreath Elder", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hired Claw", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                // Advance to upkeep — trigger fires
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Trigger should be on the stack — Hired Claw is only other creature, auto-selects
                game.resolveStack()

                // Hired Claw should be back in hand
                withClue("Hired Claw should not be on battlefield") {
                    game.isOnBattlefield("Hired Claw") shouldBe false
                }

                // Mistbreath Elder should have a +1/+1 counter
                val elderId = game.findPermanent("Mistbreath Elder")!!
                val counters = game.state.getEntity(elderId)?.get<CountersComponent>()
                withClue("Mistbreath Elder should have 1 +1/+1 counter") {
                    counters shouldNotBe null
                    counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                }

                val projected = game.state.projectedState
                withClue("Mistbreath Elder should be 3/3") {
                    projected.getPower(elderId) shouldBe 3
                    projected.getToughness(elderId) shouldBe 3
                }
            }
        }

        context("Mistbreath Elder — upkeep trigger without another creature") {
            test("offers to bounce self when no other creature, accepts") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mistbreath Elder", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                // Trigger resolves — no other creature, MayEffect fires
                game.resolveStack()

                // Answer yes to bounce self
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Mistbreath Elder should not be on battlefield") {
                    game.isOnBattlefield("Mistbreath Elder") shouldBe false
                }
            }

            test("can decline to bounce self") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mistbreath Elder", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Answer no — keep self on battlefield
                game.answerYesNo(false)
                game.resolveStack()

                withClue("Mistbreath Elder should remain on battlefield") {
                    game.isOnBattlefield("Mistbreath Elder") shouldBe true
                }
            }
        }
    }
}
