package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.MoonriseCleric
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.SunspineLynx
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.Biorhythm
import com.wingedsheep.mtg.sets.definitions.portal.cards.PathOfPeace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sunspine Lynx's "Players can't gain life" must prevent life gain for every player
 * through every path: gain-life effects, "its owner gains life" effects, and
 * set-life-total effects that would raise the total. Previously `PreventLifeGain()`
 * defaulted to `LifeGainEvent(player = Player.You)`, and several executors bypassed
 * the check entirely.
 */
class SunspineLynxPreventLifeGainTest : FunSpec({

    // Inline test spell: "Set target player's life total to 20." Used to verify
    // SetLifeTotalExecutor honors PreventLifeGain when the set would raise life.
    val SetLifeTo20 = CardDefinition.instant(
        name = "Set Life To Twenty",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "Your life total becomes 20.",
        script = CardScript.spell(
            effect = SetLifeTotalEffect(DynamicAmount.Fixed(20), EffectTarget.Controller)
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(SunspineLynx, MoonriseCleric, PathOfPeace, Biorhythm, SetLifeTo20)
        )
        return driver
    }

    test("Moonrise Cleric attack trigger gains no life while opponent controls Sunspine Lynx") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        // Opponent (p2) controls Sunspine Lynx. Active player (p1) controls Moonrise Cleric.
        driver.putPermanentOnBattlefield(p2, "Sunspine Lynx")
        val cleric = driver.putCreatureOnBattlefield(p1, "Moonrise Cleric")
        driver.removeSummoningSickness(cleric)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(p1, listOf(cleric), p2)

        // Resolve the "whenever this creature attacks, you gain 1 life" trigger.
        driver.bothPass()

        driver.getLifeTotal(p1) shouldBe 20
    }

    test("Path of Peace's 'its owner gains 4 life' does not gain life while Sunspine Lynx is out") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putPermanentOnBattlefield(p2, "Sunspine Lynx")
        val grunt = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pathId = driver.putCardInHand(p1, "Path of Peace")
        driver.giveMana(p1, Color.WHITE)
        driver.giveColorlessMana(p1, 3)
        driver.castSpell(p1, pathId, targets = listOf(grunt))
        driver.bothPass()

        // Creature is destroyed, but owner does not gain 4 life.
        driver.getLifeTotal(p1) shouldBe 20
    }

    test("SetLifeTotal to a higher value leaves life unchanged while Sunspine Lynx is out") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 5
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putPermanentOnBattlefield(p2, "Sunspine Lynx")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val spellId = driver.putCardInHand(p1, "Set Life To Twenty")
        driver.giveMana(p1, Color.WHITE)
        driver.castSpell(p1, spellId)
        driver.bothPass()

        // Per Sunspine Lynx ruling: the player's life total does not change.
        driver.getLifeTotal(p1) shouldBe 5
    }

    test("Biorhythm: player whose creature count exceeds their life does not gain life") {
        // 5 life, 10 creatures → Biorhythm would set life to 10 (a gain of 5).
        // Sunspine Lynx prevents the gain — life stays at 5.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 5
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putPermanentOnBattlefield(p2, "Sunspine Lynx")
        repeat(10) { driver.putCreatureOnBattlefield(p1, "Grizzly Bears") }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val biorhythmId = driver.putCardInHand(p1, "Biorhythm")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.giveColorlessMana(p1, 6)
        driver.castSpell(p1, biorhythmId)
        driver.bothPass()

        driver.getLifeTotal(p1) shouldBe 5
    }

    test("Biorhythm still drops a player whose life exceeds their creature count") {
        // 20 life, 2 creatures → Biorhythm sets life to 2 (an 18-point loss).
        // Sunspine Lynx only blocks gain; life loss resolves normally.
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)

        driver.putPermanentOnBattlefield(p2, "Sunspine Lynx")
        repeat(2) { driver.putCreatureOnBattlefield(p1, "Grizzly Bears") }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val biorhythmId = driver.putCardInHand(p1, "Biorhythm")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.giveColorlessMana(p1, 6)
        driver.castSpell(p1, biorhythmId)
        driver.bothPass()

        driver.getLifeTotal(p1) shouldBe 2
    }
})
