package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.NahirisWrath
import com.wingedsheep.mtg.sets.definitions.iko.cards.ChanneledForce
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A "discard X cards" cost is usually a price, paid with the cards worth least in hand. When the
 * spell deals "the total mana value of the discarded cards" (Nahiri's Wrath) the discard is the
 * fuel instead: with lands in play, one discard should be the four-drop, which kills a 3/3, not
 * the surplus land a price would be paid with — it deals 0.
 */
class DiscardManaValueFuelAiTest : FunSpec({

    val fourDrop = card("Fuel Four Drop") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(3) }
    }

    fun paidDiscard(spellName: String): List<String> {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(NahirisWrath, ChanneledForce, fourDrop))
            initMirrorMatch(Deck.of("Mountain" to 40))
        }
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, spellName)
        repeat(4) { driver.putLandOnBattlefield(player, "Mountain") }
        val fodder = listOf(driver.putCardInHand(player, "Mountain"), driver.putCardInHand(player, "Fuel Four Drop"))
        val offer = LegalAction(
            action = CastSpell(player, spell, xValue = 1),
            actionType = "CastSpell",
            description = "Cast $spellName",
            hasXCost = true,
            maxAffordableX = 2,
            additionalCostInfo = AdditionalCostData(
                description = "Discard X cards",
                costType = "DiscardCard",
                validDiscardTargets = fodder,
                discardCount = 0,
                discardMaxCount = 2,
                countIsX = true,
            ),
        )
        val paid = SelectionCostPayment.withPlainCosts(
            driver.state, offer, offer.action, IntentCatalog.of(driver.cardRegistry),
        ) as CastSpell
        return paid.additionalCostPayment!!.discardedCards.map { driver.getCardName(it)!! }
    }

    test("Nahiri's Wrath discards the highest mana value card") {
        paidDiscard("Nahiri's Wrath") shouldBe listOf("Fuel Four Drop")
    }

    test("a discard that only pays a price still takes the least valuable card — the surplus land") {
        paidDiscard("Channeled Force") shouldBe listOf("Mountain")
    }
})
