package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.DennickPiousApprentice
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dennick, Pious Apprentice // Dennick, Pious Apparition (MID #217).
 *
 * Front: lifelink; "Cards in graveyards can't be the targets of spells or abilities"; disturb.
 * Back: flying; "Whenever one or more creature cards are put into graveyards from anywhere,
 * investigate. This ability triggers only once each turn."; exiled instead of going to a graveyard.
 */
class DennickPiousApprenticeScenarioTest : FunSpec({

    val raise = card("Test Dennick Raise") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.ReturnToHandFromGraveyard(t)
        }
    }
    val murder = card("Test Dennick Murder") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(DennickPiousApprentice, raise, murder))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.clues(player: EntityId) =
        state.getBattlefield().count {
            state.getEntity(it)?.get<CardComponent>()?.name == "Clue" && state.projectedState.getController(it) == player
        }

    fun GameTestDriver.kill(player: EntityId, creature: EntityId) {
        val spell = putCardInHand(player, murder.name)
        giveColorlessMana(player, 1)
        castSpell(player, spell, listOf(creature)).error shouldBe null
        while (state.stack.isNotEmpty()) bothPass()
    }

    test("front face: an opponent's Dennick stops your spell targeting a card in your graveyard") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Dennick, Pious Apprentice")
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        val spell = d.putCardInHand(d.player1, raise.name)
        d.giveColorlessMana(d.player1, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD)))
            .error shouldNotBe null
        d.getGraveyard(d.player1).contains(bear) shouldBe true
    }

    test("disturb still casts it from the graveyard — casting doesn't target") {
        val d = driver()
        // An opponent's Dennick locks every graveyard; a disturb cast isn't a targeting.
        d.putCreatureOnBattlefield(d.player2, "Dennick, Pious Apprentice")
        val dennick = d.putCardInGraveyard(d.player1, "Dennick, Pious Apprentice")
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(
            CastSpell(
                d.player1, dennick, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB, paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        while (d.state.stack.isNotEmpty()) d.bothPass()
        val apparition = d.findPermanent(d.player1, "Dennick, Pious Apparition")
        apparition shouldNotBe null
        d.state.projectedState.hasKeyword(apparition!!, Keyword.FLYING) shouldBe true
        d.state.projectedState.getPower(apparition) shouldBe 3
    }

    test("back face: investigates when a creature card hits any graveyard, only once each turn") {
        val d = driver()
        // The front face dies first: a creature card hitting a graveyard before the Apparition
        // is in play, which must not count against its once-each-turn trigger.
        val dennick = d.putCreatureOnBattlefield(d.player1, "Dennick, Pious Apprentice")
        d.kill(d.player1, dennick)
        val card = d.getGraveyard(d.player1).single { d.state.getEntity(it)?.get<CardComponent>()?.name == "Dennick, Pious Apprentice" }
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(
            CastSpell(
                d.player1, card, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB, paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        while (d.state.stack.isNotEmpty()) d.bothPass()
        d.findPermanent(d.player1, "Dennick, Pious Apparition") shouldNotBe null
        d.clues(d.player1) shouldBe 0

        val theirBear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.kill(d.player1, theirBear)
        d.clues(d.player1) shouldBe 1

        val ownBear = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.kill(d.player1, ownBear)
        d.clues(d.player1) shouldBe 1
    }

    test("back face: exiled instead of going to the graveyard") {
        val d = driver()
        val card = d.putCardInGraveyard(d.player1, "Dennick, Pious Apprentice")
        d.giveMana(d.player1, Color.WHITE, 1)
        d.giveMana(d.player1, Color.BLUE, 1)
        d.giveColorlessMana(d.player1, 2)
        d.submit(
            CastSpell(
                d.player1, card, useAlternativeCost = true,
                alternativeCostType = AlternativeCostType.DISTURB, paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        while (d.state.stack.isNotEmpty()) d.bothPass()
        val apparition = d.findPermanent(d.player1, "Dennick, Pious Apparition")!!
        d.kill(d.player1, apparition)
        d.getGraveyard(d.player1).contains(card) shouldBe false
        d.getExile(d.player1).contains(card) shouldBe true
    }
})
