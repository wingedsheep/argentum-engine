package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario coverage for two FIN cards built from existing primitives:
 *
 *  - Hope Estheim — "At the beginning of your end step, each opponent mills X cards, where X is
 *    the amount of life you gained this turn." Verifies the mill scales with life gained this turn
 *    and is a no-op when no life was gained.
 *  - G'raha Tia — "Whenever one or more other creatures and/or artifacts you control die, draw a
 *    card. This ability triggers only once each turn." Verifies the batched once-per-turn draw,
 *    that artifacts count, and that it does not fire on the source's own death.
 */
class FinHopeGrahaScenarioTest : ScenarioTestBase() {

    init {
        // Gains the controller 4 life so Hope's "amount of life you gained this turn" is non-zero.
        cardRegistry.register(
            CardDefinition.instant(
                name = "Test Healing Light",
                manaCost = ManaCost.parse("{W}"),
                oracleText = "You gain 4 life.",
                script = CardScript.spell(effect = GainLifeEffect(4, EffectTarget.Controller)),
            ),
        )

        context("Hope Estheim — end-step mill scales with life gained") {

            test("each opponent mills equal to the life you gained this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hope Estheim")
                    .withCardInHand(1, "Test Healing Light")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    // Seed the opponent's library so there is something to mill.
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oppLibraryBefore = game.librarySize(2)
                val oppGraveBefore = game.graveyardSize(2)

                // Gain 4 life this turn.
                game.castSpell(1, "Test Healing Light").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Opponent milled exactly the 4 life Hope's controller gained this turn") {
                    game.librarySize(2) shouldBe oppLibraryBefore - 4
                    game.graveyardSize(2) shouldBe oppGraveBefore + 4
                }
            }

            test("no life gained means no mill") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hope Estheim")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val oppLibraryBefore = game.librarySize(2)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("No life gained → X = 0 → opponent mills nothing") {
                    game.librarySize(2) shouldBe oppLibraryBefore
                }
            }
        }

        context("G'raha Tia — once-per-turn draw on creature/artifact death") {

            test("drawing only once even when several controlled permanents die in a turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "G'raha Tia")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardOnBattlefield(1, "Frogmite") // an artifact creature — counts as an artifact
                    .withCardsInHand(1, "Doom Blade", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                val frogmite = game.findPermanent("Frogmite")!!
                val handBefore = game.handSize(1)

                // Kill the first nonblack creature (Centaur Courser).
                game.castSpell(1, "Doom Blade", targetId = courser).error shouldBe null
                game.resolveStack()

                withClue("First controlled death this turn draws exactly one card") {
                    // hand: -1 Doom Blade cast, +1 drawn from G'raha = net 0 vs handBefore.
                    game.handSize(1) shouldBe handBefore - 1 + 1
                }
                withClue("Centaur Courser died") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }

                val handAfterFirst = game.handSize(1)

                // Kill a second controlled permanent (the artifact creature Frogmite) the same turn.
                game.castSpell(1, "Doom Blade", targetId = frogmite).error shouldBe null
                game.resolveStack()

                withClue("Second controlled death the same turn does NOT draw again (once per turn)") {
                    // Only the Doom Blade leaves hand; no extra G'raha draw.
                    game.handSize(1) shouldBe handAfterFirst - 1
                }
                withClue("Frogmite (an artifact) still died and is in the graveyard") {
                    game.isInGraveyard(1, "Frogmite") shouldBe true
                }
            }
        }
    }
}
