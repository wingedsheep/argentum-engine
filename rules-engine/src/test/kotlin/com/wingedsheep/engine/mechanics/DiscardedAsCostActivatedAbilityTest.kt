package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for [EffectTarget.DiscardedAsCost] on an **activated ability** — "the discarded
 * card" when the discard is the ability's own cost rather than a spell's additional cost.
 *
 * The cost is paid on activation, so the ability records the card it discarded and hands it to
 * the resolving effect. Each discard shape is covered: a chosen card, a card the engine picks at
 * random (the ability must see the card actually discarded, not a choice it never had), and the
 * source discarding itself from hand. A second activation must see only its own discard.
 */
class DiscardedAsCostActivatedAbilityTest : FunSpec({

    val lifeEqualToDiscardedManaValue = Effects.GainLife(DynamicAmounts.manaValueOf(EffectTarget.DiscardedAsCost()))

    val chosenDiscarder = card("Test Chosen Discarder") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Discard()
            effect = lifeEqualToDiscardedManaValue
            description = "Discard a card: You gain life equal to the discarded card's mana value."
        }
    }

    val randomDiscarder = card("Test Random Discarder") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.DiscardAtRandom(1)
            effect = lifeEqualToDiscardedManaValue
            description = "Discard a card at random: You gain life equal to the discarded card's mana value."
        }
    }

    val selfDiscarder = card("Test Self Discarder") {
        manaCost = "{4}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.DiscardSelf
            activateFromZone = Zone.HAND
            effect = lifeEqualToDiscardedManaValue
            description = "Discard this card: You gain life equal to the discarded card's mana value."
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + chosenDiscarder + randomDiscarder + selfDiscarder)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.emptyHand(player: EntityId) {
        for (card in getHand(player)) {
            replaceState(state.removeFromZone(ZoneKey(player, Zone.HAND), card).addToZone(ZoneKey(player, Zone.LIBRARY), card))
        }
    }

    fun GameTestDriver.activate(sourceId: EntityId, payment: AdditionalCostPayment?) {
        val definition = listOf(chosenDiscarder, randomDiscarder, selfDiscarder)
            .first { it.name == state.getEntity(sourceId)!!.get<CardComponent>()!!.name }
        val result = submit(
            ActivateAbility(
                playerId = player1,
                sourceId = sourceId,
                abilityId = definition.activatedAbilities.first().id,
                costPayment = payment,
            )
        )
        withClue(result.error ?: "activation failed") { result.outcome shouldBe Outcome.Done }
    }

    test("a chosen discard is the card the resolving ability reads") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Chosen Discarder")
        val myr = d.putCardInHand(d.player1, "Palladium Myr")
        val life = d.getLifeTotal(d.player1)

        d.activate(source, AdditionalCostPayment(discardedCards = listOf(myr)))
        d.bothPass()

        d.getLifeTotal(d.player1) shouldBe life + 3
    }

    test("a random discard records the card the engine actually discarded") {
        val d = driver()
        d.emptyHand(d.player1)
        val source = d.putPermanentOnBattlefield(d.player1, "Test Random Discarder")
        d.putCardInHand(d.player1, "Force of Nature")
        val life = d.getLifeTotal(d.player1)

        d.activate(source, null)
        d.bothPass()

        withClue("Force of Nature (mana value 5) was the only card to discard") {
            d.getLifeTotal(d.player1) shouldBe life + 5
        }
    }

    test("a card discarding itself from hand is the discarded card") {
        val d = driver()
        val source = d.putCardInHand(d.player1, "Test Self Discarder")
        val life = d.getLifeTotal(d.player1)

        d.activate(source, null)
        d.bothPass()

        d.getLifeTotal(d.player1) shouldBe life + 4
    }

    test("each activation reads only its own discard") {
        val d = driver()
        val source = d.putPermanentOnBattlefield(d.player1, "Test Chosen Discarder")
        val myr = d.putCardInHand(d.player1, "Palladium Myr")
        val lions = d.putCardInHand(d.player1, "Savannah Lions")
        val life = d.getLifeTotal(d.player1)

        d.activate(source, AdditionalCostPayment(discardedCards = listOf(myr)))
        d.activate(source, AdditionalCostPayment(discardedCards = listOf(lions)))
        d.bothPass()
        withClue("the second activation (Savannah Lions, mana value 1) resolves first") {
            d.getLifeTotal(d.player1) shouldBe life + 1
        }
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe life + 4
    }
})
