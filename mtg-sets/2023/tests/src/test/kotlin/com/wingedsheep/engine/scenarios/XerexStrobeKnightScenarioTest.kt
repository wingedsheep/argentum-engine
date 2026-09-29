package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.XerexStrobeKnight
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Xerex Strobe-Knight (MOM #85):
 * "{T}: Create a 2/2 white and blue Knight creature token with vigilance. Activate only if
 * you've cast two or more spells this turn."
 */
class XerexStrobeKnightScenarioTest : FunSpec({

    val trinket = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(trinket, XerexStrobeKnight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        return driver
    }

    val abilityId = XerexStrobeKnight.activatedAbilities.single().id

    fun GameTestDriver.knightTokens(playerId: EntityId): List<EntityId> =
        state.getBattlefield().filter { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == "Knight Token" && getController(id) == playerId
        }

    fun GameTestDriver.castTrinket(playerId: EntityId) {
        val t = putCardInHand(playerId, "Test Trinket")
        giveColorlessMana(playerId, 1)
        withClue("casting Test Trinket should succeed") { castSpell(playerId, t).error shouldBe null }
        bothPass()
    }

    test("activation is rejected with fewer than two spells cast, allowed after the second") {
        val driver = setup()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val knight = driver.putCreatureOnBattlefield(p1, "Xerex Strobe-Knight")
        driver.removeSummoningSickness(knight)

        // Zero spells cast.
        driver.submit(ActivateAbility(playerId = p1, sourceId = knight, abilityId = abilityId))
            .outcome shouldNotBe Outcome.Done
        driver.knightTokens(p1).size shouldBe 0

        // One spell cast.
        driver.castTrinket(p1)
        driver.submit(ActivateAbility(playerId = p1, sourceId = knight, abilityId = abilityId))
            .outcome shouldNotBe Outcome.Done
        driver.knightTokens(p1).size shouldBe 0

        // Two spells cast.
        driver.castTrinket(p1)
        val result = driver.submit(ActivateAbility(playerId = p1, sourceId = knight, abilityId = abilityId))
        withClue("activation after two spells should succeed") { result.error shouldBe null }
        driver.bothPass()

        driver.state.getEntity(knight)?.has<TappedComponent>() shouldBe true
        val tokens = driver.knightTokens(p1)
        tokens.size shouldBe 1
        val token = tokens.single()
        driver.state.getEntity(token)!!.get<CardComponent>()!!.colors shouldBe setOf(Color.WHITE, Color.BLUE)
        val projected = driver.state.projectedState
        projected.getPower(token) shouldBe 2
        projected.getToughness(token) shouldBe 2
        projected.getKeywords(token) shouldContain Keyword.VIGILANCE.name
    }
})
