package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * With Great Power . . . (SPM) — Aura: enchanted creature gets +2/+2 per Aura/Equipment attached to
 * it, and all damage that would be dealt to you is dealt to the enchanted creature instead.
 *
 * Pins the Pariah-style static `RedirectDamage` to `EffectTarget.EnchantedCreature` (now resolved
 * by `DamageUtils.resolveRedirectTarget`) and the `attachmentsOnEnchantedCreature()` dynamic buff.
 */
class WithGreatPowerScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != you }
        return Triple(driver, you, opponent)
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("buffs the enchanted creature (+2/+2 per attachment) and redirects damage from you to it") {
        val (driver, you, opponent) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        // Cast With Great Power . . . on Centaur Courser.
        driver.giveMana(you, Color.WHITE, 4)
        val wgp = driver.putCardInHand(you, "With Great Power . . .")
        driver.castSpellWithTargets(you, wgp, listOf(ChosenTarget.Permanent(courser)))
        driver.bothPass()
        resolveStack(driver)

        // One attachment (the Aura itself) → +2/+2 → 5/5.
        driver.state.projectedState.getPower(courser) shouldBe 5
        driver.state.projectedState.getToughness(courser) shouldBe 5

        // Damage that would be dealt to you is dealt to the enchanted creature instead. You have
        // priority in your main phase; bolt yourself — the redirect applies to any damage to you.
        val before = driver.getLifeTotal(you)
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(you)))
        driver.bothPass()
        resolveStack(driver)

        driver.getLifeTotal(you) shouldBe before // unchanged — redirected
        (driver.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 3
    }

    test("the +2/+2 scales per attachment: two attachments on the creature grant +4/+4 each") {
        val (driver, you, _) = newGame()
        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3

        // Enchant Centaur Courser with two copies of With Great Power . . . ({3}{W} each). Nothing
        // stops two of the same Aura on one creature, and it gives a clean two-attachment board
        // without dragging in Equipment stat bonuses.
        driver.giveMana(you, Color.WHITE, 8)
        repeat(2) {
            val wgp = driver.putCardInHand(you, "With Great Power . . .")
            driver.castSpellWithTargets(you, wgp, listOf(ChosenTarget.Permanent(courser)))
            driver.bothPass()
            resolveStack(driver)
        }

        // Two Auras attached → each grants +2/+2 *per attachment* = +4/+4 → +8/+8 total → 11/11.
        // A flat "+2/+2" (ignoring the count) would give only 7/7, so this pins the scaling.
        driver.state.projectedState.getPower(courser) shouldBe 11
        driver.state.projectedState.getToughness(courser) shouldBe 11
    }
})
