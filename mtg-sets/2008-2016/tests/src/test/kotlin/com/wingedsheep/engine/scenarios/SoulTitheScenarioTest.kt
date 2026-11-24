package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Soul Tithe — "At the beginning of the upkeep of enchanted permanent's
 * controller, that player sacrifices it unless they pay {X}, where X is its mana value."
 *
 * The parts a reader can't confirm from the script alone: the trigger keys off the *enchanted
 * permanent's* controller (Player2) even though Player1 controls the Aura, Player2 is the one asked
 * and charged, X is the host's mana value (Grizzly Bears → {2}), and the sacrifice happens even
 * though the ability's controller (Player1) doesn't control the permanent.
 */
class SoulTitheScenarioTest : ScenarioTestBase() {

    init {
        context("Soul Tithe upkeep tax") {

            fun build(forests: Int): TestGame {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Soul Tithe", "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", forests)
                    .withActivePlayer(1)
                repeat(5) {
                    builder.withCardInLibrary(1, "Plains")
                    builder.withCardInLibrary(2, "Forest")
                }
                return builder.build()
            }

            test("the enchanted permanent's controller pays its mana value on their upkeep and keeps it") {
                val game = build(forests = 2)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<YesNoDecision>()
                withClue("Player2 — the Bears' controller, not the Aura's — is asked to pay") {
                    decision.playerId shouldBe game.player2Id
                }

                game.answerYesNo(true)
                while (game.getPendingDecision() is SelectManaSourcesDecision) {
                    game.submitManaSourcesAutoPay()
                }
                game.resolveStack()

                withClue("Paid {2}: the Bears stay") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("X was the Bears' mana value, so both Forests were tapped") {
                    game.findPermanents("Forest").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
            }

            test("declining to pay makes that player sacrifice it") {
                val game = build(forests = 2)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()

                game.answerYesNo(false)
                game.resolveStack()

                withClue("Bears go to Player2's graveyard, and the Aura follows as a state-based action") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Soul Tithe") shouldBe true
                }
            }

            test("a player who can't afford X sacrifices it without being asked") {
                val game = build(forests = 1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("One Forest can't pay {2}: no prompt, straight to the sacrifice") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("the Aura controller's own upkeep doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Soul Tithe", "Grizzly Bears")
                    .withActivePlayer(2)
                    .also { b ->
                        repeat(5) {
                            b.withCardInLibrary(1, "Plains")
                            b.withCardInLibrary(2, "Forest")
                        }
                    }
                    .build()

                // Player2's turn → Player1's upkeep.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Player1 controls the Aura, but its upkeep isn't the Bears' controller's") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.hasPendingDecision() shouldBe false
                }
            }
        }
    }
}
