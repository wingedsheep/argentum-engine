package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sevinne's Reclamation (C19 #5) — {2}{W} Sorcery, Flashback {4}{W}.
 * "Return target permanent card with mana value 3 or less from your graveyard to the battlefield.
 *  If this spell was cast from a graveyard, you may copy this spell and may choose a new target for
 *  the copy."
 *
 * Pins the ruling that the copy "wasn't cast from a graveyard, so it won't make another copy of
 * itself" (CR 707.10: a copy of a spell isn't cast).
 */
class SevinnesReclamationScenarioTest : ScenarioTestBase() {

    init {
        context("Sevinne's Reclamation") {

            test("hard cast from hand returns one card and offers no copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sevinne's Reclamation")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.castSpellTargetingGraveyardCard(1, "Sevinne's Reclamation", listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("no copy prompt for a spell cast from hand") { game.state.pendingDecision shouldBe null }
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
                game.isInGraveyard(1, "Sevinne's Reclamation") shouldBe true
            }

            test("flashback cast copies once onto a new target, and the copy does not copy itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Sevinne's Reclamation")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
                val spell = game.findCardsInGraveyard(1, "Sevinne's Reclamation").single()

                game.execute(
                    CastSpell(game.player1Id, spell, listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(lions)).error shouldBe null

                withClue("the flashback original is exiled; its copy waits on the stack") {
                    game.isInExile(1, "Sevinne's Reclamation") shouldBe true
                    game.state.stack.size shouldBe 1
                }

                game.resolveStack()
                game.isOnBattlefield("Savannah Lions") shouldBe true
                withClue("the copy wasn't cast from a graveyard, so it offers no further copy") {
                    game.state.pendingDecision shouldBe null
                    game.state.stack.size shouldBe 0
                }
                game.isInGraveyard(1, "Glory Seeker") shouldBe true
            }

            test("flashback cast may decline the copy") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Sevinne's Reclamation")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val spell = game.findCardsInGraveyard(1, "Sevinne's Reclamation").single()
                game.execute(
                    CastSpell(game.player1Id, spell, listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)))
                ).error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null

                game.state.stack.size shouldBe 0
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
            }
        }
    }
}
