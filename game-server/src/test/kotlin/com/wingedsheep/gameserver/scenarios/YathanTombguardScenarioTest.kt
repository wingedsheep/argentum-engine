package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Yathan Tombguard (TDM #100) — {2}{B}, 2/3 Human Warrior, Menace.
 *
 * "Whenever a creature you control with a counter on it deals combat damage to a player,
 *  you draw a card and you lose 1 life."
 *
 * The trigger is bound ANY with a `sourceFilter` of "a creature you control with a counter
 * on it". "You" is Yathan's controller, so both the draw and the 1 life loss apply to the
 * controller. Verifies the positive case (countered attacker → draw + lose 1), and that a
 * counterless attacker does not trigger it.
 */
class YathanTombguardScenarioTest : ScenarioTestBase() {

    init {
        context("Yathan Tombguard combat-damage trigger") {

            test("a countered creature dealing combat damage draws a card and loses 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yathan Tombguard")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Put a +1/+1 counter on Grizzly Bears so it is "a creature you control with a counter on it".
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }

                val startHand = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.step != Step.POSTCOMBAT_MAIN && iterations++ < 20) {
                    game.passPriority()
                    game.resolveStack()
                }

                withClue("Grizzly Bears (3/3 with counter) deals 3 combat damage to Player2") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("Yathan's trigger draws Player1 a card") {
                    game.handSize(1) shouldBe startHand + 1
                }
                withClue("Yathan's trigger costs Player1 1 life") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }

            test("a creature with no counter dealing combat damage does not trigger Yathan") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Yathan Tombguard")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val startHand = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                var iterations = 0
                while (game.state.step != Step.POSTCOMBAT_MAIN && iterations++ < 20) {
                    game.passPriority()
                    game.resolveStack()
                }

                withClue("Grizzly Bears (2/2, no counter) deals 2 combat damage to Player2") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("No counter on the attacker → Yathan does not trigger, no draw") {
                    game.handSize(1) shouldBe startHand
                }
                withClue("No trigger → Player1 loses no life") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
