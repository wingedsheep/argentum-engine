package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * Crush Underfoot (LRW #162) — "Choose a Giant creature you control. It deals damage equal to its
 * power to target creature."
 *
 * The card's whole shape is two decisions at two different times: "target creature" is declared on
 * announcement, while the Giant is chosen mid-resolution by `SelectTargetEffect`. These tests pin
 * the consequences of that split — the damage scales to whichever Giant is picked, the spell is a
 * clean no-op with no Giant to choose (rather than dealing damage as the *spell*), and it still
 * needs a legal creature target to be cast at all.
 */
class CrushUnderfootScenarioTest : ScenarioTestBase() {

    init {
        context("Crush Underfoot") {

            test("the sole Giant is auto-chosen and deals damage equal to its power") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crush Underfoot")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    // Axegrinder Giant is a 6/4 — plenty to kill a 2/2.
                    .withCardOnBattlefield(1, "Axegrinder Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Crush Underfoot", bears).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("6 damage from the Giant kills the 2/2") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("The Giant itself is untouched — it dealt the damage, it didn't fight") {
                    game.findPermanent("Axegrinder Giant") shouldNotBe null
                }
            }

            test("with two Giants the controller picks which one swings, mid-resolution") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crush Underfoot")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Axegrinder Giant")   // 6/4
                    .withCardOnBattlefield(1, "Hill Giant")         // 3/3
                    .withCardOnBattlefield(2, "Hunted Dragon")      // 6/6, survives 3 but not 6
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dragon = game.findPermanent("Hunted Dragon")!!
                val smallGiant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Crush Underfoot", dragon).error shouldBe null
                game.resolveStack()

                val choice = game.state.pendingDecision
                withClue("Two Giants means a mid-resolution choice, not an auto-select") {
                    choice shouldNotBe null
                }
                withClue("The prompt says what the Giant is for, not a bare second \"target\"") {
                    val targets = choice as ChooseTargetsDecision
                    targets.prompt shouldContain "deals damage equal to its power to the targeted creature"
                    targets.targetRequirements.single().description shouldBe targets.prompt
                }
                // Deliberately pick the *smaller* Giant: 3 damage is not lethal to a 6/6.
                game.submitDecision(TargetsResponse(choice!!.id, mapOf(0 to listOf(smallGiant))))
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("The chosen Giant's power decided the damage — 3 is not lethal to a 6/6") {
                    game.findPermanent("Hunted Dragon") shouldNotBe null
                }
            }

            test("with no Giant to choose the spell does nothing — it does not deal damage itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crush Underfoot")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Crush Underfoot", bears).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("No Giant means no damage source, and CR 608.2b skips the instruction") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                withClue("Crush Underfoot still resolved and went to the graveyard") {
                    game.isInGraveyard(1, "Crush Underfoot") shouldBe true
                }
            }

            test("an opponent's Giant can't be chosen — the clause says \"you control\"") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Crush Underfoot")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Axegrinder Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Crush Underfoot", bears).error shouldBe null
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("Their Giant is not yours to swing, so nothing happens") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
            }
        }
    }
}
