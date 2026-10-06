package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.JadeStatue
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Jade Statue (LEA #253).
 *
 * Oracle: "{2}: This artifact becomes a 3/6 Golem artifact creature until end of combat.
 * Activate only during combat."
 *
 * First card to lower `ActivationRestriction.DuringPhase(Phase.COMBAT)`, and the first animate
 * effect with `Duration.EndOfCombat` — so both the restriction and the expiry are proven here.
 */
class JadeStatueScenarioTest : FunSpec({

    val animateAbilityId = JadeStatue.activatedAbilities[0].id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(JadeStatue)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("cannot be activated outside combat") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val statue = driver.putPermanentOnBattlefield(player, "Jade Statue")
        driver.giveColorlessMana(player, 2)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = statue, abilityId = animateAbilityId)
        ).error shouldNotBe null
        projector.project(driver.state).hasType(statue, "CREATURE") shouldBe false
    }

    test("during combat it becomes a 3/6 Golem artifact creature, and reverts when combat ends") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val statue = driver.putPermanentOnBattlefield(player, "Jade Statue")

        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = statue, abilityId = animateAbilityId)
        ).error shouldBe null
        driver.bothPass()

        val animated = projector.project(driver.state)
        animated.hasType(statue, "CREATURE") shouldBe true
        animated.hasType(statue, "ARTIFACT") shouldBe true
        animated.hasSubtype(statue, "Golem") shouldBe true
        animated.getPower(statue) shouldBe 3
        animated.getToughness(statue) shouldBe 6

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        val after = projector.project(driver.state)
        after.hasType(statue, "CREATURE") shouldBe false
        after.hasType(statue, "ARTIFACT") shouldBe true
        after.hasSubtype(statue, "Golem") shouldBe false
    }
})
