package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shiko, Paragon of the Way (TDM) — {2}{U}{R}{W} Legendary Spirit Dragon.
 *
 * "When Shiko enters, exile target nonland card with mana value 3 or less from your graveyard.
 * Copy it, then you may cast the copy without paying its mana cost."
 *
 * Exercises the reusable "copy a card in a zone, then cast the copy" chain
 * ([com.wingedsheep.sdk.scripting.effects.CopyCardIntoCollectionEffect] +
 * `CastFromCollectionWithoutPayingCostEffect`) and the Rule 707.10a phantom-copy state-based
 * action:
 *  - an instant copy is cast for free and the original stays exiled (it does not return to the
 *    graveyard);
 *  - a permanent copy becomes a token on resolution (Rule 707.10);
 *  - declining the cast leaves no phantom copy lingering in exile.
 */
class ShikoParagonOfTheWayScenarioTest : ScenarioTestBase() {

    init {
        context("Shiko, Paragon of the Way ETB") {

            test("copies an instant, casts the copy for free, and leaves the original exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shiko, Paragon of the Way")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInGraveyard(1, "Shock")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shockId = graveyardCardId(game, "Shock")

                game.castSpell(1, "Shiko, Paragon of the Way")
                game.resolveStack()
                // Shiko's ETB pauses to choose its target — the Shock in the graveyard.
                game.selectTargets(listOf(shockId))
                // Trigger resolves: exile Shock, copy it, prompt "you may cast the copy".
                game.resolveStack()
                game.answerYesNo(true)
                // The Shock copy is "deals 2 damage to any target" — pick the opponent.
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("Shock copy should deal 2 damage to the opponent") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("The original Shock is exiled, not returned to the graveyard") {
                    game.isInGraveyard(1, "Shock") shouldBe false
                }
                withClue("Exile holds exactly one Shock — the original, no phantom copy") {
                    cardsInExile(game, "Shock") shouldBe 1
                }
            }

            test("copies a creature card; the cast copy becomes a token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shiko, Paragon of the Way")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = graveyardCardId(game, "Grizzly Bears")

                game.castSpell(1, "Shiko, Paragon of the Way")
                game.resolveStack()
                game.selectTargets(listOf(bearsId))
                game.resolveStack()
                game.answerYesNo(true)
                // The Grizzly Bears copy is a creature spell (no targets) — resolve it.
                game.resolveStack()

                val tokenId = game.findPermanent("Grizzly Bears")
                withClue("A Grizzly Bears should be on the battlefield (the cast copy)") {
                    (tokenId != null) shouldBe true
                }
                withClue("Rule 707.10 — a copy of a permanent spell becomes a token") {
                    game.state.getEntity(tokenId!!)!!.has<TokenComponent>() shouldBe true
                }
                withClue("The original Grizzly Bears card stays exiled (not a token)") {
                    nonTokenCardsInExile(game, "Grizzly Bears") shouldBe 1
                }
            }

            test("declining the cast leaves no phantom copy in exile (Rule 707.10a)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shiko, Paragon of the Way")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = graveyardCardId(game, "Grizzly Bears")

                game.castSpell(1, "Shiko, Paragon of the Way")
                game.resolveStack()
                game.selectTargets(listOf(bearsId))
                game.resolveStack()
                game.answerYesNo(false)

                withClue("No Grizzly Bears should hit the battlefield when the cast is declined") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
                withClue("The original is exiled and the uncast copy ceased to exist (count == 1)") {
                    cardsInExile(game, "Grizzly Bears") shouldBe 1
                }
            }
        }
    }

    private fun graveyardCardId(game: TestGame, name: String) =
        game.state.getGraveyard(game.player1Id).first { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    private fun cardsInExile(game: TestGame, name: String): Int =
        game.state.getExile(game.player1Id).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    private fun nonTokenCardsInExile(game: TestGame, name: String): Int =
        game.state.getExile(game.player1Id).count { id ->
            val container = game.state.getEntity(id)
            container?.get<CardComponent>()?.name == name &&
                !container.has<TokenComponent>() &&
                !container.has<CopyOfComponent>()
        }
}
