package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aether Revolt (MH3) — "Revolt — As long as a permanent left the battlefield under your control
 * this turn, if a source you control would deal noncombat damage to an opponent or a permanent an
 * opponent controls, it deals that much damage plus 2 instead. / Whenever you get one or more {E},
 * this enchantment deals that much damage to any target."
 *
 * Rulings pinned:
 *  - "If you get multiple {E} at once, Aether Revolt's last ability will trigger only once ... equal
 *    to the amount of {E} you gained." (2024-06-07)
 *  - "If an effect instructs you to get one or more {E} and then allows you to spend {E}, Aether
 *    Revolt's last ability will see the amount of {E} you got." (2024-06-07)
 */
class AetherRevoltScenarioTest : ScenarioTestBase() {

    init {
        context("Aether Revolt — whenever you get one or more {E}") {

            test("getting {E}{E}{E} fires once for 3, even after spending all of it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Revolt")
                    .withCardInHand(1, "Galvanic Discharge")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Galvanic Discharge", targetId = wurm).error shouldBe null
                game.resolveStack()
                game.chooseNumber(3).error shouldBe null

                withClue("one trigger, asking for its single target") {
                    game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                }
                game.selectTargets(listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("3 damage to the opponent — the amount of {E} gained, not what remains") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }

        context("Aether Revolt — revolt") {

            test("noncombat damage to an opponent is unchanged when no permanent of yours left") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Revolt")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 18
            }

            test("once a permanent you controlled left, noncombat damage to an opponent gets +2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aether Revolt")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Shock")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Shock", targetId = bears).error shouldBe null
                game.resolveStack()
                withClue("a permanent you control left the battlefield — revolt is on") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 16
            }
        }
    }
}
