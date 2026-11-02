package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nog, Ambitious Cadet (TRK #249) — {B}{G} 2/2 Legendary Ferengi Rogue with menace.
 *
 * "{1}: If Nog is a Rogue, he becomes a Ferengi Scout with base power and toughness 3/4 and gains
 * 'Whenever Nog becomes tapped, surveil 2.'
 * {3}: If Nog is a Scout, he becomes a Ferengi Officer and gains 'You may play lands from your
 * graveyard' and 'You may play an additional land on each of your turns.'"
 *
 * The engine rules for gained land-play statics live in `GainedLandPlayStaticsTest`; this pins the
 * card's own upgrade chain.
 */
class NogAmbitiousCadetScenarioTest : ScenarioTestBase() {

    private val nogName = "Nog, Ambitious Cadet"

    private fun TestGame.activateNog(index: Int) {
        val nog = findPermanent(nogName)!!
        val abilityId = cardRegistry.getCard(nogName)!!.activatedAbilities[index].id
        execute(ActivateAbility(playerId = player1Id, sourceId = nog, abilityId = abilityId)).error shouldBe null
        resolveStack()
    }

    private fun newGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, nogName)
        .withLandsOnBattlefield(1, "Swamp", 4)
        .withCardInHand(1, "Forest")
        .withCardInGraveyard(1, "Forest")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Nog, Ambitious Cadet") {

            test("the Rogue becomes a 3/4 Ferengi Scout that surveils 2 when it becomes tapped") {
                val game = newGame()
                game.activateNog(0)

                val nog = game.findPermanent(nogName)!!
                withClue("types are overwritten, base P/T set") {
                    game.state.projectedState.getSubtypes(nog) shouldBe setOf("Ferengi", "Scout")
                    game.state.projectedState.getPower(nog) shouldBe 3
                    game.state.projectedState.getToughness(nog) shouldBe 4
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf(nogName to 2)).error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the gained trigger surveils the top two cards") { decision.options.size shouldBe 2 }
                game.selectCards(decision.options)
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.graveyardSize(1) shouldBe 3
            }

            test("the Officer ability does nothing while Nog is still a Rogue") {
                val game = newGame()
                game.activateNog(1)

                val nog = game.findPermanent(nogName)!!
                game.state.projectedState.getSubtypes(nog) shouldBe setOf("Ferengi", "Rogue")
                val graveyardForest = game.findCardsInGraveyard(1, "Forest").single()
                game.execute(PlayLand(game.player1Id, graveyardForest)).error shouldNotBe null
            }

            test("the Officer may play an extra land, from the graveyard") {
                val game = newGame()
                game.activateNog(0)
                game.activateNog(1)

                val nog = game.findPermanent(nogName)!!
                game.state.projectedState.getSubtypes(nog) shouldBe setOf("Ferengi", "Officer")
                withClue("the Scout's base 3/4 stays") { game.state.projectedState.getPower(nog) shouldBe 3 }

                val handForest = game.findCardsInHand(1, "Forest").single()
                game.execute(PlayLand(game.player1Id, handForest)).error shouldBe null
                val graveyardForest = game.findCardsInGraveyard(1, "Forest").single()
                withClue("the second land play comes from the graveyard") {
                    game.execute(PlayLand(game.player1Id, graveyardForest)).error shouldBe null
                    game.findPermanents("Forest").size shouldBe 2
                }
            }
        }
    }
}
