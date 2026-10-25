package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fut.cards.DryadArbor
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Dryad Arbor (FUT #174) — Land Creature — Forest Dryad, 1/1.
 *
 *   "(This land isn't a spell, it's affected by summoning sickness, and it has
 *    '{T}: Add {G}.')"
 *
 * Two pieces of behaviour to pin down:
 *  - It's a green 1/1 creature (from its color indicator, since it has no mana cost) as well as
 *    a land, the moment it's played.
 *  - Unlike an ordinary land, its mana ability IS gated by summoning sickness (CR 302.6, because
 *    it's also a creature) — this is the one place the engine's land/creature carve-out for
 *    tap-ability summoning sickness had to be corrected for a land that's also a creature.
 */
class DryadArborScenarioTest : FunSpec({

    val projector = StateProjector()
    val manaAbilityId = DryadArbor.activatedAbilities[0].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DryadArbor)
        return driver
    }

    test("playing it puts a green 1/1 land creature onto the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val arbor = driver.putCardInHand(me, "Dryad Arbor")

        driver.playLand(me, arbor).error shouldBe null
        driver.findPermanent(me, "Dryad Arbor") shouldBe arbor

        val projected = projector.project(driver.state)
        projected.getPower(arbor) shouldBe 1
        projected.getToughness(arbor) shouldBe 1
        projected.getColors(arbor) shouldBe setOf("GREEN")
    }

    test("its mana ability is blocked by summoning sickness the turn it's played") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val arbor = driver.putCardInHand(me, "Dryad Arbor")
        driver.playLand(me, arbor)

        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = arbor, abilityId = manaAbilityId)
        )
        result.error shouldBe "This creature has summoning sickness"
        driver.isTapped(arbor) shouldBe false
    }

    test("once summoning sickness clears, {T}: Add {G} works") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val arbor = driver.putCardInHand(me, "Dryad Arbor")
        driver.playLand(me, arbor)
        driver.removeSummoningSickness(arbor)

        val result = driver.submit(
            ActivateAbility(playerId = me, sourceId = arbor, abilityId = manaAbilityId)
        )
        result.outcome shouldBe Outcome.Done
        driver.isTapped(arbor) shouldBe true
        driver.state.getEntity(me)?.get<ManaPoolComponent>()?.green shouldBe 1
    }

    test("auto-pay won't tap it for a spell while it's summoning sick") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val arbor = driver.putCardInHand(me, "Dryad Arbor")
        driver.playLand(me, arbor)
        driver.putCreatureOnBattlefield(me, "Grizzly Bears").also(driver::removeSummoningSickness)
        val growth = driver.putCardInHand(me, "Giant Growth")

        driver.castSpell(me, growth).error shouldNotBe null
        driver.isTapped(arbor) shouldBe false
    }
})
