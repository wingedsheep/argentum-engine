package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cliffhaven Vampire — {2}{W}{B} 2/4 flier. "Whenever you gain life, each opponent loses 1 life."
 */
class CliffhavenVampireScenarioTest : ScenarioTestBase() {

    // A {0} sorcery that makes its controller gain 3 life — a clean way to fire one life-gain event.
    private val gainThreeLife = card("Gain Three Life") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You gain 3 life."
        spell {
            effect = Effects.GainLife(3)
        }
    }

    init {
        cardRegistry.register(gainThreeLife)

        fun board(gainer: Int) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Cliffhaven Vampire")
            .withCardInHand(gainer, "Gain Three Life")
            .withActivePlayer(gainer)
            .withPriorityPlayer(gainer)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("your life gain drains each opponent for 1") {
            val game = board(gainer = 1)
            val vampire = game.findPermanent("Cliffhaven Vampire")!!
            game.state.projectedState.hasKeyword(vampire, Keyword.FLYING) shouldBe true

            game.castSpell(1, "Gain Three Life").error shouldBe null
            game.resolveStack()

            withClue("one life-gain event is one trigger, not one per point") {
                game.getLifeTotal(1) shouldBe 23
                game.getLifeTotal(2) shouldBe 19
            }
        }

        test("an opponent's life gain doesn't trigger it") {
            val game = board(gainer = 2)

            game.castSpell(2, "Gain Three Life").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 23
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
