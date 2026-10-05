package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ForcedManaPaymentSafetyTest : FunSpec({
    val spell = card("Safety Paid Probe") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell { effect = Effects.GainLife(1) }
    }
    val zero = card("Safety Zero Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 0); manaAbility = true }
    }
    val surplus = card("Safety Surplus Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 3); manaAbility = true }
    }
    val choice = card("Safety Color Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddAnyColorMana(1); manaAbility = true }
    }
    val parts = card("Safety Multipart Producer") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.Composite(listOf(Effects.AddAnyColorMana(1), Effects.AddAnyColorMana(1)))
            manaAbility = true
        }
    }
    val sacrifice = card("Safety Sacrifice Producer") {
        typeLine = "Land"
        activatedAbility { cost = Costs.SacrificeSelf; effect = Effects.AddMana(Color.GREEN, 0); manaAbility = true }
    }
    val converter = card("Safety Paid Converter") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.Mana("{G}"))
            effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(spell, zero, surplus, choice, parts, sacrifice, converter))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    // Reuse the ordinary announced-cast window. This prerequisite does not register a partial Word of Command.
    fun window(d: GameTestDriver, scoped: Boolean = true, forced: Boolean = true, announcedSpell: CardDefinition = spell): EntityId {
        val player = d.activePlayer!!
        val chosen = d.putCardInHand(player, announcedSpell.name)
        val context = EffectContext(sourceId = null, controllerId = player,
            pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(chosen))))
        var state = d.state
        if (scoped) state = state.pushContinuation(ManaSpendingObligationsContinuation(player, context, "safety"))
        if (forced) state = state.pushContinuation(FinishForcedPlayContinuation(
            state.objectRef(chosen)!!, player, "chosen", null, context))
        val cost = announcedSpell.manaCost
        val paymentContext = SpellPaymentContext(cardTypes = setOf(CardType.SORCERY))
        state = state.suspendForDecision(
            question = { id -> ManaPaymentWindow.buildDecision(state, player, cost, id, "Pay for chosen card",
                DecisionContext(sourceId = chosen, phase = DecisionPhase.CASTING), false,
                d.services.manaSolver, spellContext = paymentContext) },
            answer = ManaActionPaymentContinuation(CastSpell(player, chosen), cost,
                lockedCastCost = cost, paymentContext = paymentContext)).state
        d.replaceState(state)
        return chosen
    }
    fun activate(d: GameTestDriver, source: EntityId): ExecutionResult {
        val action = d.services.legalActionEnumerator.enumerateManaAbilities(d.state, d.activePlayer!!)
            .map { it.action }.filterIsInstance<ActivateAbility>().first { it.sourceId == source }
        return d.submit(action)
    }
    fun pay(d: GameTestDriver) = d.submitDecision(d.activePlayer!!,
        ManaSourcesSelectedResponse(d.pendingDecision!!.id, emptyList(), autoPay = true))

    test("zero output rejects atomically and the same payment can still complete") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, zero.name); val forest = d.putLandOnBattlefield(p, "Forest")
        val chosen = window(d); val before = d.state
        val result = activate(d, source)
        result.error!!.contains("prevent paying") shouldBe true
        result.state shouldBe before; result.events shouldBe emptyList(); d.state shouldBe before
        pay(d).error shouldBe null
        (chosen in d.state.stack) shouldBe true
        d.state.getEntity(source)!!.has<TappedComponent>() shouldBe false
        d.state.getEntity(forest)!!.has<TappedComponent>() shouldBe true
    }
    test("a sacrificed zero-output source is restored on rejection") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, sacrifice.name); d.putLandOnBattlefield(p, "Forest")
        window(d); val before = d.state
        activate(d, source).error!!.contains("prevent paying") shouldBe true
        d.state shouldBe before; (source in d.state.getBattlefield()) shouldBe true
    }
    test("a second activation cannot exceed the number of payable contributions") {
        val d = driver(); val p = d.activePlayer!!
        val first = d.putLandOnBattlefield(p, "Forest"); val second = d.putLandOnBattlefield(p, "Forest")
        val chosen = window(d)
        activate(d, first).error shouldBe null
        val before = d.state
        activate(d, second).error!!.contains("prevent paying") shouldBe true
        d.state shouldBe before
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
        d.state.getEntity(second)!!.has<TappedComponent>() shouldBe false
    }
    test("excess from one contributing activation remains after paying") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, surplus.name)
        val chosen = window(d)
        activate(d, source).error shouldBe null
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
        d.state.getEntity(d.activePlayer!!)!!.get<ManaPoolComponent>()!!.total shouldBe 2
    }
    test("a wrong color answer is rejected while its payable alternative remains live") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, choice.name)
        val chosen = window(d)
        activate(d, source).error shouldBe null
        (d.pendingDecision is ChooseColorDecision) shouldBe true
        val before = d.state; val id = d.pendingDecision!!.id
        val rejected = d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.RED))
        rejected.error!!.contains("prevent paying") shouldBe true
        rejected.events shouldBe emptyList(); d.state shouldBe before; d.pendingDecision!!.id shouldBe id
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.GREEN)).error shouldBe null
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
    }
    test("a first multipart choice must be completed before its pool counts as a proof") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, parts.name)
        val chosen = window(d)
        activate(d, source).error shouldBe null
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(d.pendingDecision!!.id, Color.RED)).error shouldBe null
        val before = d.state; val id = d.pendingDecision!!.id
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.RED)).error!!.contains("prevent paying") shouldBe true
        d.state shouldBe before
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.GREEN)).error shouldBe null
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
        d.state.getEntity(d.activePlayer!!)!!.get<ManaPoolComponent>()!!.total shouldBe 1
    }
    test("a sacrifice cost answer preserves the source needed for final payment and permits retry") {
        val d = driver(); val p = d.activePlayer!!
        val paid = card("Safety Two Color Probe") {
            manaCost = "{G}{U}"; typeLine = "Sorcery"
            spell { effect = Effects.GainLife(1) }
        }
        val green = card("Safety Green Creature") {
            typeLine = "Creature — Bear"; power = 2; toughness = 2
            activatedAbility { cost = Costs.Tap; effect = Effects.AddMana(Color.GREEN, 1); manaAbility = true }
        }
        val blue = card("Safety Sacrifice For Blue") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Sacrifice(GameObjectFilter.Creature)
                effect = Effects.AddMana(Color.BLUE, 1); manaAbility = true
            }
        }
        d.registerCards(listOf(paid, green, blue))
        val keep = d.putCreatureOnBattlefield(p, green.name)
        d.replaceState(d.state.updateEntity(keep) { it.without<SummoningSicknessComponent>() })
        val victim = d.putCreatureOnBattlefield(p, "Grizzly Bears")
        val source = d.putLandOnBattlefield(p, blue.name)
        val chosen = window(d, announcedSpell = paid)
        activate(d, source).error shouldBe null
        (d.pendingDecision is SelectCardsDecision) shouldBe true
        val before = d.state; val id = d.pendingDecision!!.id
        val rejected = d.submitDecision(p, CardsSelectedResponse(id, listOf(keep)))
        rejected.error!!.contains("prevent paying") shouldBe true
        rejected.state shouldBe before; rejected.events shouldBe emptyList()
        d.state shouldBe before; d.pendingDecision!!.id shouldBe id
        d.submitDecision(p, CardsSelectedResponse(id, listOf(victim))).error shouldBe null
        (keep in d.state.getBattlefield()) shouldBe true
        (victim in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
        d.state.getEntity(keep)!!.has<TappedComponent>() shouldBe true
    }
    test("paying a converter settles the feeder and leaves one final contribution") {
        val d = driver(); val p = d.activePlayer!!
        val forest = d.putLandOnBattlefield(p, "Forest"); val convert = d.putLandOnBattlefield(p, converter.name)
        val chosen = window(d)
        activate(d, forest).error shouldBe null
        activate(d, convert).error shouldBe null
        d.state.activeManaSpendingScope(p)!!.pendingIds.size shouldBe 1
        pay(d).error shouldBe null; (chosen in d.state.stack) shouldBe true
    }
    test("explicit payment selection executes only a complete contributing subset") {
        val d = driver(); val p = d.activePlayer!!
        val first = d.putLandOnBattlefield(p, "Forest"); val second = d.putLandOnBattlefield(p, "Forest")
        val chosen = window(d); val id = d.pendingDecision!!.id
        val result = d.submitDecision(p, ManaSourcesSelectedResponse(id, listOf(first, second)))
        result.error shouldBe null
        (chosen in d.state.stack) shouldBe true
        listOf(first, second).count { d.state.getEntity(it)!!.has<TappedComponent>() } shouldBe 1
        result.events.filterIsInstance<AbilityActivatedEvent>().size shouldBe 1
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.total shouldBe 0
    }
    test("a doomed production pause rejects before paying its activation cost") {
        val d = driver(); val p = d.activePlayer!!
        val redOnly = card("Safety Red Choices") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.AddManaOfChoice(com.wingedsheep.sdk.scripting.values.ManaColorSet.Specific(setOf(Color.RED, Color.BLUE)), 1)
                manaAbility = true
            }
        }
        d.registerCards(listOf(redOnly)); val source = d.putLandOnBattlefield(p, redOnly.name)
        d.putLandOnBattlefield(p, "Forest"); window(d); val before = d.state
        val result = activate(d, source)
        result.error!!.contains("prevent paying") shouldBe true
        result.events shouldBe emptyList(); d.state shouldBe before
    }

    test("ordinary unscoped payment permits manual excess activations") {
        val d = driver(); val p = d.activePlayer!!
        val first = d.putLandOnBattlefield(p, "Forest"); val second = d.putLandOnBattlefield(p, "Forest")
        window(d, scoped = false)
        activate(d, first).error shouldBe null
        activate(d, second).error shouldBe null
    }
    test("a spending scope without a forced instruction retains its existing behavior") {
        val d = driver(); val p = d.activePlayer!!
        val source = d.putLandOnBattlefield(p, zero.name)
        window(d, forced = false)
        activate(d, source).error shouldBe null
        d.state.getEntity(source)!!.has<TappedComponent>() shouldBe true
    }
    test("uncertain production rejects without publishing its activation or costs") {
        val d = driver(); val p = d.activePlayer!!
        val unsupported = card("Safety Optional Producer") {
            typeLine = "Land"
            activatedAbility {
                cost = Costs.Tap
                effect = Effects.Composite(listOf(Effects.AddAnyColorMana(1), Effects.May(Effects.GainLife(1))))
                manaAbility = true
            }
        }
        d.registerCards(listOf(unsupported)); val source = d.putLandOnBattlefield(p, unsupported.name)
        d.putLandOnBattlefield(p, "Forest"); window(d); val before = d.state
        val result = activate(d, source)
        result.error!!.contains("could not be verified") shouldBe true
        result.events shouldBe emptyList(); d.state shouldBe before
    }
    test("uncaptured X and Phyrexian choices cannot establish a weaker manual proof") {
        for (price in listOf("{X}{G}", "{G/P}")) {
            val d = driver(); val p = d.activePlayer!!
            val source = d.putLandOnBattlefield(p, "Forest"); window(d)
            val suspension = d.state.peekContinuation() as Suspension
            val payment = suspension.answer as ManaActionPaymentContinuation
            d.replaceState(d.state.copy(continuationStack = d.state.continuationStack.dropLast(1) +
                suspension.copy(answer = payment.copy(cost = ManaCost.parse(price).withXAs(0),
                    lockedCastCost = ManaCost.parse(price)))))
            val before = d.state
            val result = activate(d, source)
            result.error!!.contains("could not be verified") shouldBe true
            result.events shouldBe emptyList(); d.state shouldBe before
        }
    }

    test("paused manual production survives serialization and keeps atomic retry") {
        val d = driver(); val source = d.putLandOnBattlefield(d.activePlayer!!, choice.name)
        window(d); activate(d, source).error shouldBe null
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        val before = d.state; val id = d.pendingDecision!!.id
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.BLUE)).error!!.contains("prevent paying") shouldBe true
        d.state shouldBe before
        d.submitDecision(d.activePlayer!!, ColorChosenResponse(id, Color.GREEN)).error shouldBe null
    }
})
