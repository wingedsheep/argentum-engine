package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.BurnAtTheStake
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

/**
 * "Tap any number of untapped creatures you control" is usually a price, paid at its floor (none).
 * When the spell deals "three times the number of creatures tapped this way" (Burn at the Stake)
 * the taps are the fuel instead: every untapped creature should be turned sideways, or the spell
 * deals 0.
 */
class TapVariableFuelAiTest : FunSpec({

    val tapForNothing = card("Test Idle Muster") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.TapPermanentsVariable())
        spell { effect = Effects.DrawCards(1) }
    }

    fun paidTaps(spellName: String): Pair<List<com.wingedsheep.sdk.model.EntityId>, List<com.wingedsheep.sdk.model.EntityId>> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(BurnAtTheStake, tapForNothing))
            initMirrorMatch(Deck.of("Mountain" to 40))
        }
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, spellName)
        val creatures = List(3) { driver.putCreatureOnBattlefield(player, "Grizzly Bears") }
        val offer = LegalAction(
            action = CastSpell(player, spell),
            actionType = "CastSpell",
            description = "Cast $spellName",
            additionalCostInfo = AdditionalCostData(
                description = "Tap any number of untapped creatures you control",
                costType = "TapVariable",
                validTapTargets = creatures,
                tapCount = 0,
            ),
        )
        val paid = SelectionCostPayment.withPlainCosts(
            driver.state, offer, offer.action, IntentCatalog.of(driver.cardRegistry),
        ) as CastSpell
        return creatures to paid.additionalCostPayment!!.variableCostPermanents!!
    }

    test("Burn at the Stake taps every untapped creature") {
        val (creatures, tapped) = paidTaps("Burn at the Stake")
        tapped shouldContainExactlyInAnyOrder creatures
    }

    test("a tap cost that only pays a price taps nothing") {
        paidTaps("Test Idle Muster").second.shouldBeEmpty()
    }
})
