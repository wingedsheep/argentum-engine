package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.ecl.cards.FormidableSpeaker
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Regression for issue #1365: Blossombind's "Enchanted creature can't become untapped" must block
 * *every* untap source — including explicit untap effects like Formidable Speaker's
 * "{1}, {T}: Untap another target permanent." — not only the controller's untap step.
 *
 * Blossombind now grants [AbilityFlag.CANT_BECOME_UNTAPPED] (the stronger continuous "can't"
 * restriction) rather than [AbilityFlag.DOESNT_UNTAP] (untap-step-only, CR 502.3). These tests pin
 * the distinction:
 *   1. CANT_BECOME_UNTAPPED blocks the untap effect (the bug).
 *   2. DOESNT_UNTAP does NOT block the untap effect (the narrower flag is unchanged).
 *   3. CANT_BECOME_UNTAPPED also keeps the creature tapped through its controller's untap step
 *      (the stronger restriction subsumes the weaker one).
 */
class BlossombindUntapRestrictionTest : ScenarioTestBase() {

    /** Formidable Speaker's only activated ability — "{1}, {T}: Untap another target permanent." */
    private val untapAbilityId = FormidableSpeaker.activatedAbilities.first { !it.isManaAbility }.id

    init {

        test("Blossombind blocks Formidable Speaker's untap effect (creature stays tapped)") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardAttachedTo(1, "Blossombind", "Grizzly Bears")
                .withCardOnBattlefield(1, "Formidable Speaker")
                .withCardOnBattlefield(1, "Island") // pays the {1}
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val speaker = game.findPermanent("Formidable Speaker")!!

            withClue("Blossombind grants CANT_BECOME_UNTAPPED to the enchanted creature") {
                game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BECOME_UNTAPPED) shouldBe true
            }

            val activation = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = speaker,
                    abilityId = untapAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            )
            withClue("Activating the untap ability should succeed: ${activation.error}") {
                activation.error shouldBe null
            }
            game.resolveStack()

            withClue("The enchanted creature can't become untapped — it stays tapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }

        test("Charmed Sleep (DOESNT_UNTAP only) does NOT block the untap effect") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardAttachedTo(1, "Charmed Sleep", "Grizzly Bears")
                .withCardOnBattlefield(1, "Formidable Speaker")
                .withCardOnBattlefield(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val speaker = game.findPermanent("Formidable Speaker")!!

            withClue("Charmed Sleep grants the narrower DOESNT_UNTAP, not CANT_BECOME_UNTAPPED") {
                game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BECOME_UNTAPPED) shouldBe false
            }

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = speaker,
                    abilityId = untapAbilityId,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("DOESNT_UNTAP only blocks the untap step, so the untap effect untaps it") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            }
        }

        test("Blossombind keeps the creature tapped through its controller's untap step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true)
                .withCardAttachedTo(1, "Blossombind", "Grizzly Bears")
                .withCardsInHand(1, "Forest", 5)
                .withCardsInHand(2, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!

            // Advance a full cycle back to Player 1's next turn: their untap step runs between
            // Player 2's turn and Player 1's next upkeep (the untap step has no priority window,
            // so we stop at the following upkeep).
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Player 2's upkeep
            withClue("first stop is Player 2's upkeep") {
                game.state.activePlayerId shouldBe game.player2Id
            }
            // Step past Player 2's upkeep, then on to Player 1's next upkeep (their untap step
            // runs on the turn boundary in between).
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // Player 2's main
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Player 1's next upkeep
            withClue("second stop is Player 1's next upkeep — their untap step just ran") {
                game.state.activePlayerId shouldBe game.player1Id
            }

            withClue("Blossombind kept the creature tapped through Player 1's untap step") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
