package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tithe Taker (RNA #27) — {1}{W} 2/1.
 *
 *   During your turn, spells your opponents cast cost {1} more to cast and abilities your
 *   opponents activate cost {1} more to activate unless they're mana abilities.
 *   Afterlife 1
 *
 * Both taxes are gated on its controller's turn; an opponent acting at instant speed during that
 * turn pays {1} more for a spell and for a non-mana activated ability (a bare "{T}:" becomes
 * "{1}, {T}:", per the ruling). On the opponent's own turn both are at printed cost.
 */
class TitheTakerScenarioTest : ScenarioTestBase() {

    private fun TestGame.castShockAction(player: Int) =
        getLegalActions(player).first { (it.action as? CastSpell)?.cardId == findCardsInHand(player, "Shock").first() }

    private fun TestGame.pingAction(player: Int) =
        getLegalActions(player).first { (it.action as? ActivateAbility)?.sourceId == findPermanent("Prodigal Sorcerer") }

    init {
        context("Tithe Taker — during its controller's turn") {
            test("an opponent's instant costs {1} more and their {T} ability becomes {1}, {T}") {
                val game = scenario()
                    .withPlayers("Taker", "Opponent")
                    .withCardOnBattlefield(1, "Tithe Taker")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(1)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Shock {R} is taxed to {1}{R}") {
                    game.castShockAction(2).manaCostString shouldBe "{1}{R}"
                }
                withClue("the ping gains a {1} mana part") {
                    game.pingAction(2).description shouldBe "{1}, {T}: Deal 1 damage to any target"
                }
            }

            test("Tithe Taker's controller pays nothing extra") {
                val game = scenario()
                    .withPlayers("Taker", "Opponent")
                    .withCardOnBattlefield(1, "Tithe Taker")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castShockAction(1).manaCostString shouldBe "{R}"
                game.pingAction(1).isAffordable shouldBe true
            }
        }

        context("Tithe Taker — on an opponent's turn") {
            test("the opponent's spells and abilities cost their printed amount") {
                val game = scenario()
                    .withPlayers("Taker", "Opponent")
                    .withCardOnBattlefield(1, "Tithe Taker")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val shock = game.castShockAction(2)
                shock.manaCostString shouldBe "{R}"
                shock.isAffordable shouldBe true
                game.pingAction(2).isAffordable shouldBe true
            }
        }

        context("Tithe Taker — afterlife 1") {
            test("dying creates a 1/1 white and black flying Spirit") {
                val game = scenario()
                    .withPlayers("Taker", "Opponent")
                    .withCardOnBattlefield(1, "Tithe Taker")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Shock", game.findPermanent("Tithe Taker")!!).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Tithe Taker") shouldBe true
                game.findPermanents("Spirit Token").size shouldBe 1
            }
        }
    }
}
