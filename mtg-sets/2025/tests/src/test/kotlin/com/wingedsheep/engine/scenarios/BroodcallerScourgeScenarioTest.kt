package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdc.cards.BroodcallerScourge
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Broodcaller Scourge (TDC #44) — {5}{G}{G} Creature — Dragon, 5/7.
 *
 *   Flying
 *   Whenever one or more Dragons you control deal combat damage to a player, you may put a
 *   permanent card with mana value less than or equal to that damage from your hand onto the
 *   battlefield.
 *
 * "That damage" is everything the Dragons dealt that player in the step, so two Scourges hitting
 * the same player make 10 available to each trigger.
 */
class BroodcallerScourgeScenarioTest : FunSpec({

    fun driver(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(BroodcallerScourge))
        val ids = d.initMultiplayer(List(2) { Deck.of("Grizzly Bears" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe ids[0]
        return d to ids
    }

    /** Walks combat to postcombat main; the first card prompt picks [pick], later ones decline. */
    fun GameTestDriver.finishCombat(pick: EntityId?): List<List<EntityId>> {
        val offered = mutableListOf<List<EntityId>>()
        var guard = 0
        while (state.step != Step.POSTCOMBAT_MAIN && guard++ < 60) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision -> {
                    offered += decision.options
                    val choice = if (offered.size == 1 && pick != null) listOf(pick) else emptyList()
                    submitCardSelection(decision.playerId, choice).error shouldBe null
                }
                null -> bothPass()
                else -> autoResolveDecision()
            }
        }
        return offered
    }

    test("offers only permanent cards with mana value up to the damage dealt") {
        val (d, p) = driver()
        val scourge = d.putCreatureOnBattlefield(p[0], "Broodcaller Scourge").also { d.removeSummoningSickness(it) }
        val force = d.putCardInHand(p[0], "Force of Nature") // MV 5
        d.putCardInHand(p[0], "Gurmag Angler") // MV 7
        d.putCardInHand(p[0], "Lightning Bolt") // not a permanent

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(scourge to p[1])).error shouldBe null
        val offered = d.finishCombat(pick = force)

        offered.single().filter { d.getCardName(it) != "Grizzly Bears" } shouldContainExactlyInAnyOrder listOf(force)
        d.findPermanent(p[0], "Force of Nature") shouldNotBe null
    }

    test("that damage sums every Dragon that hit the player") {
        val (d, p) = driver()
        val first = d.putCreatureOnBattlefield(p[0], "Broodcaller Scourge").also { d.removeSummoningSickness(it) }
        val second = d.putCreatureOnBattlefield(p[0], "Broodcaller Scourge").also { d.removeSummoningSickness(it) }
        val angler = d.putCardInHand(p[0], "Gurmag Angler") // MV 7 > 5, but ≤ 10

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(first to p[1], second to p[1])).error shouldBe null
        val offered = d.finishCombat(pick = angler)

        (angler in offered.first()) shouldBe true
        d.findPermanent(p[0], "Gurmag Angler") shouldNotBe null
        d.getLifeTotal(p[1]) shouldBe 10
    }
})
