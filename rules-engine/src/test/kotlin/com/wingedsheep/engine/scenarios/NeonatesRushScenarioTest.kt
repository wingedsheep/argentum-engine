package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.InnistradMidnightHuntSet
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Neonate's Rush — {2}{R} Instant
 * This spell costs {1} less to cast if you control a Vampire.
 * Neonate's Rush deals 1 damage to target creature and 1 damage to its controller. Draw a card.
 */
class NeonatesRushScenarioTest : FunSpec({

    // A minimal Vampire to switch the cost-reduction static on.
    val testVampire = card("Test Vampire") {
        manaCost = "{B}"
        colorIdentity = "B"
        typeLine = "Creature — Vampire"
        power = 1
        toughness = 1
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + InnistradMidnightHuntSet.cards + testVampire)
        return d
    }

    test("deals 1 to a creature and 1 to its controller, then draws a card") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(p2, "Savannah Lions") // 1/1

        val rush = d.putCardInHand(p1, "Neonate's Rush")
        val handBefore = d.getHand(p1).size
        d.giveMana(p1, Color.RED, 3) // no Vampire -> full {2}{R}
        d.castSpell(p1, rush, targets = listOf(victim)).isSuccess shouldBe true
        d.bothPass()

        // 1 damage kills the 1/1, and its controller takes 1.
        d.getGraveyard(p2) shouldContain victim
        d.getLifeTotal(p2) shouldBe 19
        // Cast one card (-1) and drew one (+1) -> hand size unchanged.
        d.getHand(p1).size shouldBe handBefore
    }

    test("costs {1} less while you control a Vampire") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, "Test Vampire")
        val victim = d.putCreatureOnBattlefield(p2, "Savannah Lions")

        val rush = d.putCardInHand(p1, "Neonate's Rush")
        // Only {1}{R} available — enough only if the Vampire discount applied.
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 1)
        d.castSpell(p1, rush, targets = listOf(victim)).isSuccess shouldBe true
        d.bothPass()

        // The reduced cast still resolved: victim died and its controller took 1.
        d.getGraveyard(p2) shouldContain victim
        d.getLifeTotal(p2) shouldBe 19
    }
})
