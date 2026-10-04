package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Payment-layer tests use activation identities directly; production/lifetime have separate tests. */
class ManaObligationPaymentTest : FunSpec({
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun scoped(d: GameTestDriver, pool: ManaPoolComponent, pending: Set<String>): GameState {
        val p = d.activePlayer!!
        return d.state.updateEntity(p) { it.with(pool) }.pushContinuation(
            ManaSpendingObligationsContinuation(p, EffectContext(sourceId = null, controllerId = p), "scope", pending))
    }
    fun unit(color: Color?, id: String, restriction: ManaRestriction = ManaRestriction.AnySpend) =
        RestrictedManaEntry(color, restriction, obligationIds = setOf(id))
    fun pay(d: GameTestDriver, state: GameState, cost: String,
            strategy: PaymentStrategy = PaymentStrategy.FromPool, x: Int = 0) =
        CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(
            state, CastSpell(d.activePlayer!!, d.activePlayer!!, paymentStrategy = strategy),
            ManaCost.parse(cost), "Payment probe", x,
            SpellPaymentContext(cardTypes = setOf(CardType.SORCERY)))

    test("one spent unit discharges an activation and leaves its excess usable") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(Color.GREEN, "one"), unit(Color.GREEN, "one"))), setOf("one"))
        val result = pay(d, state, "{G}")
        result.error shouldBe null
        result.state.remainingManaObligations(p) shouldBe false
        val remaining = result.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana
        remaining.size shouldBe 1
        remaining.single().obligationIds shouldBe emptySet()
        result.events.filterIsInstance<ManaSpentEvent>().single().green shouldBe 1
    }
    test("two multi-mana activations can each contribute one unit while both leave excess") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(
            unit(null, "a"), unit(null, "a"), unit(null, "b"), unit(null, "b"))), setOf("a", "b"))
        val result = pay(d, state, "{2}")
        result.error shouldBe null
        result.state.remainingManaObligations(p) shouldBe false
        val remaining = result.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana
        remaining.size shouldBe 2
        remaining.all { it.obligationIds.isEmpty() } shouldBe true
    }
    test("same-color untagged mana cannot conceal an activation whose mana was never spent") {
        val d = driver()
        val state = scoped(d, ManaPoolComponent(green = 1, restrictedMana = listOf(unit(Color.RED, "unused"))), setOf("unused"))
        val result = pay(d, state, "{G}")
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe state
        result.events shouldBe emptyList()
    }
    test("unrestricted preexisting surplus does not acquire an obligation") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(green = 2, restrictedMana = listOf(unit(Color.GREEN, "new"))), setOf("new"))
        val result = pay(d, state, "{G}")
        result.error shouldBe null
        result.state.getEntity(p)!!.get<ManaPoolComponent>()!!.green shouldBe 2
        result.state.remainingManaObligations(p) shouldBe false
    }
    test("X payment discharges the activation that produced its mana") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(Color.GREEN, "x"), unit(Color.GREEN, "x"))), setOf("x"))
        val result = pay(d, state, "{X}", x = 1)
        result.error shouldBe null
        result.xManaSpentByColor shouldBe mapOf(Color.GREEN to 1)
        result.state.remainingManaObligations(p) shouldBe false
        result.state.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana.size shouldBe 1
    }
    test("existing restrictions still apply to activation-tagged mana") {
        val d = driver()
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(Color.GREEN, "creature", ManaRestriction.CreatureSpellsOnly))), setOf("creature"))
        val result = pay(d, state, "{G}")
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe state
    }
    test("a free instructed play cannot spend away a pending activation") {
        val d = driver()
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(null, "unused"))), setOf("unused"))
        val result = pay(d, state, "{0}")
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe state
    }
    test("scoped automatic and explicit payment execute tracked intrinsic activations") {
        val d = driver(); val p = d.activePlayer!!
        val forest = d.putPermanentOnBattlefield(p, "Forest")
        val state = scoped(d, ManaPoolComponent(), emptySet())
        for (strategy in listOf(PaymentStrategy.AutoPay, PaymentStrategy.Explicit(listOf(forest)))) {
            val result = pay(d, state, "{G}", strategy)
            result.error shouldBe null
            result.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
            result.state.remainingManaObligations(p) shouldBe false
            result.events.filterIsInstance<ManaAddedEvent>().single().green shouldBe 1
            result.events.filterIsInstance<AbilityActivatedEvent>().single().sourceId shouldBe forest
            state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
        }
    }
    test("scoped automatic and explicit choices can spend sufficient already-floating mana") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(Color.GREEN, "ready"))), setOf("ready"))
        for (strategy in listOf(PaymentStrategy.AutoPay, PaymentStrategy.Explicit(emptyList()))) {
            val result = pay(d, state, "{G}", strategy)
            result.error shouldBe null
            result.state.remainingManaObligations(p) shouldBe false
        }
    }
    test("payment synchronizes all containing scopes for the paying player only") {
        val d = driver(); val p = d.activePlayer!!; val opponent = d.getOpponent(p)
        val context = EffectContext(sourceId = null, controllerId = p)
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(unit(Color.GREEN, "shared"))), setOf("shared"))
            .pushContinuation(ManaSpendingObligationsContinuation(p, context, "inner", setOf("shared")))
            .pushContinuation(ManaSpendingObligationsContinuation(opponent, context, "other", setOf("shared")))
        val result = pay(d, state, "{G}")
        result.error shouldBe null
        result.state.remainingManaObligations(p) shouldBe false
        result.state.remainingManaObligations(opponent) shouldBe true
        result.state.continuationStack.size shouldBe state.continuationStack.size
    }
    test("scoped explicit mana activation pays both fixed and X portions directly") {
        val d = driver(); val p = d.activePlayer!!
        val converter = card("Scoped X Payment Probe") {
            manaCost = "{0}"; typeLine = "Land"
            activatedAbility {
                cost = Costs.Mana("{X}{G}")
                effect = Effects.AddMana(Color.BLUE, 1)
                manaAbility = true
            }
        }
        d.registerCards(listOf(converter))
        val source = d.putLandOnBattlefield(p, converter.name)
        val forest = d.putLandOnBattlefield(p, "Forest")
        d.replaceState(scoped(d, ManaPoolComponent(restrictedMana = listOf(
            unit(Color.GREEN, "fixed"), unit(null, "x"), unit(null, "x"))), setOf("fixed", "x")))
        val result = d.submit(ActivateAbility(p, source, converter.script.activatedAbilities.first().id,
            xValue = 2, paymentStrategy = PaymentStrategy.Explicit(emptyList())))
        result.error shouldBe null
        d.state.getEntity(forest)!!.has<TappedComponent>() shouldBe false
        val pool = d.state.getEntity(p)!!.get<ManaPoolComponent>()!!
        pool.total shouldBe 1
        pool.restrictedMana.single().color shouldBe Color.BLUE
        val pending = d.state.activeManaSpendingScope(p)!!.pendingIds
        pending.size shouldBe 1
        pending.intersect(setOf("fixed", "x")) shouldBe emptySet()
        pool.restrictedMana.single().obligationIds shouldBe pending
    }
    test("ordinary automatic payment retains the existing solver path") {
        val d = driver(); val p = d.activePlayer!!
        val forest = d.putPermanentOnBattlefield(p, "Forest")
        val result = pay(d, d.state, "{G}", PaymentStrategy.AutoPay)
        result.error shouldBe null
        result.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
    }
    test("synchronous chosen-color production keeps mirror-color tap bonuses outside its obligation") {
        val d = driver(); val p = d.activePlayer!!
        val sourceCard = card("Scoped Chosen Mana Probe") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddAnyColorMana()
                manaAbility = true
            }
        }
        val bonusCard = card("Scoped Mirror Mana Probe") {
            typeLine = "Enchantment"
            staticAbility {
                ability = com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap(
                    sourceFilter = com.wingedsheep.sdk.scripting.GameObjectFilter.Land,
                    color = null,
                )
            }
        }
        d.registerCards(listOf(sourceCard, bonusCard))
        val source = d.putLandOnBattlefield(p, sourceCard.name)
        d.putPermanentOnBattlefield(p, bonusCard.name)
        d.replaceState(scoped(d, ManaPoolComponent(), emptySet()))
        val result = d.submit(ActivateAbility(p, source, sourceCard.script.activatedAbilities.first().id,
            manaColorChoice = Color.BLUE))
        result.error shouldBe null
        val pool = d.state.getEntity(p)!!.get<ManaPoolComponent>()!!
        pool.blue shouldBe 1
        pool.restrictedMana.single().color shouldBe Color.BLUE
        pool.restrictedMana.single().obligationIds shouldBe d.state.activeManaSpendingScope(p)!!.pendingIds
        result.events.filterIsInstance<ManaAddedEvent>().any {
            it.sourceId == source && it.blue == 1
        } shouldBe true
        result.events.filterIsInstance<ManaAddedEvent>().any {
            it.sourceId != source && it.blue == 1
        } shouldBe true
    }
    test("tagged snow units pay snow alongside a color-spending substitution") {
        val context = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
        val snow = ManaSourceTag(com.wingedsheep.sdk.model.EntityId("snow"), isSnow = true)
        val pool = com.wingedsheep.engine.mechanics.mana.ManaPool(
            restrictedMana = listOf(
                unit(Color.GREEN, "a").copy(source = snow),
                unit(Color.GREEN, "b").copy(source = snow)),
            spendingColors = mapOf(Color.BLUE to setOf(Color.GREEN)))
        val paid = pool.pay(ManaCost.parse("{U}{S}"), context)!!
        paid.restrictedMana shouldBe emptyList()
        paid.dischargedObligations shouldBe setOf("a", "b")
    }

    test("zero production cannot make an activation count as a contribution") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(green = 1), emptySet())
        val tagged = tagManaObligationProduction(state, state, p)
        tagged.remainingManaObligations(p) shouldBe true
        tagged.getEntity(p)!!.get<ManaPoolComponent>()!!.restrictedMana shouldBe emptyList()
        val result = pay(d, tagged, "{G}")
        result.error.isNullOrEmpty() shouldBe false
        result.state shouldBe tagged
    }

    test("complete scoped spell payment reserves tagged snow for the snow pip") {
        val d = driver(); val p = d.activePlayer!!
        val snow = ManaSourceTag(com.wingedsheep.sdk.model.EntityId("snow"), isSnow = true)
        val state = scoped(d, ManaPoolComponent(green = 1,
            restrictedMana = listOf(unit(Color.GREEN, "snow").copy(source = snow))), setOf("snow"))
        for (strategy in listOf(PaymentStrategy.FromPool, PaymentStrategy.AutoPay, PaymentStrategy.Explicit(emptyList()))) {
            val result = pay(d, state, "{G}{S}", strategy)
            result.error shouldBe null
            result.state.remainingManaObligations(p) shouldBe false
            result.spentManaProvenance.snow shouldBe 1
            result.events.filterIsInstance<ManaSpentEvent>().single().green shouldBe 2
        }
    }
    test("fixed and restricted X are allocated together with actual X colors") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(green = 1, restrictedMana = listOf(
            unit(Color.BLUE, "blue"), unit(Color.GREEN, "green"))), setOf("blue", "green"))
        val result = CastPaymentProcessor(d.services.zones, d.services.manaSolver, d.services.costHandler,
            d.services.manaAbilitySideEffectExecutor).processPayment(
            state, CastSpell(p, p, paymentStrategy = PaymentStrategy.FromPool),
            ManaCost.parse("{G/U}{X}"), "Restricted X probe", 1,
            SpellPaymentContext(cardTypes = setOf(CardType.SORCERY)), setOf(Color.BLUE))
        result.error shouldBe null
        result.xManaSpentByColor shouldBe mapOf(Color.BLUE to 1)
        result.state.getEntity(p)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        result.state.remainingManaObligations(p) shouldBe false
    }
    test("scoped mono hybrid can pay the larger alternative to satisfy both activations") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(
            unit(Color.GREEN, "green"), unit(Color.BLUE, "blue"))), setOf("green", "blue"))
        val result = pay(d, state, "{2/G}")
        result.error shouldBe null
        result.state.remainingManaObligations(p) shouldBe false
        result.events.filterIsInstance<ManaSpentEvent>().single().let { it.green + it.blue } shouldBe 2
    }
    test("scoped mana activation reserves X color while settling prior activations") {
        val d = driver(); val p = d.activePlayer!!
        val converter = card("Allocated X Mana Probe") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Mana("{G/U}{X}")
                effect = Effects.AddMana(Color.RED, 1)
                manaAbility = true
                xManaRestriction = setOf(Color.BLUE)
            }
        }
        d.registerCards(listOf(converter))
        val source = d.putLandOnBattlefield(p, converter.name)
        d.replaceState(scoped(d, ManaPoolComponent(green = 1, restrictedMana = listOf(
            unit(Color.BLUE, "blue"), unit(Color.GREEN, "green"))), setOf("blue", "green")))
        val result = d.submit(ActivateAbility(p, source, converter.script.activatedAbilities.first().id,
            xValue = 1, paymentStrategy = PaymentStrategy.FromPool))
        result.error shouldBe null
        d.state.activeManaSpendingScope(p)!!.pendingIds.intersect(setOf("blue", "green")) shouldBe emptySet()
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.green shouldBe 1
        result.events.filterIsInstance<ManaSpentEvent>().single().let { it.blue + it.green } shouldBe 2
    }

    test("affordability recognizes a complete restricted-X allocation before partial source planning") {
        val d = driver(); val p = d.activePlayer!!
        val state = scoped(d, ManaPoolComponent(restrictedMana = listOf(
            unit(Color.BLUE, "blue"), unit(Color.GREEN, "green"))), setOf("blue", "green"))
        d.services.manaSolver.canPay(state, p, ManaCost.parse("{G/U}{X}"), xValue = 1,
            spellContext = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY)),
            xManaRestriction = setOf(Color.BLUE)) shouldBe true
    }

})
