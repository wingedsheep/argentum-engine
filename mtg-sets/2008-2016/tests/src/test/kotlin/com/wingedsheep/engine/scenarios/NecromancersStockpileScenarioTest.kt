package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m15.cards.NecromancersStockpile
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Necromancer's Stockpile (M15 #108) — "{1}{B}, Discard a creature card: Draw a card. If the
 * discarded card was a Zombie card, create a tapped 2/2 black Zombie creature token."
 *
 * The discard is an activation cost, so the Zombie check at resolution has to read the card the
 * activation recorded — it's in the graveyard by then.
 */
class NecromancersStockpileScenarioTest : FunSpec({

    val abilityId = NecromancersStockpile.activatedAbilities.first().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + NecromancersStockpile)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.activateDiscarding(discardName: String): EntityId {
        val me = player1
        val stockpile = putPermanentOnBattlefield(me, "Necromancer's Stockpile")
        giveMana(me, Color.BLACK, 2)
        val discarded = putCardInHand(me, discardName)
        val result = submit(
            ActivateAbility(
                playerId = me,
                sourceId = stockpile,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(discarded)),
            )
        )
        withClue(result.error ?: "activation failed") { result.outcome shouldBe Outcome.Done }
        withClue("the discard is paid on activation") { getGraveyard(me).contains(discarded) shouldBe true }
        return discarded
    }

    test("discarding a Zombie creature card draws a card and creates a tapped 2/2 black Zombie") {
        val d = driver()
        d.activateDiscarding("Gurmag Angler")
        val handBefore = d.getHandSize(d.player1)
        d.bothPass()

        d.getHandSize(d.player1) shouldBe handBefore + 1
        val token = d.findPermanent(d.player1, "Zombie Token")
        token shouldNotBe null
        d.isTapped(token!!) shouldBe true
    }

    test("discarding a non-Zombie creature card only draws a card") {
        val d = driver()
        d.activateDiscarding("Savannah Lions")
        val handBefore = d.getHandSize(d.player1)
        d.bothPass()

        d.getHandSize(d.player1) shouldBe handBefore + 1
        d.findPermanent(d.player1, "Zombie Token") shouldBe null
    }

    test("a non-creature card can't pay the discard cost") {
        val d = driver()
        val stockpile = d.putPermanentOnBattlefield(d.player1, "Necromancer's Stockpile")
        d.giveMana(d.player1, Color.BLACK, 2)
        val land = d.putCardInHand(d.player1, "Swamp")
        val result = d.submit(
            ActivateAbility(
                playerId = d.player1,
                sourceId = stockpile,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(land)),
            )
        )
        result.outcome shouldNotBe Outcome.Done
        d.getHand(d.player1).contains(land) shouldBe true
    }
})
