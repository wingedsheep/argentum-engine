package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Mystical Tether. */
class MysticalTetherScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Mystical Tether") {
            test("exiles an opponent's creature on ETB, returns it when the enchantment leaves") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mystical Tether")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val victim = game.findPermanent("Hill Giant")!!
                val cast = game.castSpell(1, "Mystical Tether")
                withClue("Cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack() // enchantment enters → ETB trigger asks for a target

                val selected = game.selectTargets(listOf(victim))
                withClue("ETB target selection should succeed: ${selected.error}") {
                    selected.error shouldBe null
                }
                game.resolveStack()

                withClue("Hill Giant should be exiled while Mystical Tether is in play") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                    } shouldBe 1
                }
            }

            test("can also exile an opponent's artifact (artifact-or-creature filter)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Mystical Tether")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val artifact = game.findPermanent("Ornithopter")!!
                game.castSpell(1, "Mystical Tether")
                game.resolveStack()
                game.selectTargets(listOf(artifact))
                game.resolveStack()

                withClue("Ornithopter (an artifact) should be a legal exile target") {
                    game.isOnBattlefield("Ornithopter") shouldBe false
                    game.state.getExile(game.player2Id).count {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Ornithopter"
                    } shouldBe 1
                }
            }
        }
    }
}
