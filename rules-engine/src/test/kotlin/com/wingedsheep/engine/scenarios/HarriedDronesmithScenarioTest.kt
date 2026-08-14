package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Harried Dronesmith (MKM #131) — {3}{R} 2/3 Creature — Human Artificer.
 *
 * "At the beginning of combat on your turn, create a 1/1 colorless Thopter artifact creature token
 *  with flying. It gains haste until end of turn. Sacrifice it at the beginning of your next end
 *  step."
 *
 * Covers the `Effects.CreateToken(sacrificeAtStep = Step.END)` path — the plain-token sibling of
 * `CreateTokenCopyOfTarget`'s delayed sacrifice. The token must arrive hasty and flying, be able to
 * attack the turn it is made, and be gone by the end step of that same turn.
 *
 * Note: the begin-combat trigger only fires when the step is reached by actually passing priority
 * ([passUntilPhase]); `advanceToPhase` rewrites phase/step without the step event.
 */
class HarriedDronesmithScenarioTest : ScenarioTestBase() {

    init {
        context("Harried Dronesmith — disposable hasty Thopter each combat") {

            test("begin combat mints a 1/1 flying, hasty Thopter artifact token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harried Dronesmith", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("No Thopter before combat begins") {
                    game.findPermanent("Thopter") shouldBe null
                }

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val thopter = game.findPermanent("Thopter")
                withClue("The begin-combat trigger created the Thopter") {
                    (thopter != null) shouldBe true
                }

                val projected = game.state.projectedState
                projected.getPower(thopter!!) shouldBe 1
                projected.getToughness(thopter) shouldBe 1
                projected.hasType(thopter, "ARTIFACT") shouldBe true
                projected.isCreature(thopter) shouldBe true
                projected.hasKeyword(thopter, Keyword.FLYING) shouldBe true
                withClue("Haste — the token has to be able to attack the turn it arrives") {
                    projected.hasKeyword(thopter, Keyword.HASTE) shouldBe true
                }
            }

            test("the hasty Thopter can attack the turn it is created") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harried Dronesmith", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                game.isOnBattlefield("Thopter") shouldBe true

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Thopter" to 2)).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("A summoning-sick token could not have attacked; 1 damage got through") {
                    game.getLifeTotal(2) shouldBe 19
                }
            }

            test("the Thopter is sacrificed at the end step of the same turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Harried Dronesmith", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()
                game.isOnBattlefield("Thopter") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("Delayed trigger sacrifices it at the beginning of your next end step") {
                    game.isOnBattlefield("Thopter") shouldBe false
                }
                withClue("The Dronesmith itself survives — only the token is sacrificed") {
                    game.isOnBattlefield("Harried Dronesmith") shouldBe true
                }
            }
        }
    }
}
