package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.legalactions.support.setupP1
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.LluwenImperfectNaturalist
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests that activated abilities with an `AbilityCost.Discard` sub-cost surface
 * `AdditionalCostInfo(costType = "DiscardCard")` so the client can prompt the
 * player to pick the card to discard.
 *
 * Regression: Lluwen, Imperfect Naturalist has a {2}{B/G}{B/G}{B/G}, {T}, Discard
 * a land card ability. Before the fix, the enumerator didn't build a DiscardCard
 * cost-info entry, so the client bypassed the discard-selection UI and the
 * server rejected the activation with "No discard target chosen".
 */
class ActivatedAbilityDiscardCostTest : FunSpec({

    fun entityOnBattlefield(
        driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver,
        name: String
    ): EntityId {
        val state = driver.game.state
        return state.getBattlefield(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    fun cardIdInHand(
        driver: com.wingedsheep.engine.legalactions.support.EnumerationTestDriver,
        name: String
    ): EntityId {
        val state = driver.game.state
        return state.getHand(driver.player1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }
    }

    test("Lluwen's tap/discard-a-land ability surfaces DiscardCard cost info with only lands as valid targets") {
        val driver = setupP1(
            hand = listOf("Forest", "Grizzly Bears"),
            battlefield = listOf(
                "Lluwen, Imperfect Naturalist",
                "Swamp", "Swamp", "Swamp", "Forest", "Forest"
            ),
            extraSetCards = listOf(LluwenImperfectNaturalist)
        )
        val lluwenId = entityOnBattlefield(driver, "Lluwen, Imperfect Naturalist")
        val forestInHand = cardIdInHand(driver, "Forest")
        val bearsInHand = cardIdInHand(driver, "Grizzly Bears")

        val ability = driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(lluwenId).single()

        ability.affordable shouldBe true
        val costInfo = ability.additionalCostInfo.shouldNotBeNull()
        costInfo.costType shouldBe "DiscardCard"
        costInfo.discardCount shouldBe 1
        costInfo.validDiscardTargets shouldContain forestInHand
        costInfo.validDiscardTargets shouldNotContain bearsInHand
    }

    test("Lluwen with no land in hand emits the ability as unaffordable") {
        val driver = setupP1(
            hand = listOf("Grizzly Bears"),
            battlefield = listOf(
                "Lluwen, Imperfect Naturalist",
                "Swamp", "Swamp", "Swamp", "Forest", "Forest"
            ),
            extraSetCards = listOf(LluwenImperfectNaturalist)
        )
        val lluwenId = entityOnBattlefield(driver, "Lluwen, Imperfect Naturalist")

        val ability = driver.enumerateFor(driver.player1)
            .activatedAbilityActionsFor(lluwenId).single()

        ability.affordable shouldBe false
    }
})
