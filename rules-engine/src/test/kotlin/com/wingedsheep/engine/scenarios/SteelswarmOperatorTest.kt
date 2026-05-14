package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression tests for Steelswarm Operator's two restricted mana abilities.
 *
 * Steelswarm Operator ({1}{U}, 1/1 Artifact Creature — Robot Soldier, Flying):
 *   {T}: Add {U}. Spend this mana only to cast an artifact spell.
 *   {T}: Add {U}{U}. Spend this mana only to activate abilities of artifact sources.
 *
 * Bug guarded against: when both abilities live on the same source, the auto-tap
 * solver used to collapse "different restrictions" to "unrestricted" and treat the
 * source as a 2-blue producer, leaving stray blue mana in the pool after casting an
 * artifact spell. The fix filters mana abilities by the spell payment context before
 * combining them into a single ManaSource.
 */
class SteelswarmOperatorTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )
        return driver
    }

    test("auto-tap casting an artifact spell only uses the artifact-spell-only ability, no leftover mana") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val operator = driver.putPermanentOnBattlefield(caster, "Steelswarm Operator")
        val island = driver.putLandOnBattlefield(caster, "Island")
        val relic = driver.putCardInHand(caster, "Cryogen Relic")

        val castResult = driver.castSpell(caster, relic)
        castResult.isSuccess shouldBe true

        // Both sources tapped.
        driver.state.getEntity(operator)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(island)?.has<TappedComponent>() shouldBe true

        // Mana pool should be empty — the bug left 1 leftover blue here because the
        // solver assumed Steelswarm Operator's {U}{U} ability could pay for the spell.
        val pool = driver.state.getEntity(caster)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.blue shouldBe 0
        pool.white shouldBe 0
        pool.black shouldBe 0
        pool.red shouldBe 0
        pool.green shouldBe 0
        pool.colorless shouldBe 0
        pool.restrictedMana.size shouldBe 0

        // Resolve the stack — Cryogen Relic should now be on the battlefield.
        driver.bothPass()
        driver.state.getZone(caster, Zone.BATTLEFIELD).contains(relic) shouldBe true
    }
})
