package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OptionalLifeCostTest : FunSpec({
    val land = card("Island") { typeLine = "Basic Land — Island" }
    val discount = card("Life Discounter") {
        typeLine = "Creature — Wizard"; power = 2; toughness = 2
        staticAbility { ability = ModifySpellCost(SpellCostTarget.YouCast(GameObjectFilter.Creature.withColor(Color.BLUE)),
            CostModification.ReduceColored("{U}"), optionalLifePayment = 2) }
    }
    val spell = card("Blue Test Creature") {
        manaCost = "{1}{U}{U}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
    }
    val phyrexian = card("Phyrexian Test Creature") {
        manaCost = "{U}{U/P}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
    }
    val red = card("Red Test Creature") { manaCost = "{R}"; typeLine = "Creature"; power = 1; toughness = 1 }
    val kicked = card("Kicked Test Creature") {
        manaCost = "{U}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
        keywordAbility(KeywordAbility.kicker("{U}"))
    }
    val indicator = card("Indicator Test Creature") {
        manaCost = "{2}"; colorIndicator = "U"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
    }
    val hybrid = card("Hybrid Test Creature") {
        manaCost = "{U/R}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
    }
    val recurring = card("Recurring Blue Creature") {
        manaCost = "{U}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD, Zone.EXILE)) }
        selfAlternativeCost = SelfAlternativeCost(ManaCost.parse("{2}"))
    }
    fun setup(life: Int = 20) = GameTestDriver().apply {
        registerCards(listOf(land, discount, spell, phyrexian, red, kicked, indicator, hybrid, recurring))
        initMirrorMatch(Deck.of("Island" to 30), startingLife = life)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    test("declared life cost reduces exactly one pip and emits life payment") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name)
        d.giveMana(p, Color.BLUE, 2)
        val offer = d.legalActions(p).single { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() } == true }
        offer.affordable shouldBe true
        val result = d.submit(offer.action)
        result.error shouldBe null
        d.getLifeTotal(p) shouldBe 18
        result.events.filterIsInstance<LifeChangedEvent>().size shouldBe 1
        d.state.stack.contains(id) shouldBe true
    }
    test("declining retains full mana price and life") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name); d.giveMana(p, Color.BLUE, 3)
        d.submit(CastSpell(p, id)).error shouldBe null
        d.getLifeTotal(p) shouldBe 20
    }
    test("two copies each permit one payment") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name); d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name); d.giveColorlessMana(p, 1)
        val offer = d.legalActions(p).single { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.size == 2 } == true }
        d.submit(offer.action).error shouldBe null
        d.getLifeTotal(p) shouldBe 16
    }
    test("a forged repeated payment is rejected atomically") {
        val d = setup(); val p = d.activePlayer!!
        val source = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name)
        d.giveMana(p, Color.BLUE, 3)
        val payment = CostModifierPayment(source, 0)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(payment, payment))).error shouldNotBe null
        d.getLifeTotal(p) shouldBe 20
    }
    test("an unmatched spell cannot use the payment") {
        val d = setup(); val p = d.activePlayer!!
        val source = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, red.name); d.giveMana(p, Color.RED)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(source, 0)))).error shouldNotBe null
    }
    test("enumeration reserves life before offering Phyrexian payment") {
        val d = setup(3); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, phyrexian.name)
        d.legalActions(p).filter { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() } == true }
            .any { it.affordable } shouldBe false
    }
    test("two payments reduce both the printed and kicker blue pips") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name); val b = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, kicked.name)
        val action = CastSpell(p, id, declaredCostSlot = ChoiceSlot.KICKED,
            optionalCostPayments = listOf(CostModifierPayment(a, 0), CostModifierPayment(b, 0)))
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe 16
    }
    test("life may be paid without reducing any mana and never reduces generic") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, indicator.name)
        d.giveColorlessMana(p, 1)
        val action = CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))
        d.submit(action).error shouldNotBe null
        d.getLifeTotal(p) shouldBe 20
        d.giveColorlessMana(p, 1)
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
    }
    test("payment is unavailable below the amount and may pay exactly the life total") {
        val d = setup(1); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, kicked.name)
        val action = CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))
        d.submit(action).error shouldNotBe null
        d.setLifeTotal(p, 2)
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe 0
    }
    test("declared colored half of hybrid is reducible") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, hybrid.name)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldBe null
    }

    for (cost in listOf("{U/R/P}", "{2/U}")) test("optional reduction removes the blue component of $cost") {
        val d = setup(3); val p = d.activePlayer!!
        val flexible = card("Flexible Blue Creature") {
            manaCost = cost; typeLine = "Creature — Wizard"; power = 2; toughness = 2
        }
        d.registerCards(listOf(flexible))
        d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, flexible.name)
        val offer = d.legalActions(p).single {
            (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() } == true
        }
        offer.affordable shouldBe true
        d.submit(offer.action).error shouldBe null
        d.getLifeTotal(p) shouldBe 1
    }

    test("source departure invalidates an unannounced payment") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, kicked.name)
        d.moveToGraveyard(a)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldNotBe null
    }
    test("opponent sources grant no payment") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(d.getOpponent(p), discount.name)
        val id = d.putCardInHand(p, kicked.name)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldNotBe null
        d.legalActions(p).none { (it.action as? CastSpell)?.optionalCostPayments?.isNotEmpty() == true } shouldBe true
    }
    test("removing source abilities with a spell removes the offer") {
        val blank = card("Blank Life Discounter") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature); effect = Effects.RemoveAllAbilities(t) }
        }
        val d = setup(); val p = d.activePlayer!!; d.registerCard(blank)
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val erase = d.putCardInHand(p, blank.name)
        d.castSpell(p, erase, listOf(a)).error shouldBe null; d.bothPass()
        val id = d.putCardInHand(p, kicked.name)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldNotBe null
    }
    test("a token copy supplies a distinct payment after the original leaves") {
        val copy = card("Copy Life Discounter") {
            manaCost = "{0}"; typeLine = "Instant"
            spell { val t = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.Creature); effect = Effects.CreateTokenCopyOfTarget(t) }
        }
        val d = setup(); val p = d.activePlayer!!; d.registerCard(copy)
        val a = d.putCreatureOnBattlefield(p, discount.name)
        d.castSpell(p, d.putCardInHand(p, copy.name), listOf(a)).error shouldBe null; d.bothPass()
        d.moveToGraveyard(a)
        val id = d.putCardInHand(p, kicked.name)
        val action = d.legalActions(p).single { (it.action as? CastSpell)?.let { c -> c.cardId == id && c.declaredCostSlot == null && c.optionalCostPayments.isNotEmpty() } == true }.action
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
    }
    test("a life-loss lock forbids optional payment") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, kicked.name)
        d.replaceState(d.state.updateEntity(p) { it.with(com.wingedsheep.engine.state.components.player.CantLoseLifeComponent()) })
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldNotBe null
    }
    test("explicit Phyrexian life payment is distinct from the optional cost") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, phyrexian.name)
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)),
            paymentStrategy = PaymentStrategy.Explicit(emptyList(), phyrexianLifePayments = listOf(Color.BLUE)))).error shouldBe null
        d.getLifeTotal(p) shouldBe 16
    }

    test("a locked life obligation survives source departure during a mana window") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name); d.giveMana(p, Color.BLUE, 2)
        val cost = ManaCost.parse("{1}{U}")
        val action = CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))
        val state = d.state
        d.replaceState(state.suspendForDecision(
            question = { decision -> ManaPaymentWindow.buildDecision(state, p, cost, decision, "Pay for spell",
                DecisionContext(sourceId = id, phase = DecisionPhase.CASTING), false, d.services.manaSolver) },
            answer = ManaActionPaymentContinuation(action, cost, lockedCastCost = cost,
                lockedAdditionalCosts = listOf(Costs.additional.PayLife(2))),
        ).state)
        d.moveToGraveyard(a)
        d.submitDecision(p, ManaSourcesSelectedResponse(d.pendingDecision!!.id, emptyList(), autoPay = true)).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
        d.state.stack.contains(id) shouldBe true
    }

    for (zone in listOf(Zone.GRAVEYARD, Zone.EXILE)) test("payment applies to casts from $zone") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name)
        val id = if (zone == Zone.GRAVEYARD) d.putCardInGraveyard(p, recurring.name) else d.putCardInExile(p, recurring.name)
        val action = d.legalActions(p).first { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() && !a.useAlternativeCost } == true }.action
        d.submit(action).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
    }
    test("an alternative generic cost can elect life without a discount") {
        val d = setup(); val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, recurring.name); d.giveColorlessMana(p, 2)
        val offer = d.legalActions(p).first { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() && a.useAlternativeCost } == true }
        offer.manaCostString shouldBe "{2}"
        d.submit(offer.action).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
    }
    test("a free cast can still elect its optional life cost") {
        val d = setup(); val p = d.activePlayer!!
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, spell.name)
        d.replaceState(d.state.updateEntity(id) { it.with(com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent(p)) })
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldBe null
        d.getLifeTotal(p) shouldBe 18
    }

    test("mandatory and optional life costs share the same budget") {
        val costly = card("Extra Life Creature") {
            manaCost = "{U}"; typeLine = "Creature — Wizard"; power = 2; toughness = 2
            additionalCost(Costs.additional.PayLife(2))
        }
        val d = setup(3); val p = d.activePlayer!!; d.registerCard(costly)
        val a = d.putCreatureOnBattlefield(p, discount.name)
        val id = d.putCardInHand(p, costly.name); d.giveMana(p, Color.BLUE)
        d.legalActions(p).none { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.optionalCostPayments.isNotEmpty() } == true && it.affordable } shouldBe true
        d.submit(CastSpell(p, id, optionalCostPayments = listOf(CostModifierPayment(a, 0)))).error shouldNotBe null
        d.getLifeTotal(p) shouldBe 3
    }

})
