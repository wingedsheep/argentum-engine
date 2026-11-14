package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ori.cards.ShadowsOfThePast
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Shadows of the Past (ORI #118) — {1}{B} Enchantment.
 *
 *   Whenever a creature dies, scry 1. (Look at the top card of your library. You may put that card
 *   on the bottom.)
 *   {4}{B}: Each opponent loses 2 life and you gain 2 life. Activate only if there are four or
 *   more creature cards in your graveyard.
 */
class ShadowsOfThePastScenarioTest : ScenarioTestBase() {

    private val drainAbilityId = ShadowsOfThePast.activatedAbilities.single().id

    private fun drainBoard(creatureCardsInGraveyard: Int): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Shadows of the Past")
        .withLandsOnBattlefield(1, "Swamp", 5)
        .apply { repeat(creatureCardsInGraveyard) { withCardInGraveyard(1, "Grizzly Bears") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activateDrain(): ExecutionResult =
        execute(ActivateAbility(player1Id, findPermanent("Shadows of the Past")!!, drainAbilityId))

    init {
        context("Shadows of the Past") {

            test("an opponent's creature dying lets you scry 1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shadows of the Past")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val top = game.state.getLibrary(game.player1Id).first()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true

                val scry = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Shadows' controller scries the top card of their library") {
                    scry.playerId shouldBe game.player1Id
                    scry.options shouldContainExactly listOf(top)
                }
                game.selectCards(listOf(top)).error shouldBe null
                game.resolveStack()

                withClue("the scried card went to the bottom") {
                    game.state.getLibrary(game.player1Id).last() shouldBe top
                }
            }

            test("the drain can't be activated with only three creature cards in your graveyard") {
                val game = drainBoard(creatureCardsInGraveyard = 3)

                game.activateDrain().error shouldNotBe null
                game.getLifeTotal(1) shouldBe 20
                game.getLifeTotal(2) shouldBe 20
            }

            test("with four creature cards in your graveyard, each opponent loses 2 and you gain 2") {
                val game = drainBoard(creatureCardsInGraveyard = 4)

                val result = game.activateDrain()
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 22
            }
        }
    }
}
