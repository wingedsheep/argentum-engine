package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.UrzasFactory
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Urza's Factory (TSP #280) — Land — Urza's
 * "{T}: Add {C}.
 *  {7}, {T}: Create a 2/2 colorless Assembly-Worker artifact creature token."
 */
class UrzasFactoryScenarioTest : FunSpec({

    val tokenAbilityId = UrzasFactory.activatedAbilities[1].id

    test("{7}, {T}: creates a 2/2 colorless Assembly-Worker artifact creature token") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(UrzasFactory)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val factory = driver.putPermanentOnBattlefield(me, "Urza's Factory")
        driver.giveColorlessMana(me, 7)

        val before = creatureTokens(driver.state, me).toSet()
        driver.submit(
            ActivateAbility(playerId = me, sourceId = factory, abilityId = tokenAbilityId)
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        val newTokens = creatureTokens(driver.state, me) - before
        newTokens.size shouldBe 1
        val token = newTokens.single()
        val card = driver.state.getEntity(token)!!.get<CardComponent>()!!
        card.typeLine.isArtifact shouldBe true
        card.typeLine.isCreature shouldBe true
        card.typeLine.subtypes.map { it.value } shouldBe listOf("Assembly-Worker")
        card.colors shouldBe emptySet()
        driver.state.projectedState.getPower(token) shouldBe 2
        driver.state.projectedState.getToughness(token) shouldBe 2
        driver.isTapped(factory) shouldBe true
    }

    test("the factory's type line carries the Urza's land subtype") {
        val typeLine = UrzasFactory.typeLine
        typeLine.isLand shouldBe true
        typeLine.subtypes.map { it.value } shouldBe listOf("Urza's")
    }
})

private fun creatureTokens(state: GameState, player: EntityId): List<EntityId> =
    state.getBattlefield().filter {
        val e = state.getEntity(it) ?: return@filter false
        e.has<TokenComponent>() &&
            e.get<ControllerComponent>()?.playerId == player &&
            e.get<CardComponent>()?.typeLine?.isCreature == true
    }
