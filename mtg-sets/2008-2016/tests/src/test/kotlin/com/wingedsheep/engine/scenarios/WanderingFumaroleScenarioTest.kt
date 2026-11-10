package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ogw.cards.WanderingFumarole
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wandering Fumarole: the animation grants the land its own "{0}: switch" ability, and the switch
 * applies in its own P/T sublayer after the animation's set-P/T — so re-animating the land later
 * in the turn (a newer set-P/T effect) does not undo an earlier switch.
 */
class WanderingFumaroleScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(WanderingFumarole))
        initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.animate(land: EntityId) {
        giveMana(player1, Color.BLUE, 1)
        giveMana(player1, Color.RED, 3)
        submit(ActivateAbility(player1, land, WanderingFumarole.activatedAbilities[2].id)).error shouldBe null
        bothPass().error shouldBe null
    }

    fun GameTestDriver.switchId(land: EntityId): AbilityId? =
        state.grantedActivatedAbilities.firstOrNull { it.entityId == land }?.ability?.id

    fun GameTestDriver.switch(land: EntityId) {
        val id = switchId(land)
        id shouldNotBe null
        submit(ActivateAbility(player1, land, id!!)).error shouldBe null
        bothPass().error shouldBe null
    }

    fun GameTestDriver.stats(land: EntityId, power: Int, toughness: Int) {
        state.projectedState.getPower(land) shouldBe power
        state.projectedState.getToughness(land) shouldBe toughness
    }

    test("becomes a 1/4 blue and red Elemental land creature that can switch to 4/1") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player1, "Wandering Fumarole")
        d.switchId(land) shouldBe null

        d.animate(land)
        val projected = d.state.projectedState
        projected.isCreature(land) shouldBe true
        projected.hasType(land, "LAND") shouldBe true
        projected.hasSubtype(land, "Elemental") shouldBe true
        projected.getColors(land) shouldBe setOf("BLUE", "RED")
        d.stats(land, 1, 4)

        d.switch(land)
        d.stats(land, 4, 1)
    }

    test("a later re-animation does not undo an earlier switch") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player1, "Wandering Fumarole")
        d.animate(land)
        d.switch(land)
        d.animate(land)
        d.stats(land, 4, 1)
    }

    test("animation, switch and the granted ability all end with the turn") {
        val d = driver()
        val land = d.putLandOnBattlefield(d.player1, "Wandering Fumarole")
        d.animate(land)
        d.switch(land)
        d.passPriorityUntil(Step.UPKEEP)
        d.state.projectedState.isCreature(land) shouldBe false
        d.switchId(land) shouldBe null
    }
})
