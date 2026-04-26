package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.DeepchannelDuelist
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Deepchannel Duelist: "Other Merfolk you control get +1/+1."
 * The lord bonus must NOT apply to Merfolk controlled by opponents.
 */
class DeepchannelDuelistTest : FunSpec({

    test("lord bonus only affects Merfolk you control, not opponent's Merfolk") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DeepchannelDuelist))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val duelist = driver.putCreatureOnBattlefield(activePlayer, "Deepchannel Duelist")
        val ownMerfolk = driver.putCreatureOnBattlefield(activePlayer, "Island Walker")
        val opponentMerfolk = driver.putCreatureOnBattlefield(opponent, "Island Walker")

        val projector = StateProjector()

        // Own Merfolk (Island Walker is base 2/2) gets +1/+1.
        projector.getProjectedPower(driver.state, ownMerfolk) shouldBe 3
        projector.getProjectedToughness(driver.state, ownMerfolk) shouldBe 3

        // Opponent's Merfolk does NOT get the bonus.
        projector.getProjectedPower(driver.state, opponentMerfolk) shouldBe 2
        projector.getProjectedToughness(driver.state, opponentMerfolk) shouldBe 2

        // Duelist itself is excluded by excludeSelf — base 2/2.
        projector.getProjectedPower(driver.state, duelist) shouldBe 2
        projector.getProjectedToughness(driver.state, duelist) shouldBe 2
    }
})
