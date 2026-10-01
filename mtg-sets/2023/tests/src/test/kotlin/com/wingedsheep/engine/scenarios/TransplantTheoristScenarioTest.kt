package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.TransplantTheorist
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Transplant Theorist (ONE #73) — {3}{U} Artifact Creature — Phyrexian Artificer, 2/4.
 *
 * Whenever this creature or another artifact you control enters, you may draw a card. If you do,
 * discard a card.
 * {2}: Put target card from your graveyard on the bottom of your library.
 */
class TransplantTheoristScenarioTest : ScenarioTestBase() {

    private val abilityId = TransplantTheorist.activatedAbilities.single().id

    init {
        context("Transplant Theorist") {

            test("its own entry offers a loot; accepting draws then discards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Transplant Theorist")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Transplant Theorist").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                withClue("drew the Forest") { game.librarySize(1) shouldBe 0 }
                val discard = game.getPendingDecision()
                discard.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("discarded Grizzly Bears, kept the Forest") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }
            }

            test("another artifact entering triggers; declining draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transplant Theorist")
                    .withCardInHand(1, "Ornithopter")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ornithopter").error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declined the may — no draw, no discard") {
                    game.librarySize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 0
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("a non-artifact creature entering does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transplant Theorist")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()

                withClue("no trigger for a non-artifact") {
                    game.hasPendingDecision() shouldBe false
                    game.librarySize(1) shouldBe 1
                }
            }

            test("{2}: puts a card from your graveyard on the bottom of your library, not an opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Transplant Theorist")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val theorist = game.findPermanent("Transplant Theorist")!!
                val bolt = game.findCardsInGraveyard(2, "Lightning Bolt").single()
                val illegal = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = theorist,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bolt, game.player2Id, Zone.GRAVEYARD)),
                    )
                )
                withClue("an opponent's graveyard card is not a legal target") { illegal.error shouldNotBe null }

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = theorist,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Card(bears, game.player1Id, Zone.GRAVEYARD)),
                    )
                )
                withClue("activation: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                withClue("Grizzly Bears is on the bottom of the library") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                    game.state.getLibrary(game.player1Id).last() shouldBe bears
                }
            }
        }
    }
}
