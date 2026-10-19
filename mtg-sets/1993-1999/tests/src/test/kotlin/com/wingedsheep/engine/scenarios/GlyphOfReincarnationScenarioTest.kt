package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Glyph of Reincarnation (LEG #189) — "Cast this spell only after combat. Destroy all creatures that
 * were blocked by target Wall this turn. They can't be regenerated. For each creature that died this
 * way, put a creature card from the graveyard of the player who controlled that creature the last
 * time it became blocked by that Wall onto the battlefield under its owner's control."
 *
 * Player 2 attacks with Grizzly Bears into player 1's Wall of Wood; the Bears survive the block.
 */
class GlyphOfReincarnationScenarioTest : ScenarioTestBase() {

    init {
        context("Glyph of Reincarnation") {

            fun setup(): TestGame {
                val builder = scenario()
                    .withPlayers("Wall Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Wood")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withCardInHand(1, "Glyph of Reincarnation")
                    .withCardInHand(1, "Caught Red-Handed")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Forest")
                    builder.withCardInLibrary(2, "Mountain")
                }
                return builder.build()
            }

            fun TestGame.attackAndBlock() {
                passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                declareBlockers(mapOf("Wall of Wood" to listOf("Grizzly Bears"))).error shouldBe null
            }

            fun TestGame.giveWallPlayerPriority() {
                if (state.priorityPlayerId == player2Id) passPriority()
            }

            test("can't be cast before combat or during combat") {
                val game = setup()
                game.giveWallPlayerPriority()
                withClue("precombat main phase") {
                    game.castSpell(1, "Glyph of Reincarnation", game.findPermanent("Wall of Wood")!!).error shouldNotBe null
                }
                game.attackAndBlock()
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.giveWallPlayerPriority()
                withClue("end of combat step is still combat") {
                    game.castSpell(1, "Glyph of Reincarnation", game.findPermanent("Wall of Wood")!!).error shouldNotBe null
                }
            }

            test("after combat, destroys what the Wall blocked and returns a creature card from that player's graveyard") {
                val game = setup()
                game.attackAndBlock()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.giveWallPlayerPriority()
                game.castSpell(1, "Glyph of Reincarnation", game.findPermanent("Wall of Wood")!!).error shouldBe null
                game.resolveStack()

                val choice = game.state.pendingDecision as SelectCardsDecision
                withClue("the caster chooses, from the attacker's graveyard, after the Bears have died") {
                    choice.playerId shouldBe game.player1Id
                    choice.options shouldContainExactlyInAnyOrder
                        game.findCardsInGraveyard(2, "Grizzly Bears") + game.findCardsInGraveyard(2, "Hill Giant")
                }
                game.selectCards(game.findCardsInGraveyard(2, "Hill Giant")).error shouldBe null

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                val giant = game.findPermanent("Hill Giant")!!
                withClue("it returns under its owner's control") {
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                }
                game.isOnBattlefield("Wall of Wood") shouldBe true
            }

            test("reads the graveyard of whoever controlled the creature when the Wall blocked it") {
                val game = setup()
                game.attackAndBlock()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.giveWallPlayerPriority()
                game.castSpell(1, "Caught Red-Handed", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player1Id

                game.giveWallPlayerPriority()
                game.castSpell(1, "Glyph of Reincarnation", game.findPermanent("Wall of Wood")!!).error shouldBe null
                game.resolveStack()

                val choice = game.state.pendingDecision as SelectCardsDecision
                withClue("player 2 controlled the Bears when they were blocked — not the Glyph's caster's graveyard") {
                    choice.options shouldContainExactlyInAnyOrder
                        game.findCardsInGraveyard(2, "Grizzly Bears") + game.findCardsInGraveyard(2, "Hill Giant")
                }
                game.selectCards(game.findCardsInGraveyard(2, "Grizzly Bears")).error shouldBe null
                game.state.projectedState.getController(game.findPermanent("Grizzly Bears")!!) shouldBe game.player2Id
            }
        }
    }
}
