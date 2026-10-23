package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.HallOfStormGiants
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hall of Storm Giants (AFR #257) — Land.
 *
 * "If you control two or more other lands, this land enters tapped. {T}: Add {U}.
 *  {5}{U}: Until end of turn, this land becomes a 7/7 blue Giant creature with ward {3}.
 *  It's still a land."
 *
 * Pins the inverted enters-tapped condition both ways and the animation's ward, which comes from a
 * runtime-granted `GrantWard` — the opponent's spell is countered unless they pay {3}, and the ward
 * is gone with the animation the next turn.
 */
class HallOfStormGiantsScenarioTest : FunSpec({
    fun createDriver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.registerCard(HallOfStormGiants)
        it.initMirrorMatch(deck = Deck.of("Island" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.animate(player: EntityId, hall: EntityId) {
        giveMana(player, Color.BLUE, 6)
        submitSuccess(ActivateAbility(player, hall, HallOfStormGiants.activatedAbilities[1].id))
        bothPass()
    }

    fun GameTestDriver.boltFrom(caster: EntityId, target: EntityId) {
        giveMana(caster, Color.RED, 1)
        castSpellWithTargets(caster, putCardInHand(caster, "Lightning Bolt"), listOf(ChosenTarget.Permanent(target)))
        bothPass()
    }

    test("enters untapped with only one other land") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putLandOnBattlefield(player, "Island")
        val hall = driver.putCardInHand(player, "Hall of Storm Giants")
        driver.submitSuccess(PlayLand(player, hall))
        driver.isTapped(hall) shouldBe false
    }

    test("enters tapped with two or more other lands") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        repeat(2) { driver.putLandOnBattlefield(player, "Island") }
        val hall = driver.putCardInHand(player, "Hall of Storm Giants")
        driver.submitSuccess(PlayLand(player, hall))
        driver.isTapped(hall) shouldBe true
    }

    test("animated, it is a 7/7 blue Giant land with ward {3} that counters an unpaid opponent's spell") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val hall = driver.putLandOnBattlefield(player, "Hall of Storm Giants")

        driver.animate(player, hall)
        val projected = driver.state.projectedState
        projected.hasType(hall, "LAND") shouldBe true
        projected.hasType(hall, "CREATURE") shouldBe true
        projected.hasSubtype(hall, "Giant") shouldBe true
        projected.getPower(hall) shouldBe 7
        projected.getToughness(hall) shouldBe 7
        projected.getColors(hall) shouldBe setOf("BLUE")
        projected.hasKeyword(hall, Keyword.WARD) shouldBe true

        driver.passPriority(player)
        // With {3} open the opponent is asked whether to pay; they decline.
        repeat(3) { driver.putLandOnBattlefield(opponent, "Island") }
        driver.boltFrom(opponent, hall)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        decision.playerId shouldBe opponent
        driver.submitManaAutoPayOrDecline(opponent, autoPay = false)
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
        driver.findPermanent(player, "Hall of Storm Giants") shouldNotBe null
        driver.state.getEntity(hall)?.get<DamageComponent>()?.amount shouldBe null
    }

    test("the ward ends with the animation") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val hall = driver.putLandOnBattlefield(player, "Hall of Storm Giants")

        driver.animate(player, hall)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)

        val projected = driver.state.projectedState
        projected.hasType(hall, "CREATURE") shouldBe false
        projected.hasKeyword(hall, Keyword.WARD) shouldBe false
    }
})
