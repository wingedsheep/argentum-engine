package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.ReaperOfFlightMoonsilver
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Reaper of Flight Moonsilver (SOI #36) — {3}{W}{W} 3/3 Angel with flying.
 *
 *   Delirium — Sacrifice another creature: This creature gets +2/+1 until end of turn. Activate
 *   only if there are four or more card types among cards in your graveyard.
 *
 * Pins the activation restriction: with three card types in the graveyard the ability can't be
 * activated at all; with four it can, the other creature is sacrificed and the Reaper is 5/4.
 */
class ReaperOfFlightMoonsilverScenarioTest : FunSpec({

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to player
    }

    fun activate(player: EntityId, reaper: EntityId, fodder: EntityId) =
        ActivateAbility(
            playerId = player,
            sourceId = reaper,
            abilityId = ReaperOfFlightMoonsilver.activatedAbilities.single().id,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
        )

    test("without delirium the ability can't be activated") {
        val (driver, player) = newGame()
        val reaper = driver.putCreatureOnBattlefield(player, "Reaper of Flight Moonsilver")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery

        driver.submitExpectFailure(activate(player, reaper, bears))

        driver.findPermanent(player, "Grizzly Bears").shouldNotBeNull()
        driver.state.projectedState.getPower(reaper) shouldBe 3
        driver.state.projectedState.getToughness(reaper) shouldBe 3
    }

    test("with delirium, sacrificing another creature gives it +2/+1") {
        val (driver, player) = newGame()
        val reaper = driver.putCreatureOnBattlefield(player, "Reaper of Flight Moonsilver")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact

        driver.submitSuccess(activate(player, reaper, bears))
        driver.getGraveyard(player) shouldContain bears
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(reaper) shouldBe 5
        driver.state.projectedState.getToughness(reaper) shouldBe 4
    }
})
