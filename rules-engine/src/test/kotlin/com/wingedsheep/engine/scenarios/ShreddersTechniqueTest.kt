package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shredder's Technique (TMT #77) — Sorcery, Sneak {B}. "Destroy target creature
 * or enchantment. If an enchantment was destroyed this way, you lose 2 life."
 */
class ShreddersTechniqueTest : FunSpec({
    test("destroying a creature loses no life") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Shredder's Technique")
        driver.giveMana(player, Color.BLACK, 3)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSpellWithTargets(player, spell, listOf(ChosenTarget.Permanent(victim))).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        (driver.findPermanent(opponent, "Grizzly Bears") == null) shouldBe true
        driver.assertLifeTotal(player, 20) // creature, not enchantment → no life loss
    }
})
