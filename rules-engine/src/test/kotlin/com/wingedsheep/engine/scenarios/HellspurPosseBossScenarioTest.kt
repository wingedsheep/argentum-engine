package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hellspur Posse Boss (OTJ #128, {2}{R}{R} Creature — Lizard Rogue, 2/4).
 *
 *   Other outlaws you control have haste.
 *   When this creature enters, create two 1/1 red Mercenary creature tokens with
 *   "{T}: Target creature you control gets +1/+0 until end of turn. Activate only as a sorcery."
 *
 * Reuses the OTJ outlaw subtype group ([com.wingedsheep.sdk.core.Subtype.OUTLAW_TYPES]) for the
 * `excludeSelf` haste lord and the standard Mercenary token for the ETB trigger.
 */
class HellspurPosseBossScenarioTest : ScenarioTestBase() {

    init {
        context("Hellspur Posse Boss") {

            test("ETB creates two Mercenary tokens and they gain haste from the lord") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hellspur Posse Boss")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the Boss; its ETB trigger creates the two Mercenary tokens on resolution.
                game.castSpell(1, "Hellspur Posse Boss").error shouldBe null
                game.resolveStack() // resolve the Boss spell -> ETB trigger goes on the stack
                game.resolveStack() // resolve the ETB trigger

                val tokens = game.state.getBattlefield().filter { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Mercenary Token"
                }
                withClue("ETB created exactly two Mercenary tokens") {
                    tokens.size shouldBe 2
                }

                // Mercenary tokens are outlaws (Mercenary) controlled by you, so the lord grants
                // them haste even though they just entered.
                val projected = game.state.projectedState
                withClue("Each Mercenary token has haste from the lord") {
                    tokens.forEach { token ->
                        projected.hasKeyword(token, Keyword.HASTE) shouldBe true
                    }
                }
            }

            test("the lord does not grant haste to itself (excludeSelf)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hellspur Posse Boss", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.resolveStack()

                val boss = game.findPermanent("Hellspur Posse Boss")!!
                withClue("The Boss (a Rogue) does not grant haste to itself") {
                    game.state.projectedState.hasKeyword(boss, Keyword.HASTE) shouldBe false
                }
            }

            test("the lord does not grant haste to non-outlaw creatures you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = true) // Bear, not an outlaw
                    .withCardOnBattlefield(1, "Hellspur Posse Boss", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("A non-outlaw creature you control does not gain haste") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }
            }
        }
    }
}
