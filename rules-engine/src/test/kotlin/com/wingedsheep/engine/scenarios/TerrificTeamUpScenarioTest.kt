package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Terrific Team-Up (SPM #120) — {3}{G} Instant.
 *
 * "This spell costs {2} less to cast if you control a permanent with mana value 4 or greater.
 *  One or two target creatures you control each get +1/+0 until end of turn. They each deal
 *  damage equal to their power to target creature an opponent controls."
 *
 * Verifies (1) the conditional {2} cost reduction keys off controlling an mv>=4 permanent,
 * (2) the pump applies before the damage so each creature deals its *boosted* power, and
 * (3) the single-creature mode. Targets are supplied at cast time in requirement order: the
 * opponent's creature (declared first), then the one or two creatures you control.
 */
class TerrificTeamUpScenarioTest : ScenarioTestBase() {

    init {
        context("Terrific Team-Up") {

            test("costs {2} less while controlling a permanent with mana value 4+") {
                // Hill Giant is {3}{R} — mana value 4, so it enables the discount.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Terrific Team-Up")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Terrific Team-Up"),
                    game.player1Id,
                )

                withClue("controlling an mv>=4 permanent reduces the generic from 3 to 1") {
                    cost.genericAmount shouldBe 1
                }
            }

            test("no discount without an mv>=4 permanent") {
                // Grizzly Bears is {1}{G} — mana value 2, below the threshold.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Terrific Team-Up")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .build()

                val calculator = CostCalculator(cardRegistry)
                val cost = calculator.calculateEffectiveCost(
                    game.state,
                    cardRegistry.requireCard("Terrific Team-Up"),
                    game.player1Id,
                )

                withClue("no mv>=4 permanent means the full {3} generic remains") {
                    cost.genericAmount shouldBe 3
                }
            }

            test("two creatures get +1/+0 and each deals its boosted power to the opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Terrific Team-Up")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 -> 4/3
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 -> 3/2
                    .withCardOnBattlefield(2, "Colossus of Sardia") // 9/9 victim, survives to hold marked damage
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val victim = game.findPermanent("Colossus of Sardia")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Terrific Team-Up"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Permanent(victim),
                            ChosenTarget.Permanent(giant),
                            ChosenTarget.Permanent(bears),
                        ),
                    ),
                )
                withClue("Casting Terrific Team-Up should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Boosted powers: Hill Giant 3->4, Grizzly Bears 2->3, total 4 + 3 = 7 damage.
                withClue("victim took the summed boosted power (4 + 3 = 7)") {
                    game.state.getEntity(victim)?.get<DamageComponent>()?.amount shouldBe 7
                }
            }

            test("single creature deals its boosted power (the +1/+0 is lethal)") {
                // Hill Giant 3/3 becomes 4/3, dealing 4 to a 4-toughness creature that its base
                // power of 3 could not have killed.
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Terrific Team-Up")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3 -> 4/3
                    .withCardOnBattlefield(2, "Air Elemental") // 4/4 victim
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val victim = game.findPermanent("Air Elemental")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Terrific Team-Up"
                }

                val cast = game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = spell,
                        targets = listOf(
                            ChosenTarget.Permanent(victim),
                            ChosenTarget.Permanent(giant),
                        ),
                    ),
                )
                cast.error shouldBe null
                game.resolveStack()

                withClue("4 boosted damage kills the 4/4 (base power 3 would not have)") {
                    game.isOnBattlefield("Air Elemental") shouldBe false
                    game.isInGraveyard(2, "Air Elemental") shouldBe true
                }
            }
        }
    }
}
