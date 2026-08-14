package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Black Cat, Cunning Thief (SPM #52) — {3}{B}{B} Legendary Creature 2/3.
 *
 *   When Black Cat enters, look at the top nine cards of target opponent's library, exile two of
 *   them face down, then put the rest on the bottom of their library in a random order. You may play
 *   the exiled cards for as long as they remain exiled. Mana of any type can be spent to cast spells
 *   this way.
 *
 * Covers the ETB dig (look at the top nine of the opponent's library, exile exactly two, bottom the
 * remaining seven) and the impulse-with-any-mana payoff: the exiled off-color card carries a
 * permanent may-play permission whose {G} pip is paid entirely from the Black Cat controller's
 * Swamps — impossible without `withAnyManaType`.
 */
class BlackCatCunningThiefScenarioTest : ScenarioTestBase() {

    init {
        context("Black Cat, Cunning Thief") {

            test("exiles two of the opponent's top nine, bottoms the rest, and an exiled off-color card is castable with any mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Black Cat, Cunning Thief")
                    // {3}{B}{B} for Black Cat (5) + {1}{G} for the stolen Grizzly Bears (2) = 7 Swamps.
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    // Opponent's library = the nine looked-at cards: one off-color creature + eight lands.
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Black Cat, Cunning Thief")
                withClue("Casting Black Cat should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                // Resolve the creature; the ETB trigger auto-targets the sole opponent and pauses on
                // the "exile two of them" selection.
                game.resolveStack()

                withClue("Black Cat resolved onto the battlefield") {
                    game.isOnBattlefield("Black Cat, Cunning Thief") shouldBe true
                }

                val decision = game.getPendingDecision()
                    .shouldBeInstanceOf<SelectCardsDecision>()
                withClue("The dig looks at nine cards and forces exactly two to be exiled") {
                    decision.options.size shouldBe 9
                    decision.minSelections shouldBe 2
                    decision.maxSelections shouldBe 2
                }

                // Exile the off-color Grizzly Bears plus one Forest.
                val grizzly = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val aForest = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                }
                game.selectCards(listOf(grizzly, aForest))

                withClue("Exactly two cards were exiled (to the owner's exile), the other seven bottomed") {
                    namesInExile(game, 2).size shouldBe 2
                    game.state.getExile(game.player2Id).contains(grizzly).shouldBeTrue()
                    game.librarySize(2) shouldBe 7
                }

                withClue("The Black Cat controller holds a permanent, any-mana play permission for the exiled card") {
                    val permission = game.state.mayPlayPermissions.single { grizzly in it.cardIds }
                    permission.controllerId shouldBe game.player1Id
                    permission.permanent shouldBe true
                    permission.withAnyManaType shouldBe true
                }

                // Cast the exiled Grizzly Bears from the opponent's exile as Player1, paying its {G}
                // pip entirely from Swamps — only possible because mana of any type may be spent.
                val castExiled = game.execute(CastSpell(game.player1Id, grizzly))
                withClue("Casting the exiled Grizzly Bears from exile should succeed: ${castExiled.error}") {
                    castExiled.error shouldBe null
                }
                game.resolveStack()

                withClue("The exiled card resolved onto the Black Cat controller's battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.getExile(game.player2Id).contains(grizzly) shouldBe false
                }
            }

            // Regression: the two cards are exiled FACE DOWN, but the Black Cat controller "may play"
            // them. The client view must reveal them (with a play-from-exile affordance) to that
            // controller — otherwise the server offers a CastSpell the UI has no card data to act on,
            // and the player is "unable to play" the exiled cards. Everyone else still sees only a
            // face-down card. (Face-up impulse cards like Laughing Jasper Flint never hit this path.)
            test("a face-down exiled card is revealed and playable to the controller, but masked to others") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Black Cat, Cunning Thief")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Black Cat, Cunning Thief").error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val grizzly = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val aForest = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                }
                game.selectCards(listOf(grizzly, aForest))

                // The Black Cat controller (Player1) holds the may-play permission, so their view must
                // expose the real card and flag it playable from exile.
                val asController = stateTransformer
                    .transform(game.state, viewingPlayerId = game.player1Id)
                    .cards[grizzly].shouldNotBeNull()
                withClue("The permission-holder sees the real, playable card rather than a masked stub") {
                    asController.name shouldBe "Grizzly Bears"
                    asController.isFaceDown shouldBe false
                    asController.manaCost shouldNotBe ""
                    asController.playableFromExile shouldBe true
                }

                // A spectator (no play permission) still sees only a face-down card with no identity.
                val asSpectator = stateTransformer
                    .transform(game.state, viewingPlayerId = game.player2Id, isSpectator = true)
                    .cards[grizzly].shouldNotBeNull()
                withClue("A viewer without permission still sees a masked, unplayable face-down card") {
                    asSpectator.name shouldBe "Face-down card"
                    asSpectator.isFaceDown shouldBe true
                    asSpectator.playableFromExile shouldBe false
                }
            }

            // Regression: a LAND exiled face down must enter the battlefield face up as the real land,
            // not as a face-down 2/2. Lands bypass the stack (where face-down spells are revealed), so
            // PlayLandHandler must strip the face-down marker itself (CR 305.1 — a land is played face up).
            test("a land exiled face down is played face up as the real land, not a face-down creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Black Cat, Cunning Thief")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Black Cat, Cunning Thief").error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val grizzly = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
                }
                val aForest = decision.options.first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
                }
                game.selectCards(listOf(grizzly, aForest))

                // Play the exiled Forest as a land (its may-play permission covers it).
                val played = game.execute(PlayLand(game.player1Id, aForest))
                withClue("Playing the exiled Forest as a land should succeed: ${played.error}") {
                    played.error shouldBe null
                }

                withClue("The Forest entered face up as a real land, not a face-down 2/2 creature") {
                    game.isOnBattlefield("Forest") shouldBe true
                    // The face-down marker is stripped on the way in.
                    game.state.getEntity(aForest)?.get<FaceDownComponent>() shouldBe null
                }

                // The client view confirms it renders as a Forest with no power/toughness — a
                // face-down permanent would surface as a nameless 2/2.
                val playedForest = stateTransformer
                    .transform(game.state, viewingPlayerId = game.player1Id)
                    .cards[aForest].shouldNotBeNull()
                withClue("The played land shows as a Forest, not a face-down creature") {
                    playedForest.name shouldBe "Forest"
                    playedForest.isFaceDown shouldBe false
                    playedForest.power shouldBe null
                    playedForest.toughness shouldBe null
                }
            }
        }
    }

    private fun namesInExile(game: TestGame, playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name
        }
    }
}
