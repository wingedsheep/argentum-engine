package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Mnemonic Betrayal (GRN #189).
 *
 * Focus: an opponent's green creature card is cast with Swamps (mana of any type), an exiled
 * land can't be played ("cast spells"), the uncast cards return to their owner's graveyard at the
 * next end step, and Mnemonic Betrayal itself is exiled.
 */
class MnemonicBetrayalScenarioTest : ScenarioTestBase() {

    init {
        context("Mnemonic Betrayal") {

            fun board() = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Mnemonic Betrayal")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInGraveyard(2, "Grizzly Bears")
                .withCardInGraveyard(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun ScenarioTestBase.TestGame.opponentExiled(name: String): EntityId =
                state.getExile(player2Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

            test("casts an opponent's card with any mana; the rest return at the end step") {
                val game = board()

                val cast = game.castSpell(1, "Mnemonic Betrayal")
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the opponent's graveyard is exiled, and so is Mnemonic Betrayal") {
                    game.graveyardSize(2) shouldBe 0
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Forest") shouldBe true
                    game.isInExile(1, "Mnemonic Betrayal") shouldBe true
                    game.isInGraveyard(1, "Mnemonic Betrayal") shouldBe false
                }

                val playForest = game.execute(PlayLand(game.player1Id, game.opponentExiled("Forest")))
                withClue("an exiled land can't be played") { playForest.error shouldNotBe null }

                val castBears = game.execute(CastSpell(game.player1Id, game.opponentExiled("Grizzly Bears")))
                withClue("Grizzly Bears is castable with Swamps: ${castBears.error}") { castBears.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears resolved under my control") {
                    bears shouldNotBe null
                    game.state.getEntity(bears!!)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the Forest went back to its owner's graveyard; the cast Bears did not") {
                    game.isInExile(2, "Forest") shouldBe false
                    game.isInGraveyard(2, "Forest") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
            }
        }
    }
}
