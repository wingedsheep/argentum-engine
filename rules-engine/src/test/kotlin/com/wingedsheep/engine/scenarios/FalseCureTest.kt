package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.triggers.OnLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for False Cure.
 *
 * False Cure: {B}{B}
 * Instant
 * Until end of turn, whenever a player gains life, that player loses 2 life
 * for each 1 life they gained.
 */
class FalseCureTest : FunSpec({

    val HealingSalve = CardDefinition.instant(
        name = "Healing Salve",
        manaCost = ManaCost.parse("{W}"),
        oracleText = "You gain 4 life.",
        script = CardScript.spell(effect = GainLifeEffect(4))
    )

    val FalseCure = CardDefinition.instant(
        name = "False Cure",
        manaCost = ManaCost.parse("{B}{B}"),
        oracleText = "Until end of turn, whenever a player gains life, that player loses 2 life for each 1 life they gained.",
        script = CardScript.spell(
            effect = CreateGlobalTriggeredAbilityUntilEndOfTurnEffect(
                ability = TriggeredAbility.create(
                    trigger = OnLifeGain(controllerOnly = false),
                    effect = LoseLifeEffect(
                        amount = DynamicAmount.Multiply(DynamicAmount.TriggerLifeGainAmount, 2),
                        target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
                    )
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HealingSalve, FalseCure))
        return driver
    }

    test("False Cure causes life loss when a player gains life") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast False Cure
        val falseCure = driver.putCardInHand(activePlayer, "False Cure")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, falseCure).isSuccess shouldBe true
        driver.bothPass() // resolve False Cure

        driver.state.globalGrantedTriggeredAbilities.size shouldBe 1

        // Active player casts Healing Salve to gain 4 life
        val salve = driver.putCardInHand(activePlayer, "Healing Salve")
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.castSpell(activePlayer, salve).isSuccess shouldBe true
        driver.bothPass() // resolve Healing Salve — gains 4 life, trigger fires
        driver.bothPass() // resolve the triggered ability (lose 8 life)

        // Gained 4 life (20 -> 24) then lost 8 (2 * 4) from trigger = 16
        driver.assertLifeTotal(activePlayer, 16)
    }

    test("False Cure effect expires at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast False Cure
        val falseCure = driver.putCardInHand(activePlayer, "False Cure")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, falseCure)
        driver.bothPass()

        driver.state.globalGrantedTriggeredAbilities.size shouldBe 1

        // First advance past PRECOMBAT_MAIN to the next step
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        // Then advance to next turn's PRECOMBAT_MAIN
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Global ability should have been cleaned up at end of turn
        driver.state.globalGrantedTriggeredAbilities.size shouldBe 0

        // Now cast Healing Salve — False Cure should have expired
        val currentPlayer = driver.state.activePlayerId!!
        val salve = driver.putCardInHand(currentPlayer, "Healing Salve")
        driver.giveMana(currentPlayer, Color.WHITE, 1)
        driver.castSpell(currentPlayer, salve)
        driver.bothPass()

        // Should just gain 4 life with no penalty
        driver.assertLifeTotal(currentPlayer, 24)
    }

    test("False Cure with larger life gain causes proportional loss") {
        // Test with a card that gains more life to verify the multiplier
        val BigHeal = CardDefinition.instant(
            name = "Big Heal",
            manaCost = ManaCost.parse("{W}"),
            oracleText = "You gain 10 life.",
            script = CardScript.spell(effect = GainLifeEffect(10))
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BigHeal, FalseCure))
        driver.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast False Cure
        val falseCure = driver.putCardInHand(activePlayer, "False Cure")
        driver.giveMana(activePlayer, Color.BLACK, 2)
        driver.castSpell(activePlayer, falseCure)
        driver.bothPass()

        // Cast Big Heal to gain 10 life
        val heal = driver.putCardInHand(activePlayer, "Big Heal")
        driver.giveMana(activePlayer, Color.WHITE, 1)
        driver.castSpell(activePlayer, heal)
        driver.bothPass() // resolve Big Heal (gain 10 life, trigger fires)
        driver.bothPass() // resolve trigger (lose 20 life)

        // 20 + 10 - 20 = 10
        driver.assertLifeTotal(activePlayer, 10)
    }
})
