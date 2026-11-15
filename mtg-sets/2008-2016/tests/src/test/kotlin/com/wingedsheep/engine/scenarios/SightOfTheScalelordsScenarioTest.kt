package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sight of the Scalelords (DTK #207) — {4}{G} Enchantment.
 *
 * "At the beginning of combat on your turn, creatures you control with toughness 4 or greater get
 * +2/+2 and gain vigilance until end of turn."
 *
 * Pins the toughness gate (a 2/4 is pumped, a 3/3 is not), the controller gate (an opponent's 2/4
 * is not), and that nothing happens at the beginning of combat on the opponent's turn.
 */
class SightOfTheScalelordsScenarioTest : ScenarioTestBase() {

    init {
        context("Sight of the Scalelords") {

            test("on your turn, your creatures with toughness 4+ get +2/+2 and vigilance") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sight of the Scalelords")
                    .withCardOnBattlefield(1, "Giant Spider")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Giant Spider")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mySpider = game.findPermanents("Giant Spider").first { it in game.state.getBattlefield(game.player1Id) }
                val theirSpider = game.findPermanents("Giant Spider").first { it != mySpider }
                val giant = game.findPermanent("Hill Giant")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("your 2/4 becomes a 4/6 with vigilance") {
                    projected.getPower(mySpider) shouldBe 4
                    projected.getToughness(mySpider) shouldBe 6
                    projected.hasKeyword(mySpider, Keyword.VIGILANCE) shouldBe true
                }
                withClue("your 3/3 is below the toughness gate") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                    projected.hasKeyword(giant, Keyword.VIGILANCE) shouldBe false
                }
                withClue("the opponent's 2/4 is not yours") {
                    projected.getPower(theirSpider) shouldBe 2
                    projected.hasKeyword(theirSpider, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("does not trigger at the beginning of combat on an opponent's turn") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Sight of the Scalelords")
                    .withCardOnBattlefield(1, "Giant Spider")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val spider = game.findPermanent("Giant Spider")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.state.stack.isEmpty() shouldBe true
                game.resolveStack()

                game.state.projectedState.getPower(spider) shouldBe 2
                game.state.projectedState.hasKeyword(spider, Keyword.VIGILANCE) shouldBe false
            }
        }
    }
}
