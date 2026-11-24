package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Guild Feud (RTR #97) — "At the beginning of your upkeep, target opponent reveals the top three
 * cards of their library, may put a creature card from among them onto the battlefield, then puts
 * the rest into their graveyard. You do the same with the top three cards of your library. If two
 * creatures are put onto the battlefield this way, those creatures fight each other."
 *
 * Covers who decides each half (the opponent first, then you), where the rest goes, and that the
 * fight happens only when both players put a creature onto the battlefield.
 */
class GuildFeudScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Guild Feud")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Grizzly Bears")
        .withCardInLibrary(2, "Island")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    private fun TestGame.optionNamed(decision: SelectCardsDecision, name: String): EntityId =
        decision.options.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.damageOn(id: EntityId): Int =
        state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    private fun TestGame.reachTrigger() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        state.activePlayerId shouldBe player1Id
        if (getPendingDecision() is ChooseTargetsDecision) {
            selectTargets(listOf(player2Id)).error shouldBe null
        }
        resolveStack()
    }

    init {
        context("Guild Feud") {

            test("the opponent chooses first, then you; the two creatures fight and the rest is milled") {
                val game = board()
                game.reachTrigger()

                val theirChoice = game.getPendingDecision()
                theirChoice.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the targeted opponent decides for their own three cards") {
                    theirChoice.playerId shouldBe game.player2Id
                    theirChoice.minSelections shouldBe 0
                    theirChoice.maxSelections shouldBe 1
                }
                game.selectCards(listOf(game.optionNamed(theirChoice, "Grizzly Bears"))).error shouldBe null

                withClue("the opponent's rest is in their graveyard before you reveal") {
                    game.graveyardSize(2) shouldBe 2
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                val yourChoice = game.getPendingDecision()
                yourChoice.shouldBeInstanceOf<SelectCardsDecision>()
                yourChoice.playerId shouldBe game.player1Id
                game.selectCards(listOf(game.optionNamed(yourChoice, "Hill Giant"))).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant enters under your control and fights Grizzly Bears") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                    game.damageOn(giant) shouldBe 2
                }
                withClue("Grizzly Bears died in the fight") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.graveyardSize(2) shouldBe 3
                }
                withClue("your rest went to your graveyard") {
                    game.graveyardSize(1) shouldBe 2
                    game.librarySize(1) shouldBe 0
                }
            }

            test("no fight when only one creature is put onto the battlefield") {
                val game = board()
                game.reachTrigger()

                val theirChoice = game.getPendingDecision()
                theirChoice.shouldBeInstanceOf<SelectCardsDecision>()
                theirChoice.playerId shouldBe game.player2Id
                game.skipSelection().error shouldBe null

                withClue("all three of the opponent's cards went to their graveyard") {
                    game.graveyardSize(2) shouldBe 3
                }

                val yourChoice = game.getPendingDecision()
                yourChoice.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(game.optionNamed(yourChoice, "Hill Giant"))).error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                withClue("Hill Giant entered and never fought") {
                    game.damageOn(giant) shouldBe 0
                }
                game.graveyardSize(1) shouldBe 2
            }
        }
    }
}
