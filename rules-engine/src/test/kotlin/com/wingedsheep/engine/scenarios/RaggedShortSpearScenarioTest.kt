package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ragged Short Spear (HOB) — {1}{R} Artifact — Equipment.
 *
 * "When this Equipment enters, you may discard a card. If you do, draw two cards.
 *  Equipped creature gets +2/+0.
 *  Equip {3}"
 *
 * The ETB is a may with an if-you-do rider, so declining must draw nothing, and accepting must
 * both discard and draw. The buff is power-only, which the equip test pins down.
 */
class RaggedShortSpearScenarioTest : ScenarioTestBase() {

    init {
        context("Ragged Short Spear") {

            test("accepting the may discards one and draws two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragged Short Spear")
                    // Two cards left in hand, so the discard is a genuine choice.
                    .withCardInHand(1, "Centaur Courser")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(1, "Ragged Short Spear").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the ETB asks whether to discard") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                game.answerYesNo(true).error shouldBe null

                val discard = game.getPendingDecision()
                withClue("accepting then asks which card to discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                val discardOptions = (discard as SelectCardsDecision).options
                withClue("both remaining cards in hand are offered") {
                    discardOptions.size shouldBe 2
                }
                game.selectCards(listOf(discardOptions.first())).error shouldBe null
                game.resolveStack()

                withClue("two cards were drawn") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                }
                withClue("discarded 1 and drew 2 — hand went from 2 to 3") {
                    game.handSize(1) shouldBe 3
                }
                withClue("the discarded card is in the graveyard") {
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("declining the may draws nothing and discards nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ragged Short Spear")
                    .withCardInHand(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(1, "Ragged Short Spear").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("'if you do' never fired — no draw") {
                    game.librarySize(1) shouldBe libraryBefore
                }
                withClue("the hand still holds the one card that was never discarded") {
                    game.handSize(1) shouldBe 1
                    game.graveyardSize(1) shouldBe 0
                }
                withClue("the Equipment still entered") {
                    game.isOnBattlefield("Ragged Short Spear") shouldBe true
                }
            }

            test("equipping grants +2/+0 — power only") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ragged Short Spear")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spear = game.findPermanent("Ragged Short Spear")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val equip = cardRegistry.requireCard("Ragged Short Spear")
                    .activatedAbilities.single { it.isEquipAbility }.id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id, sourceId = spear, abilityId = equip,
                        targets = listOf(ChosenTarget.Permanent(courser))
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.state.getEntity(spear)?.get<AttachedToComponent>()?.targetId shouldBe courser
                withClue("+2/+0 — toughness must not move") {
                    game.state.projectedState.getPower(courser) shouldBe 5
                    game.state.projectedState.getToughness(courser) shouldBe 3
                }
            }
        }
    }
}
