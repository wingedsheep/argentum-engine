package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Treasure tokens ("{T}, Sacrifice this artifact: Add one mana of any color") can be
 * spent to pay a ward cost the same way they pay any other mana cost — by being
 * sacrificed. They're filtered out of the auto-pay solver (since auto-pay can't
 * silently sacrifice permanents), but they appear in the manual mana-source list.
 */
class WardPaidWithTreasureTest : FunSpec({

    val wardedBear = card("Warded Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.ward("{2}"))
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(wardedBear, PredefinedTokens.Treasure))
        return driver
    }

    test("treasures appear in the ward mana selection list with requiresSacrifice flag") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        val treasure1 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")
        val treasure2 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Permanent(bear))
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        val sourceIds = decision.availableSources.map { it.entityId }
        sourceIds shouldContain treasure1
        sourceIds shouldContain treasure2
        decision.availableSources.filter { it.requiresSacrifice }
            .map { it.entityId }
            .toSet() shouldBe setOf(treasure1, treasure2)
    }

    test("ward {2} can be paid by manually selecting two treasures (sacrificed)") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        val treasure1 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")
        val treasure2 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Permanent(bear))
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()

        // Manually select both treasures — they should sacrifice (move to graveyard)
        // and produce the {2} needed for ward. Bolt then resolves.
        val result = driver.submitDecision(
            activePlayer,
            ManaSourcesSelectedResponse(
                decisionId = decision.id,
                selectedSources = listOf(treasure1, treasure2),
                autoPay = false
            )
        )
        result.isSuccess shouldBe true

        repeat(4) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Bolt resolved, bear took 3 damage and died.
        driver.findPermanent(opponent, "Warded Bear") shouldBe null
        // Both treasures are in the active player's graveyard.
        driver.getGraveyardCardNames(activePlayer).count { it == "Treasure" } shouldBe 2
        // Treasures are gone from the battlefield.
        driver.state.getEntity(treasure1)?.let { container ->
            val zoneOf = driver.state.zones.entries.firstOrNull { it.value.contains(treasure1) }?.key
            zoneOf?.zoneType shouldNotBe com.wingedsheep.sdk.core.Zone.BATTLEFIELD
        }
    }

    test("auto-pay refuses to silently sacrifice treasures for ward") {
        // If the caster only has treasures available (no other mana), auto-pay should
        // not silently sacrifice them — the player must opt in explicitly. canPay still
        // reports the cost as affordable (via calculateSacrificeSelfBonusMana), but
        // solve() filters sacrifice sources, so auto-pay produces no solution and the
        // caller falls back to declining.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(opponent, "Warded Bear")
        val treasure1 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")
        val treasure2 = driver.putPermanentOnBattlefield(activePlayer, "Treasure")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(
            activePlayer, bolt, listOf(ChosenTarget.Permanent(bear))
        )

        driver.bothPass()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        // autoPaySuggestion is empty — the solver won't pick sacrifice sources.
        decision.autoPaySuggestion.isEmpty() shouldBe true

        // Both treasures are still on the battlefield.
        driver.findPermanent(activePlayer, "Treasure") shouldNotBe null
        treasure2 shouldNotBe null
    }
})
