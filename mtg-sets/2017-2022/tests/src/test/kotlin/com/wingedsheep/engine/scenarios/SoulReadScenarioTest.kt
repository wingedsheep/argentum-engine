package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Soul Read ({3}{U} Instant), Choose one —
 *   • Counter target spell unless its controller pays {4}.
 *   • Draw two cards.
 */
class SoulReadScenarioTest : ScenarioTestBase() {

    init {
        fun counterBoard(opponentSpareLands: Int): TestGame {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 2 + opponentSpareLands)
                .withCardInHand(1, "Soul Read")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.execute(PassPriority(game.player2Id))

            val bears = game.state.stack.first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
            }
            val soulRead = game.state.getHand(game.player1Id).first { id ->
                game.state.getEntity(id)?.get<CardComponent>()?.name == "Soul Read"
            }
            game.execute(
                CastSpell(
                    game.player1Id,
                    soulRead,
                    listOf(ChosenTarget.Spell(bears)),
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(bears))),
                )
            ).error shouldBe null
            game.resolveStack()
            return game
        }

        test("counter mode: a spell whose controller can't pay {4} is countered") {
            val game = counterBoard(opponentSpareLands = 0)
            if (game.hasPendingDecision()) game.answerYesNo(false)
            game.resolveStack()

            withClue("Grizzly Bears was countered") {
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                game.findPermanent("Grizzly Bears") shouldBe null
            }
            game.isInGraveyard(1, "Soul Read") shouldBe true
        }

        test("counter mode: paying {4} lets the spell resolve") {
            val game = counterBoard(opponentSpareLands = 4)
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe game.player2Id
            game.answerYesNo(true).error shouldBe null
            withClue("Paying {4} asks the opponent which lands to tap") {
                game.state.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>().playerId shouldBe game.player2Id
            }
            game.submitManaSourcesAutoPay().error shouldBe null
            game.resolveStack()

            withClue("Grizzly Bears resolved after its controller paid {4}") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
        }

        test("draw mode: draws two cards with nothing on the stack") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Soul Read")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withLandsOnBattlefield(1, "Island", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.castSpellWithMode(1, "Soul Read", modeIndex = 1).error shouldBe null
            game.resolveStack()

            withClue("Soul Read left the hand and two cards were drawn") {
                game.handSize(1) shouldBe handBefore - 1 + 2
            }
        }
    }
}
