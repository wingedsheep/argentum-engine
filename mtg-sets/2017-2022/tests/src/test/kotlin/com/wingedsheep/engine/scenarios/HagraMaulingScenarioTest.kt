package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.znr.ZendikarRisingSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hagra Mauling (ZNR #106, {2}{B}{B} Instant // Hagra Broodpit).
 *
 *   This spell costs {1} less to cast if an opponent controls no basic lands.
 *   Destroy target creature.
 *
 * Pins the cost gate: it reads the *opponent's* basic lands (the caster's own don't matter), so
 * {1}{B}{B} is enough only while the opponent controls no basic land.
 */
class HagraMaulingScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ZendikarRisingSet.cards)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("costs {1} less when the opponent controls no basic lands, even if you control some") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.tapPermanent(d.putLandOnBattlefield(p1, "Swamp"))
        val victim = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val mauling = d.putCardInHand(p1, "Hagra Mauling")
        d.giveMana(p1, Color.BLACK, 2)
        d.giveColorlessMana(p1, 1)
        d.castSpell(p1, mauling, targets = listOf(victim)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.getGraveyard(p2) shouldContain victim
    }

    test("full cost while the opponent controls a basic land") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.putLandOnBattlefield(p2, "Swamp")
        val victim = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        val mauling = d.putCardInHand(p1, "Hagra Mauling")
        d.giveMana(p1, Color.BLACK, 2)
        d.giveColorlessMana(p1, 1)
        d.castSpell(p1, mauling, targets = listOf(victim)).outcome shouldNotBe Outcome.Done
    }
})
