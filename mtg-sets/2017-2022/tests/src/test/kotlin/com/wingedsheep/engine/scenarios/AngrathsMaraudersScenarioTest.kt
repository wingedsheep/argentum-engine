package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.xln.cards.AngrathsMarauders
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AngrathsMaraudersScenarioTest : FunSpec({
    val ward = card("Marauders Test Ward") {
        manaCost = "{W}"
        typeLine = "Instant"
        spell {
            val recipient = target(Targets.Any)
            effect = Effects.PreventNextDamage(2, recipient)
        }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(AngrathsMarauders, ward))
        initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.bolt(player: EntityId, target: EntityId) {
        val bolt = putCardInHand(player, "Lightning Bolt")
        giveMana(player, Color.RED)
        castSpell(player, bolt, listOf(target)).error shouldBe null
        bothPass()
    }

    test("doubles its controller's spell damage to an opponent or friendly permanent") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val friendly = d.putCreatureOnBattlefield(player, "Angrath's Marauders")
        d.bolt(player, opponent)
        d.getLifeTotal(opponent) shouldBe 14

        d.bolt(player, friendly)
        d.assertInGraveyard(player, "Angrath's Marauders")
    }

    test("does not double damage from an opposing source") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        d.putCreatureOnBattlefield(opponent, "Angrath's Marauders")
        d.bolt(player, opponent)
        d.getLifeTotal(opponent) shouldBe 17
    }

    test("two Marauders multiply damage fourfold") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        repeat(2) { d.putCreatureOnBattlefield(player, "Angrath's Marauders") }
        d.bolt(player, opponent)
        d.getLifeTotal(opponent) shouldBe 8
    }

    test("Marauders doubles its own combat damage") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        val marauders = d.putCreatureOnBattlefield(player, "Angrath's Marauders")
        d.removeSummoningSickness(marauders)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(player, listOf(marauders), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent).error shouldBe null
        d.bothPass()
        if (d.pendingDecision is com.wingedsheep.engine.core.CombatResolutionDecision)
            d.confirmCombatDamage().error shouldBe null
        d.getLifeTotal(opponent) shouldBe 12
    }

    for ((first, remainingLife) in listOf("prevent" to 18, "double" to 16)) {
        test("the damaged player can $first first when prevention competes with doubling") {
            val d = driver()
            val player = d.activePlayer!!
            val opponent = d.getOpponent(player)
            d.putCreatureOnBattlefield(player, "Angrath's Marauders")
            // The caster can protect another player; the affected player still chooses the order.
            val shield = d.putCardInHand(player, "Marauders Test Ward")
            d.giveMana(player, Color.WHITE)
            d.castSpell(player, shield, listOf(opponent)).error shouldBe null
            d.bothPass()
            d.bolt(player, opponent)

            val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
            decision.playerId shouldBe opponent
            val option = decision.options.indexOfFirst { it.contains(first, ignoreCase = true) }
            (option >= 0) shouldBe true
            d.submitDecision(opponent, OptionChosenResponse(decision.id, option)).error shouldBe null
            d.getLifeTotal(opponent) shouldBe remainingLife
            d.pendingDecision shouldBe null
        }
    }
})
