package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.WylieDukeAtiinHero
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Wylie Duke, Atiin Hero (OTJ) — {1}{G}{W} Legendary Human Ranger 4/2.
 *
 *  - Vigilance.
 *  - Whenever Wylie Duke becomes tapped, you gain 1 life and draw a card.
 */
class WylieDukeAtiinHeroScenarioTest : FunSpec({

    // Minimal test instant that taps target creature, so we can make Wylie Duke "become tapped"
    // by something other than attacking (he has vigilance, so attacking never taps him).
    val TapThatThing = card("Tap That Thing") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        oracleText = "Tap target creature."
        spell {
            target = Targets.Creature
            effect = Effects.Tap(EffectTarget.ContextTarget(0))
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + WylieDukeAtiinHero + TapThatThing)
        return driver
    }

    test("becoming tapped gains 1 life and draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wylie = driver.putCreatureOnBattlefield(player, "Wylie Duke, Atiin Hero")

        val lifeBefore = driver.getLifeTotal(player)

        // Tap Wylie Duke with a spell → "becomes tapped" trigger fires.
        val tapSpell = driver.putCardInHand(player, "Tap That Thing")
        driver.giveMana(player, Color.BLUE, 1)
        // Hand right before casting (includes the tap spell we just added).
        val handBeforeCast = driver.getHandSize(player)
        driver.castSpell(player, tapSpell, listOf(wylie))
        driver.bothPass() // resolve the tap spell
        driver.bothPass() // resolve the BecomesTapped trigger (gain life + draw)

        driver.isTapped(wylie) shouldBe true
        driver.getLifeTotal(player) shouldBe lifeBefore + 1
        // Cast the tap spell (-1), then the trigger drew a card (+1): net unchanged from before cast.
        driver.getHandSize(player) shouldBe handBeforeCast
    }

    test("Wylie Duke has vigilance — attacking does not tap him, so the trigger does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wylie = driver.putCreatureOnBattlefield(player, "Wylie Duke, Atiin Hero")
        driver.removeSummoningSickness(wylie)

        val lifeBefore = driver.getLifeTotal(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(wylie), opponent)
        driver.bothPass()

        // Vigilance: he stays untapped, no life gained.
        driver.isTapped(wylie) shouldBe false
        driver.getLifeTotal(player) shouldBe lifeBefore
    }
})
