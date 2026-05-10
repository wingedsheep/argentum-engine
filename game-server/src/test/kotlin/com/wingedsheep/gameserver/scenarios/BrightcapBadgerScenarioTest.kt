package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Brightcap Badger // Fungus Frolic — also exercising the new Adventure
 * mechanic (CR 715) generally.
 *
 * Brightcap Badger {3}{G}, Creature — Badger Druid, 3/4
 *   Each Fungus and Saproling you control has "{T}: Add {G}."
 *   At the beginning of your end step, create a 1/1 green Saproling creature token.
 * Adventure: Fungus Frolic {2}{G}, Instant — Adventure
 *   Create two 1/1 green Saproling creature tokens.
 */
class BrightcapBadgerScenarioTest : ScenarioTestBase() {

    private fun saprolingsControlledBy(game: TestGame, playerNumber: Int): Int {
        val pid = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getBattlefield(pid).count { entityId ->
            game.state.getEntity(entityId)?.get<CardComponent>()?.let { card ->
                Subtype.SAPROLING in card.typeLine.subtypes
            } == true
        }
    }

    /** Advance from main phase to end step by passing priority until we get there. */
    private fun advanceToEndStep(game: TestGame) {
        var iterations = 0
        while (game.state.step != Step.END && game.state.pendingDecision == null && iterations++ < 12) {
            game.passPriority()
        }
    }

    init {
        context("Adventure mechanic — Fungus Frolic") {

            test("casting the Adventure creates two Saprolings, exiles the card, and grants a cast-from-exile permission") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Brightcap Badger")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Brightcap Badger").first()

                // Cast the Adventure half (face index 0).
                val castResult = game.execute(CastSpell(game.player1Id, cardId, faceIndex = 0))
                withClue("Casting Fungus Frolic should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                withClue("Spell should be on the stack after cast (stack size=${game.state.stack.size})") {
                    game.state.stack.size shouldBe 1
                }

                // Spell on the stack now; resolve it.
                game.resolveStack()
                withClue("Stack should be empty after resolution (stack=${game.state.stack.map { game.state.getEntity(it)?.get<CardComponent>()?.name }})") {
                    game.state.stack.size shouldBe 0
                }

                withClue("Two Saproling tokens should be on the battlefield") {
                    saprolingsControlledBy(game, 1) shouldBe 2
                }
                withClue("Brightcap Badger should be in exile, not in graveyard or hand") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe true
                    game.state.getGraveyard(game.player1Id).contains(cardId) shouldBe false
                    game.state.getHand(game.player1Id).contains(cardId) shouldBe false
                }
                withClue("Caster should hold the cast-from-exile permission (CR 715.5)") {
                    val perm = game.state.getEntity(cardId)?.get<MayPlayFromExileComponent>()
                    perm.shouldNotBeNull()
                    perm.controllerId shouldBe game.player1Id
                    perm.permanent shouldBe true
                }
            }

            test("after the Adventure resolves, the creature can be cast from exile") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Brightcap Badger")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.findCardsInHand(1, "Brightcap Badger").first()

                // Cast the Adventure, resolve.
                game.execute(CastSpell(game.player1Id, cardId, faceIndex = 0))
                game.resolveStack()

                game.state.getExile(game.player1Id).contains(cardId) shouldBe true

                // Now cast the creature face from exile.
                val recast = game.execute(CastSpell(game.player1Id, cardId))
                withClue("Re-cast as creature from exile should succeed: ${recast.error}") {
                    recast.error shouldBe null
                }
                game.resolveStack()

                withClue("Brightcap Badger should now be a creature on the battlefield") {
                    game.findPermanent("Brightcap Badger") shouldNotBe null
                }
                withClue("Card should have left exile after entering the battlefield") {
                    game.state.getExile(game.player1Id).contains(cardId) shouldBe false
                }
            }
        }

        context("Brightcap Badger creature face") {

            test("casting Brightcap Badger as a creature from hand puts a Badger Druid on the battlefield") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Brightcap Badger")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Brightcap Badger")
                game.resolveStack()

                val brightcap = game.findPermanent("Brightcap Badger")
                withClue("Brightcap Badger should be on the battlefield") {
                    brightcap shouldNotBe null
                }
                val card = game.state.getEntity(brightcap!!)?.get<CardComponent>()!!
                card.typeLine.isCreature shouldBe true
            }

            test("Brightcap Badger creates a Saproling token at your end step") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Brightcap Badger")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                advanceToEndStep(game)
                // Trigger fires at the start of the end step.
                withClue("End-step Saproling trigger should be on the stack") {
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()

                withClue("One Saproling token should now be on the battlefield") {
                    saprolingsControlledBy(game, 1) shouldBe 1
                }
            }
        }
    }
}
