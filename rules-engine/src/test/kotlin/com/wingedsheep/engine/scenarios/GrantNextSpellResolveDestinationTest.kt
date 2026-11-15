package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `GrantNextSpellResolveDestinationEffect` — "The next time you cast an instant or sorcery spell from
 * your hand this turn, put that card into your hand instead of into your graveyard as it resolves."
 * (Soulfire Grand Master). The rules pinned here:
 *  - the next matching spell from hand resolves and goes to its owner's hand, not the graveyard;
 *  - the rider is one-shot: the spell after it goes to the graveyard, and several activations all
 *    apply to the same one spell;
 *  - a matching spell that doesn't resolve (countered) is put into the graveyard and still spends
 *    the rider (Soulfire ruling);
 *  - a non-matching spell (a creature), or a matching one cast from somewhere other than the hand
 *    (flashback from the graveyard), neither uses nor spends it;
 *  - a spell whose own instructions exile it isn't returned (Soulfire ruling on Temporal Trespass);
 *  - it lasts only for the turn.
 */
class GrantNextSpellResolveDestinationTest : FunSpec({

    val recaller = card("Test Recaller") {
        manaCost = "{1}"
        typeLine = "Creature — Human Monk"
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.GrantNextSpellResolveDestination(
                destination = AfterResolveDestination.HAND,
                spellFilter = GameObjectFilter.InstantOrSorcery,
                fromZones = setOf(Zone.HAND)
            )
        }
    }

    val fleetingShock = card("Fleeting Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.GainLife(1)
            selfExile()
        }
    }

    val flashbackShock = card("Flashback Shock") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell {
            effect = Effects.GainLife(1)
        }
        keywordAbility(KeywordAbility.flashback("{R}"))
    }

    fun setUp(): Triple<GameTestDriver, EntityId, EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(recaller, fleetingShock, flashbackShock))
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val source = d.putCreatureOnBattlefield(me, "Test Recaller")
        return Triple(d, me, source)
    }

    fun resolveAll(d: GameTestDriver) {
        var guard = 0
        while (d.stackSize > 0 && guard++ < 10) d.bothPass()
    }

    fun activate(d: GameTestDriver, me: EntityId, source: EntityId) {
        d.giveMana(me, Color.RED, 1)
        d.submitSuccess(ActivateAbility(me, source, recaller.activatedAbilities.first().id))
        resolveAll(d)
    }

    fun boltOpponent(d: GameTestDriver, me: EntityId, name: String = "Lightning Bolt"): EntityId {
        val bolt = d.putCardInHand(me, name)
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(d.getOpponent(me))).error shouldBe null
        resolveAll(d)
        return bolt
    }

    test("the next instant from hand resolves and returns to its owner's hand") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        val bolt = boltOpponent(d, me)

        withClue("Lightning Bolt resolved") { d.getLifeTotal(d.getOpponent(me)) shouldBe 17 }
        withClue("in hand, not in the graveyard") {
            d.getHand(me).contains(bolt) shouldBe true
            d.getGraveyard(me).contains(bolt) shouldBe false
        }
    }

    test("one-shot: the spell after it goes to the graveyard, even after two activations") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        activate(d, me, source)
        val first = boltOpponent(d, me)
        val second = boltOpponent(d, me)

        d.getHand(me).contains(first) shouldBe true
        withClue("both activations applied to the first spell") {
            d.getGraveyard(me).contains(second) shouldBe true
        }
    }

    test("a countered spell goes to the graveyard and still spends the rider") {
        val (d, me, source) = setUp()
        val opponent = d.getOpponent(me)
        activate(d, me, source)
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).error shouldBe null
        d.passPriority(me)
        val counterspell = d.putCardInHand(opponent, "Counterspell")
        d.giveMana(opponent, Color.BLUE, 2)
        d.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(bolt)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        resolveAll(d)

        d.getGraveyard(me).contains(bolt) shouldBe true
        withClue("the rider was spent on the countered spell") {
            val next = boltOpponent(d, me)
            d.getGraveyard(me).contains(next) shouldBe true
        }
    }

    test("a creature spell neither uses nor spends the rider") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        val courser = d.putCardInHand(me, "Centaur Courser")
        d.giveMana(me, Color.GREEN, 3)
        d.castSpell(me, courser).error shouldBe null
        resolveAll(d)

        val bolt = boltOpponent(d, me)
        d.getHand(me).contains(bolt) shouldBe true
    }

    test("an instant cast from the graveyard neither uses nor spends the rider") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        val flashed = d.putCardInGraveyard(me, "Flashback Shock")
        d.giveMana(me, Color.RED, 1)
        d.submit(
            CastSpell(me, flashed, paymentStrategy = PaymentStrategy.FromPool, useAlternativeCost = true)
        ).error shouldBe null
        resolveAll(d)
        withClue("flashback exiled it as usual") { d.getExile(me).contains(flashed) shouldBe true }

        val bolt = boltOpponent(d, me)
        d.getHand(me).contains(bolt) shouldBe true
    }

    test("a spell that exiles itself isn't returned to hand") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        val shock = d.putCardInHand(me, "Fleeting Shock")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, shock).error shouldBe null
        resolveAll(d)

        d.getExile(me).contains(shock) shouldBe true
        d.getHand(me).contains(shock) shouldBe false
    }

    test("the rider ends with the turn") {
        val (d, me, source) = setUp()
        activate(d, me, source)
        d.passPriorityUntil(Step.UPKEEP)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        val bolt = boltOpponent(d, me)
        d.getGraveyard(me).contains(bolt) shouldBe true
    }
})
