package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * An activated ability a resolving effect grants to one specific card **in exile** — "That nonland
 * card gains 'Exile a card from your hand: … Activate only during your turn and only if this card is
 * exiled.'" (Q, Stacking the Deck). The ability is `activateFromZone = EXILE`, so:
 *
 * - the grant may land on an exiled card (an ability that functions only on the battlefield may not —
 *   it would be dead text on a card that isn't a permanent);
 * - the owner sees it as a legal action from exile and can activate it, choosing which hand card
 *   pays the exile cost;
 * - its activation restrictions (during your turn) still apply;
 * - it belongs to that object and ends as the card leaves exile (CR 400.7).
 */
class ExileGrantedActivatedAbilityTest : FunSpec({

    val exiledCard = TargetFilter(GameObjectFilter.Any, zone = Zone.EXILE)

    val gifter = card("Exile Gifter") {
        manaCost = "{1}"
        typeLine = "Artifact"
        val exileAbility = ActivatedAbility(
            cost = Costs.ExileFromHand(),
            effect = Effects.GainLife(3),
            restrictions = listOf(ActivationRestriction.OnlyDuringYourTurn),
            activateFromZone = Zone.EXILE,
            descriptionOverride = "Exile a card from your hand: You gain 3 life. Activate only during your turn and only if this card is exiled.",
        )
        val battlefieldAbility = ActivatedAbility(
            cost = Costs.Free,
            effect = Effects.GainLife(1),
            descriptionOverride = "{0}: You gain 1 life.",
        )
        activatedAbility {
            cost = Costs.Free
            val t = target(exiledCard)
            effect = Effects.GrantActivatedAbility(exileAbility, t, Duration.Permanent)
            description = "Target card in exile gains the exile ability."
        }
        activatedAbility {
            cost = Costs.Free
            val t = target(exiledCard)
            effect = Effects.GrantActivatedAbility(battlefieldAbility, t, Duration.Permanent)
            description = "Target card in exile gains a battlefield-only ability."
        }
        activatedAbility {
            cost = Costs.Free
            val t = target(exiledCard)
            effect = Effects.ReturnToHand(t)
            description = "Return target card in exile to its owner's hand."
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(gifter))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.useGifter(gifterId: EntityId, index: Int, exiled: EntityId) {
        val me = activePlayer!!
        submit(
            ActivateAbility(
                me, gifterId, gifter.activatedAbilities[index].id,
                listOf(ChosenTarget.Card(exiled, me, Zone.EXILE)),
            )
        ).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.exileActions(player: EntityId, cardId: EntityId) =
        legalActions(player).filter { it.sourceZone == "EXILE" && (it.action as? ActivateAbility)?.sourceId == cardId }

    test("an exiled card gains the ability, its owner activates it from exile paying the hand-exile cost") {
        val d = newDriver()
        val me = d.activePlayer!!
        val gifterId = d.putPermanentOnBattlefield(me, "Exile Gifter")
        val exiled = d.putCardInExile(me, "Grizzly Bears")
        d.useGifter(gifterId, 0, exiled)

        val actions = d.exileActions(me, exiled)
        withClue("the granted exile ability is offered from exile") { actions shouldHaveSize 1 }

        val handBefore = d.getHandSize(me)
        val lifeBefore = d.getLifeTotal(me)
        val hand = d.getHand(me)
        d.submit(actions.single().action).error shouldBe null
        val choice = d.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the player chooses which hand card pays the cost") {
            choice.options shouldContainExactlyInAnyOrder hand
        }
        val paid = hand.last()
        d.submitCardSelection(me, listOf(paid))
        d.bothPass()
        withClue("the chosen card was exiled") { d.state.getExile(me).contains(paid) shouldBe true }

        d.getLifeTotal(me) shouldBe lifeBefore + 3
        withClue("a card from hand was exiled to pay") { d.getHandSize(me) shouldBe handBefore - 1 }
        withClue("the ability stays with the still-exiled card") { d.exileActions(me, exiled) shouldHaveSize 1 }
    }

    test("activate only during your turn: not offered, and rejected, on the opponent's turn") {
        val d = newDriver()
        val me = d.activePlayer!!
        val gifterId = d.putPermanentOnBattlefield(me, "Exile Gifter")
        val exiled = d.putCardInExile(me, "Grizzly Bears")
        d.useGifter(gifterId, 0, exiled)
        val abilityId = d.state.grantedActivatedAbilities.single { it.entityId == exiled }.ability.id

        d.passPriorityUntil(Step.UPKEEP, d.getOpponent(me))
        d.exileActions(me, exiled).shouldBeEmpty()
        d.submit(ActivateAbility(me, exiled, abilityId)).error shouldNotBe null
    }

    test("only the owner may activate it") {
        val d = newDriver()
        val me = d.activePlayer!!
        val gifterId = d.putPermanentOnBattlefield(me, "Exile Gifter")
        val exiled = d.putCardInExile(me, "Grizzly Bears")
        d.useGifter(gifterId, 0, exiled)

        d.exileActions(d.getOpponent(me), exiled).shouldBeEmpty()
    }

    test("a battlefield-only activated ability is not granted to an exiled card") {
        val d = newDriver()
        val me = d.activePlayer!!
        val gifterId = d.putPermanentOnBattlefield(me, "Exile Gifter")
        val exiled = d.putCardInExile(me, "Grizzly Bears")
        d.useGifter(gifterId, 1, exiled)

        d.state.grantedActivatedAbilities.filter { it.entityId == exiled }.shouldBeEmpty()
    }

    test("the grant ends when the card leaves exile (CR 400.7)") {
        val d = newDriver()
        val me = d.activePlayer!!
        val gifterId = d.putPermanentOnBattlefield(me, "Exile Gifter")
        val exiled = d.putCardInExile(me, "Grizzly Bears")
        d.useGifter(gifterId, 0, exiled)
        d.state.grantedActivatedAbilities.filter { it.entityId == exiled } shouldHaveSize 1

        d.useGifter(gifterId, 2, exiled)

        withClue("the card is in hand and no longer carries the grant") {
            d.state.getHand(me).contains(exiled) shouldBe true
            d.state.grantedActivatedAbilities.filter { it.entityId == exiled }.shouldBeEmpty()
        }
    }
})
