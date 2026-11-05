package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dom.cards.KnightOfMalice
import com.wingedsheep.mtg.sets.definitions.hou.cards.MajesticMyriarch
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Majestic Myriarch (HOU #122, {4}{G}, * / *).
 *
 *   Majestic Myriarch's power and toughness are each equal to twice the number of creatures you control.
 *   At the beginning of each combat, this creature gains flying until end of turn if you control a
 *   creature with flying. The same is true for first strike, double strike, deathtouch, haste,
 *   hexproof, indestructible, lifelink, menace, reach, trample, and vigilance.
 *
 * Pins the CDA (counting itself), the per-keyword gate, the 2020 ruling that a "hexproof from
 * white" creature hands the Myriarch that specific variant (and not plain hexproof), and that the
 * trigger fires on the opponent's combat too.
 */
class MajesticMyriarchScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(listOf(MajesticMyriarch, KnightOfMalice))

        test("power and toughness are twice the creatures you control, itself included") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Majestic Myriarch")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myriarch = game.findPermanent("Majestic Myriarch")!!
            val projected = game.state.projectedState
            withClue("two creatures you control — the opponent's Lions don't count") {
                projected.getPower(myriarch) shouldBe 4
                projected.getToughness(myriarch) shouldBe 4
            }
        }

        test("gains first strike and the specific hexproof-from-white variant, nothing else") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Majestic Myriarch")
                .withCardOnBattlefield(1, "Knight of Malice")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myriarch = game.findPermanent("Majestic Myriarch")!!
            withClue("nothing before combat") {
                game.state.projectedState.hasKeyword(myriarch, Keyword.FIRST_STRIKE) shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("the Knight has first strike") {
                projected.hasKeyword(myriarch, Keyword.FIRST_STRIKE) shouldBe true
            }
            withClue("the Knight's hexproof from white is passed on as that variant") {
                projected.getKeywords(myriarch).contains("HEXPROOF_FROM_WHITE") shouldBe true
            }
            withClue("hexproof from white is not hexproof") {
                projected.hasKeyword(myriarch, Keyword.HEXPROOF) shouldBe false
            }
            withClue("no creature you control has flying") {
                projected.hasKeyword(myriarch, Keyword.FLYING) shouldBe false
            }
        }

        test("the trigger fires at the beginning of the opponent's combat too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Majestic Myriarch")
                .withCardOnBattlefield(1, "Knight of Malice")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val myriarch = game.findPermanent("Majestic Myriarch")!!
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.resolveStack()

            game.state.projectedState.hasKeyword(myriarch, Keyword.FIRST_STRIKE) shouldBe true
        }
    }
}
