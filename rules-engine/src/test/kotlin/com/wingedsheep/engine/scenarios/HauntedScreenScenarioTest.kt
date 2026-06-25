package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.HauntedScreen
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Haunted Screen (DSK #250) — {3} Artifact mana rock with a one-shot animate ability:
 *
 * "{7}: Put seven +1/+1 counters on this artifact. It becomes a 0/0 Spirit creature in addition to
 * its other types. Activate only once."
 *
 * Exercises the [com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect] animate composed with
 * seven +1/+1 counters (so the 0/0 base reads as a 7/7), the Spirit subtype and the kept artifact
 * type ("in addition to its other types"), and the [com.wingedsheep.sdk.scripting.ActivationRestriction.Once]
 * once-ever restriction.
 */
class HauntedScreenScenarioTest : FunSpec({

    val animateAbilityId = HauntedScreen.activatedAbilities.first { !it.isManaAbility }.id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + HauntedScreen)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("{7} animates into a 7/7 Spirit artifact creature with seven +1/+1 counters") {
        val driver = newDriver()
        val player = driver.player1

        val screen = driver.putPermanentOnBattlefield(player, "Haunted Screen")
        driver.giveColorlessMana(player, 7)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = screen, abilityId = animateAbilityId)
        ).error shouldBe null
        driver.bothPass() // resolve the animate ability

        // Seven +1/+1 counters.
        driver.state.getEntity(screen)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 7

        val projected = driver.state.projectedState
        // Now a 0/0 base + seven counters = 7/7 creature.
        projected.isCreature(screen) shouldBe true
        projected.getPower(screen) shouldBe 7
        projected.getToughness(screen) shouldBe 7
        // Spirit added, artifact kept ("in addition to its other types").
        projected.hasSubtype(screen, "Spirit") shouldBe true
        projected.hasType(screen, "ARTIFACT") shouldBe true
    }

    test("animate ability can be activated only once") {
        val driver = newDriver()
        val player = driver.player1

        val screen = driver.putPermanentOnBattlefield(player, "Haunted Screen")
        driver.giveColorlessMana(player, 7)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = screen, abilityId = animateAbilityId)
        ).error shouldBe null
        driver.bothPass()

        // A second activation is rejected even with the mana available (Activate only once).
        driver.giveColorlessMana(player, 7)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = screen, abilityId = animateAbilityId)
        ).error shouldNotBe null
    }
})
