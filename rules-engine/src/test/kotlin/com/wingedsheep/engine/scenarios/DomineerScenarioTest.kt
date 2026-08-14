package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario coverage for Domineer (MRD #33).
 *
 * {1}{U}{U} Enchantment — Aura
 * "Enchant artifact creature
 *  You control enchanted artifact creature."
 *
 * Mirrodin's narrow Control Magic. The claims worth pinning: the enchant restriction really is
 * *artifact* creature (a plain creature can't be targeted), control moves while the Aura is on the
 * battlefield, and it snaps back the moment the Aura leaves.
 */
class DomineerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Domineer") {

            test("stealing an opponent's artifact creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Alpha Myr")
                    .withCardInHand(1, "Domineer")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Alpha Myr")!!
                withClue("it starts under the opponent's control") {
                    projector.project(game.state).getController(myr) shouldBe game.player2Id
                }

                game.castSpell(1, "Domineer", targetId = myr).error shouldBe null
                game.resolveStack()

                withClue("the Aura resolved and moved control to its controller") {
                    game.findPermanent("Domineer") shouldNotBe null
                    projector.project(game.state).getController(myr) shouldBe game.player1Id
                }
            }

            test("cannot enchant a creature that is not an artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInHand(1, "Domineer")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("'enchant artifact creature' is a targeting restriction, not flavor") {
                    game.castSpell(1, "Domineer", targetId = giant).error shouldNotBe null
                    projector.project(game.state).getController(giant) shouldBe game.player2Id
                }
            }

            test("control reverts as soon as the Aura leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(2, "Alpha Myr")
                    .withCardOnBattlefield(1, "Domineer")
                    .withCardAttachedTo(1, "Domineer", "Alpha Myr")
                    .withCardInHand(1, "Altar's Light")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myr = game.findPermanent("Alpha Myr")!!
                val domineer = game.findPermanent("Domineer")!!

                withClue("the pre-attached Aura already has control") {
                    projector.project(game.state).getController(myr) shouldBe game.player1Id
                }

                game.castSpell(1, "Altar's Light", targetId = domineer).error shouldBe null
                game.resolveStack()

                withClue("no Aura, no control effect — the Myr goes home") {
                    game.findPermanent("Domineer") shouldBe null
                    projector.project(game.state).getController(myr) shouldBe game.player2Id
                }
            }
        }
    }
}
