package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dominaria.cards.LichsMastery
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Lich's Mastery.
 */
class LichsMasteryTest : FunSpec({

    val HealingSalve = CardDefinition.instant(
        name = "Healing Salve",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "You gain 3 life.",
        script = CardScript.spell(effect = GainLifeEffect(3))
    )

    val SelfLoseLife2 = CardDefinition.instant(
        name = "Self Lose Life 2",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "You lose 2 life.",
        script = CardScript.spell(effect = LoseLifeEffect(2, EffectTarget.Controller))
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LichsMastery, HealingSalve, SelfLoseLife2))
        return driver
    }

    test("life gain draws that many cards") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Lich's Mastery")
        val handSizeBefore = driver.getHandSize(p1)

        // Cast Healing Salve (gain 3 life)
        val salve = driver.putCardInHand(p1, "Healing Salve")
        driver.giveMana(p1, Color.WHITE, 1)
        driver.castSpell(p1, salve)
        driver.bothPass() // resolve Healing Salve
        driver.bothPass() // resolve draw trigger

        driver.getHandSize(p1) shouldBe handSizeBefore + 3
        driver.assertLifeTotal(p1, 23)
    }

    test("life loss forces exile choices") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(p1, "Lich's Mastery")
        val bear1 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(p1, "Grizzly Bears")

        // Cast Self Lose Life (you lose 2 life)
        val loseLife = driver.putCardInHand(p1, "Self Lose Life 2")
        driver.giveMana(p1, Color.BLACK, 1)
        driver.castSpell(p1, loseLife)
        driver.bothPass() // resolve Self Lose Life - trigger goes on stack
        driver.bothPass() // resolve triggered ability - ForceExileMultiZone

        // Lich's Mastery trigger: exile 2 things - decision should be pending
        driver.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()

        // Select both bears to exile
        driver.submitCardSelection(p1, listOf(bear1, bear2))

        driver.assertLifeTotal(p1, 18)
        // Bears should no longer be on the battlefield
        val creatures = driver.getCreatures(p1)
        creatures.none {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Grizzly Bears"
        } shouldBe true
    }
})
