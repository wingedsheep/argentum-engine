package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Steelbane Hydra (ELD #322) — {X}{G}{G} Creature — Turtle Hydra 0/0.
 *
 * "This creature enters with X +1/+1 counters on it. / {2}{G}, Remove a +1/+1 counter from this
 * creature: Destroy target artifact or enchantment."
 */
class SteelbaneHydraScenarioTest : ScenarioTestBase() {

    init {
        context("Steelbane Hydra") {

            test("enters with X counters, then spends one to destroy an artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steelbane Hydra")
                    .withCardOnBattlefield(2, "Arcane Signet")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Steelbane Hydra", xValue = 2).error shouldBe null
                game.resolveStack()

                val hydra = game.findPermanent("Steelbane Hydra")
                withClue("the Hydra survives as a 2/2 thanks to X = 2 counters") {
                    hydra shouldNotBe null
                    game.state.getEntity(hydra!!)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
                    game.state.projectedState.getPower(hydra) shouldBe 2
                }

                val signet = game.findPermanent("Arcane Signet")!!
                val abilityId = cardRegistry.getCard("Steelbane Hydra")!!.activatedAbilities.first().id
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hydra!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(signet))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the opponent's artifact is destroyed") {
                    game.isInGraveyard(2, "Arcane Signet") shouldBe true
                }
                withClue("one +1/+1 counter was removed as a cost") {
                    game.state.getEntity(hydra)!!.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getToughness(hydra) shouldBe 1
                }
            }

            test("cast with X = 0 it dies to state-based actions") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steelbane Hydra")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "Steelbane Hydra", xValue = 0).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Steelbane Hydra") shouldBe true
            }
        }
    }
}
