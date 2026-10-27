package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Vine Gecko (ZNR #219) — The first kicked spell you cast each turn costs {1} less to cast.
 * Whenever you cast a kicked spell, put a +1/+1 counter on this creature.
 *
 * Murasa Sproutling ({2}{G}, kicker {1}{G}) is the kicked spell: five mana kicked, four with the
 * Gecko's discount.
 */
class VineGeckoScenarioTest : ScenarioTestBase() {

    private fun TestGame.geckoCounters(): Int =
        state.getEntity(findPermanent("Vine Gecko")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("the first kicked spell costs {1} less and grows the Gecko") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vine Gecko")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sproutling = game.findCardsInHand(1, "Murasa Sproutling").single()
            game.execute(CastSpell(game.player1Id, sproutling, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            game.findPermanent("Murasa Sproutling") shouldNotBe null
            game.geckoCounters() shouldBe 1
        }

        test("only the first kicked spell each turn is discounted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vine Gecko")
                .withCardInHand(1, "Murasa Sproutling")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 8)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (first, second) = game.findCardsInHand(1, "Murasa Sproutling")
            game.execute(CastSpell(game.player1Id, first, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()

            // Four Forests left: the second kicked Sproutling costs the full five.
            game.execute(CastSpell(game.player1Id, second, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldNotBe null
            game.execute(CastSpell(game.player1Id, second)).error shouldBe null
            game.resolveStack()

            game.geckoCounters() shouldBe 1
        }

        test("an unkicked spell isn't discounted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vine Gecko")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sproutling = game.findCardsInHand(1, "Murasa Sproutling").single()
            game.execute(CastSpell(game.player1Id, sproutling)).error shouldNotBe null
        }

        test("an unkicked spell cast first doesn't use up the discount") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Vine Gecko")
                .withCardInHand(1, "Murasa Sproutling")
                .withCardInHand(1, "Murasa Sproutling")
                .withLandsOnBattlefield(1, "Forest", 7)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val (unkicked, kicked) = game.findCardsInHand(1, "Murasa Sproutling")
            // Full {2}{G}: three of the seven Forests.
            game.execute(CastSpell(game.player1Id, unkicked)).error shouldBe null
            game.resolveStack()
            game.geckoCounters() shouldBe 0

            // Four Forests left still cast the first kicked spell of the turn at a discount.
            game.execute(CastSpell(game.player1Id, kicked, declaredCostSlot = ChoiceSlot.KICKED))
                .error shouldBe null
            game.resolveStack()
            game.geckoCounters() shouldBe 1
        }
    }
}
