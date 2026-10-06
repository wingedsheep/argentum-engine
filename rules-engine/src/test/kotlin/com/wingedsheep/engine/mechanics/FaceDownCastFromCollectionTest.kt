package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.mana.SpentMana
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.TurnsFaceUpInsteadComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Cast that card face down as a 2/2 creature spell without paying its mana cost" over a card
 * chosen by the mana spent to activate the ability — Illusionary Mask's shape:
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaCostPayableWithManaSpent] picks the
 * cards, `CastFromCollectionWithoutPayingCostEffect(castFaceDown = true, turnsFaceUpInstead = true)`
 * casts one face down (CR 708.4) and leaves [FaceUpInstead]'s rider on the permanent it becomes.
 */
class FaceDownCastFromCollectionTest : FunSpec({

    val mask = card("Face Down Test Mask") {
        manaCost = "{2}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{X}")
            timing = TimingRule.SorcerySpeed
            effect = Effects.Pipeline {
                val payable = gather(
                    CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature.manaCostPayableWithManaSpent())
                )
                val chosen = chooseUpTo(1, from = payable)
                run(Effects.May(Effects.CastFaceDownFromCollection(chosen, turnsFaceUpInstead = true)))
            }
        }
    }
    fun creature(name: String, cost: String, p: Int = 3) = card(name) {
        manaCost = cost
        typeLine = "Creature — Test"
        power = p
        toughness = p
    }
    val oneU = creature("Test One Blue", "{1}{U}")
    val twoU = creature("Test Two Blue", "{2}{U}")
    val green = creature("Test Green", "{G}")
    val hybrid = creature("Test Hybrid", "{W/U}{W/U}")
    val generic = creature("Test Generic", "{2}")

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(mask, oneU, twoU, green, hybrid, generic))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.maskAbility() = mask.activatedAbilities.single().id

    /** Activate the mask for X, resolve it, and return the offered cards. */
    fun GameTestDriver.activate(
        me: EntityId,
        maskId: EntityId,
        x: Int,
        strategy: PaymentStrategy = PaymentStrategy.AutoPay,
    ): List<EntityId> {
        submitSuccess(ActivateAbility(me, maskId, maskAbility(), xValue = x, paymentStrategy = strategy))
        bothPass()
        val decision = state.pendingDecision as? SelectCardsDecision ?: return emptyList()
        return decision.options
    }

    test("spent mana could pay a cost by amount and type (the card's {U}{U} ruling)") {
        val uu = SpentMana(blue = 2)
        listOf("{U}{U}", "{1}{U}", "{2}", "{W/U}{W/U}", "{U}", "{0}", "{X}{U}").forEach {
            uu.couldPay(ManaCost.parse(it)) shouldBe true
        }
        listOf("{2}{U}", "{G}", "{C}", "{S}").forEach {
            uu.couldPay(ManaCost.parse(it)) shouldBe false
        }
        SpentMana(colorless = 1).couldPay(ManaCost.parse("{C}")) shouldBe true
        SpentMana(green = 1, colorless = 1).couldPay(ManaCost.parse("{1}{G}")) shouldBe true
    }

    test("only creature cards the mana spent on X could pay are offered") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val offered = listOf(oneU, twoU, green, hybrid, generic).associate { it.name to d.putCardInHand(me, it.name) }

        val options = d.activate(me, maskId, 2)

        options shouldContainExactlyInAnyOrder listOf(offered.getValue(oneU.name), offered.getValue(hybrid.name), offered.getValue(generic.name))
    }

    test("explicitly chosen sources count too") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        val islands = List(2) { d.putLandOnBattlefield(me, "Island") }
        val blueCard = d.putCardInHand(me, oneU.name)
        val twoBlue = d.putCardInHand(me, twoU.name)
        val greenCard = d.putCardInHand(me, green.name)

        // {U}{U} from the two chosen Islands: {1}{U} fits; {2}{U} and {G} don't.
        val options = d.activate(me, maskId, 2, PaymentStrategy.Explicit(islands))
        options shouldBe listOf(blueCard)
        (twoBlue in options || greenCard in options) shouldBe false
    }

    test("floating mana of several types counts by type") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        d.giveMana(me, Color.GREEN, 1)
        d.giveMana(me, Color.BLUE, 1)
        val greenCard = d.putCardInHand(me, green.name)
        val blueCard = d.putCardInHand(me, oneU.name)
        val hybridCard = d.putCardInHand(me, hybrid.name)

        // {G}{U} spent: {G}, {1}{U} fit; {W/U}{W/U} needs two white-or-blue.
        d.activate(me, maskId, 2, PaymentStrategy.FromPool) shouldContainExactlyInAnyOrder listOf(greenCard, blueCard)
        (hybridCard in (d.state.pendingDecision as SelectCardsDecision).options) shouldBe false
    }

    test("the chosen card is cast face down as a free 2/2 spell and enters face down with the rider") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val card = d.putCardInHand(me, oneU.name)

        d.activate(me, maskId, 2) shouldBe listOf(card)
        d.submitCardSelection(me, listOf(card)).error shouldBe null
        (d.state.pendingDecision is YesNoDecision) shouldBe true
        d.submitYesNo(me, true).error shouldBe null

        // On the stack: a face-down spell, cast — so it can be responded to — and nothing paid.
        val spell = d.state.getEntity(card)!!.get<SpellOnStackComponent>()!!
        spell.castFaceDown shouldBe true
        spell.turnsFaceUpInstead shouldBe true
        d.state.getEntity(card)!!.has<PlayWithoutPayingCostComponent>() shouldBe false
        d.state.mayPlayPermissions.none { card in it.cardIds } shouldBe true

        d.bothPass()

        d.state.getBattlefield().contains(card) shouldBe true
        val permanent = d.state.getEntity(card)!!
        permanent.has<FaceDownComponent>() shouldBe true
        permanent.has<TurnsFaceUpInsteadComponent>() shouldBe true
        // No morph, so no way to turn it face up but the rider.
        permanent.has<MorphDataComponent>() shouldBe false
        d.state.projectedState.getPower(card) shouldBe 2
        d.state.projectedState.getToughness(card) shouldBe 2
    }

    test("the rider outlives the mask, and tapping turns it face up") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val card = d.putCardInHand(me, oneU.name)
        d.activate(me, maskId, 2)
        d.submitCardSelection(me, listOf(card))
        d.submitYesNo(me, true)
        d.bothPass()

        d.moveToGraveyard(maskId)
        val tapped = com.wingedsheep.engine.core.tap(d.state, card)

        tapped.state.getEntity(card)!!.has<FaceDownComponent>() shouldBe false
        tapped.state.projectedState.getPower(card) shouldBe 3
    }

    test("a card with morph cast this way keeps its morph turn-up procedure") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        d.giveMana(me, Color.WHITE, 3)
        val card = d.putCardInHand(me, TestCards.MorphTestCreature.name)

        d.activate(me, maskId, 3, PaymentStrategy.FromPool) shouldBe listOf(card)
        d.submitCardSelection(me, listOf(card))
        d.submitYesNo(me, true)
        d.bothPass()

        d.state.getEntity(card)!!.has<FaceDownComponent>() shouldBe true
        d.state.getEntity(card)!!.has<MorphDataComponent>() shouldBe true
        d.state.getEntity(card)!!.has<TurnsFaceUpInsteadComponent>() shouldBe true
    }

    test("declining the cast leaves the card in hand with no lingering grant") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        repeat(2) { d.putLandOnBattlefield(me, "Island") }
        val card = d.putCardInHand(me, oneU.name)

        d.activate(me, maskId, 2)
        d.submitCardSelection(me, listOf(card))
        d.submitYesNo(me, false)

        d.getHand(me).contains(card) shouldBe true
        d.state.getEntity(card)!!.has<PlayWithoutPayingCostComponent>() shouldBe false
        d.state.mayPlayPermissions.none { card in it.cardIds } shouldBe true
    }

    test("X = 0 spends nothing, so only a {0} creature could be chosen") {
        val d = driver()
        val me = d.player1
        val maskId = d.putPermanentOnBattlefield(me, mask.name)
        d.putCardInHand(me, oneU.name)

        d.activate(me, maskId, 0) shouldBe emptyList()
    }

    test("without a permission a creature with no morph can't be cast face down") {
        val d = driver()
        val me = d.player1
        repeat(3) { d.putLandOnBattlefield(me, "Island") }
        val card = d.putCardInHand(me, oneU.name)

        val result = d.submitExpectFailure(CastSpell(me, card, castFaceDown = true))

        result.error shouldNotBe null
    }
})
