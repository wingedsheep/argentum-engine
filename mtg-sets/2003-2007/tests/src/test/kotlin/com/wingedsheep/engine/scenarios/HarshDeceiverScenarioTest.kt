package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.HarshDeceiver
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Harsh Deceiver (CHK #11) — "{1}: Look at the top card of your library. / {2}: Reveal the top card
 * of your library. If it's a land card, untap this creature and it gets +1/+1 until end of turn.
 * Activate only once each turn."
 */
class HarshDeceiverScenarioTest : ScenarioTestBase() {

    private val lookAbility = HarshDeceiver.activatedAbilities[0].id
    private val revealAbility = HarshDeceiver.activatedAbilities[1].id

    init {
        context("Harsh Deceiver") {

            test("revealing a land untaps it and gives +1/+1; the card stays on top") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Harsh Deceiver", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deceiver = game.findPermanent("Harsh Deceiver")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = deceiver, abilityId = revealAbility)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("Harsh Deceiver is untapped") {
                    game.state.getEntity(deceiver)!!.has<TappedComponent>() shouldBe false
                }
                game.state.projectedState.getPower(deceiver) shouldBe 2
                game.state.projectedState.getToughness(deceiver) shouldBe 5
                withClue("revealed card stays in the library") {
                    game.state.getLibrary(game.player1Id).size shouldBe 1
                }

                withClue("the reveal ability can be activated only once each turn") {
                    val again = game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = deceiver, abilityId = revealAbility)
                    )
                    again.error shouldNotBe null
                }
            }

            test("revealing a nonland does nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Harsh Deceiver", tapped = true)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deceiver = game.findPermanent("Harsh Deceiver")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = deceiver, abilityId = revealAbility)
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.state.getEntity(deceiver)!!.has<TappedComponent>() shouldBe true
                game.state.projectedState.getPower(deceiver) shouldBe 1
                game.state.projectedState.getToughness(deceiver) shouldBe 4
            }

            test("the look ability can be activated repeatedly and moves nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Harsh Deceiver")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val deceiver = game.findPermanent("Harsh Deceiver")!!
                repeat(2) {
                    val result = game.execute(
                        ActivateAbility(playerId = game.player1Id, sourceId = deceiver, abilityId = lookAbility)
                    )
                    withClue("look activation should succeed: ${result.error}") { result.error shouldBe null }
                    game.resolveStack()
                }
                game.state.getLibrary(game.player1Id).size shouldBe 1
                game.state.projectedState.getPower(deceiver) shouldBe 1
            }
        }
    }
}
