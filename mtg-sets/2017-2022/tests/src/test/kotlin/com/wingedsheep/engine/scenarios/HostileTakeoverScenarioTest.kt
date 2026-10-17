package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hostile Takeover (SNC #191) — {2}{U}{B}{R} Sorcery.
 *
 *   Up to one target creature has base power and toughness 1/1 until end of turn. Up to one
 *   other target creature has base power and toughness 4/4 until end of turn. Then Hostile
 *   Takeover deals 3 damage to each creature.
 *
 * The base-P/T effects must apply before the sweep (a 4/4'd 2/2 survives, a 1/1'd 3/3 dies), and
 * a lone target announced for the second group must get 4/4, not 1/1.
 */
class HostileTakeoverScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Hostile Takeover")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false) // 3/3
        .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false) // 6/4
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Hostile Takeover") {

            test("1/1 target dies, 4/4 target survives, untargeted creatures take 3") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Hostile Takeover").first()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears)),
                        targetGroupCounts = listOf(1, 1),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant became 1/1 and died to the 3 damage") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("Grizzly Bears became 4/4 and survived the 3 damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                withClue("untargeted Craw Wurm (6/4) survives 3 damage") {
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                }
            }

            test("a lone target announced for the second group gets 4/4, not 1/1") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!

                val card = game.findCardsInHand(1, "Hostile Takeover").first()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(ChosenTarget.Permanent(bears)),
                        targetGroupCounts = listOf(0, 1),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears became 4/4 and survived") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.projectedState.getPower(bears) shouldBe 4
                }
                withClue("untargeted Hill Giant (3/3) dies to the sweep") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
            }

            test("with no targets it is a plain 3-damage sweep") {
                val game = board()

                val card = game.findCardsInHand(1, "Hostile Takeover").first()
                game.execute(CastSpell(game.player1Id, card, emptyList())).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Craw Wurm") shouldBe true
            }
        }
    }
}
