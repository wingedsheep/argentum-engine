package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BestowPaymentTest : FunSpec({
    val helper = card("Bestow Payment Helper") {
        manaCost = "{W}"
        typeLine = "Artifact Creature — Soldier"
        power = 1
        toughness = 1
    }
    val spirit = card("Bestow Payment Spirit") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bestow("{1}{W}"))
    }
    val xSpirit = card("Bestow Payment X Spirit") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.bestow("{X}{W}"))
    }
    fun driver(keyword: Keyword, filter: GameObjectFilter = GameObjectFilter.Noncreature): GameTestDriver {
        val grant = card("Bestow Payment Grant") {
            typeLine = "Enchantment"
            staticAbility { ability = GrantKeywordToOwnSpells(keyword = keyword, spellFilter = filter) }
        }
        return GameTestDriver().also {
            it.registerCards(TestCards.all + listOf(helper, spirit, xSpirit, grant))
            it.initMirrorMatch(deck = Deck.of("Plains" to 40))
            it.passPriorityUntil(Step.PRECOMBAT_MAIN)
            it.putPermanentOnBattlefield(it.activePlayer!!, grant.name)
        }
    }

    for (keyword in listOf(Keyword.CONVOKE, Keyword.DELVE, Keyword.IMPROVISE)) {
        test("bestow offers and pays with granted $keyword using its Aura characteristics") {
            val game = driver(keyword)
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, helper.name)
            val second = game.putCreatureOnBattlefield(player, helper.name)
            val grave = game.putCardInGraveyard(player, "Plains")
            val spell = game.putCardInHand(player, spirit.name)
            if (keyword != Keyword.CONVOKE) game.giveMana(player, Color.WHITE, 1)
            val offer = game.legalActions(player).single {
                val cast = it.action as? CastSpell
                cast?.cardId == spell && cast.alternativeCostType == AlternativeCostType.BESTOW
            }
            val payment = when (keyword) {
                Keyword.CONVOKE -> {
                    offer.hasConvoke shouldBe true
                    offer.convokeCreatures!!.map { it.entityId }.toSet() shouldBe setOf(host, second)
                    AlternativePaymentChoice(convokedCreatures = mapOf(
                        host to ConvokePayment(color = Color.WHITE), second to ConvokePayment(color = null)))
                }
                Keyword.DELVE -> {
                    offer.hasDelve shouldBe true
                    offer.delveCards!!.map { it.entityId } shouldBe listOf(grave)
                    offer.minDelveNeeded shouldBe 1
                    AlternativePaymentChoice(delvedCards = listOf(grave))
                }
                else -> {
                    offer.hasTapForGeneric shouldBe true
                    offer.tapForGenericLabel shouldBe "improvise"
                    offer.tapForGenericPermanents!!.map { it.entityId }.toSet() shouldBe setOf(host, second)
                    AlternativePaymentChoice(tapForGenericPermanents = setOf(host))
                }
            }
            game.submit((offer.action as CastSpell).copy(
                targets = listOf(ChosenTarget.Permanent(host)),
                paymentStrategy = PaymentStrategy.FromPool,
                alternativePayment = payment
            )).outcome shouldBe Outcome.Done
            game.state.stack.contains(spell) shouldBe true
            if (keyword == Keyword.DELVE) game.state.getExile(player).contains(grave) shouldBe true
            else game.state.getEntity(host)!!.has<TappedComponent>() shouldBe true
        }
    }

    test("convoke also pays X in the bestow cost and raises the offered ceiling") {
        val game = driver(Keyword.CONVOKE)
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, helper.name)
        val second = game.putCreatureOnBattlefield(player, helper.name)
        val spell = game.putCardInHand(player, xSpirit.name)
        val offer = game.legalActions(player).single {
            val cast = it.action as? CastSpell
            cast?.cardId == spell && cast.alternativeCostType == AlternativeCostType.BESTOW
        }
        offer.maxAffordableX shouldBe 1
        game.submit((offer.action as CastSpell).copy(
            targets = listOf(ChosenTarget.Permanent(host)), xValue = 1,
            paymentStrategy = PaymentStrategy.FromPool,
            alternativePayment = AlternativePaymentChoice(convokedCreatures = mapOf(
                host to ConvokePayment(color = Color.WHITE), second to ConvokePayment(color = null)))
        )).outcome shouldBe Outcome.Done
    }

    test("a creature-only convoke grant does not make bestow affordable") {
        val game = driver(Keyword.CONVOKE, GameObjectFilter.Creature)
        val player = game.activePlayer!!
        game.putCreatureOnBattlefield(player, helper.name)
        game.putCreatureOnBattlefield(player, helper.name)
        val spell = game.putCardInHand(player, spirit.name)
        game.legalActions(player).any {
            val cast = it.action as? CastSpell
            cast?.cardId == spell && cast.alternativeCostType == AlternativeCostType.BESTOW
        } shouldBe false
    }
})
