package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.stack.CopyTargetSpellExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json

class SpellCopyCharacteristicsTest : FunSpec({
    val red = CopyExceptions(overrideColors = setOf(Color.RED))
    val blueBolt = card("Test Blue Copy Bolt") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.DealDamage(2, t)
        }
    }
    val noTargets = card("Test Blue Copy Life") {
        manaCost = "{U}"; typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }
    val modal = card("Test Blue Copy Modes") {
        manaCost = "{U}"; typeLine = "Instant"
        spell {
            modal(chooseCount = 2) {
                mode("Damage a creature") {
                    val t = target(TargetFilter.Creature)
                    effect = Effects.DealDamage(1, t)
                }
                mode("Gain life") { effect = Effects.GainLife(3) }
            }
        }
    }
    val guards = listOf(Color.RED, Color.BLUE).map { color ->
        card("Test ${color.name} Copy Guard") {
            manaCost = "{W}"; typeLine = "Creature — Soldier"; power = 2; toughness = 4
            keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(color)))
        }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(blueBolt, noTargets, modal) + guards)
        it.initMirrorMatch(Deck.of("Island" to 40), startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun card(d: GameTestDriver, id: EntityId) = d.state.getEntity(id)!!.get<CardComponent>()!!
    fun cast(d: GameTestDriver, name: String, targets: List<EntityId> = emptyList()): EntityId {
        val id = d.putCardInHand(d.player1, name)
        d.giveMana(d.player1, Color.BLUE, 5)
        d.giveMana(d.player1, Color.GREEN, 5)
        d.castSpell(d.player1, id, targets).error shouldBe null
        return id
    }
    fun castModal(d: GameTestDriver, target: EntityId): EntityId {
        val id = d.putCardInHand(d.player1, modal.name)
        d.giveMana(d.player1, Color.BLUE)
        val targets = listOf(ChosenTarget.Permanent(target))
        d.submit(CastSpell(playerId = d.player1, cardId = id, targets = targets,
            paymentStrategy = PaymentStrategy.FromPool, chosenModes = listOf(0, 1),
            modeTargetsOrdered = listOf(targets, emptyList()))).error shouldBe null
        return id
    }
    fun copy(d: GameTestDriver, source: EntityId, exceptions: CopyExceptions = red, count: Int = 1): EffectResult {
        val predicates = PredicateEvaluator(cardRegistry = d.cardRegistry)
        val result = CopyTargetSpellExecutor(predicates.conditions.amounts, TargetFinder(predicates)).execute(
            d.state, CopyTargetSpellEffect(EffectTarget.ContextTarget(0), copies = DynamicAmount.Fixed(count), exceptions = exceptions),
            EffectContext(sourceId = null, controllerId = d.player2, targets = listOf(ChosenTarget.Spell(source)))
        )
        result.error shouldBe null
        d.replaceState(result.state)
        return result
    }
    fun copies(d: GameTestDriver) = d.state.stack.filter { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
    fun roundTrip(d: GameTestDriver) {
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        d.replaceState(json.decodeFromString(GameState.serializer(), json.encodeToString(GameState.serializer(), d.state)))
    }
    test("a color exception keeps the mana cost and source intact and emits copying rather than casting") {
        val d = driver(); val source = cast(d, noTargets.name); val original = card(d, source)
        val result = copy(d, source)
        val c = copies(d).single()
        card(d, c).colors shouldBe setOf(Color.RED)
        card(d, c).manaCost shouldBe original.manaCost
        card(d, c).ownerId shouldBe d.player2
        card(d, source) shouldBe original
        result.events.filterIsInstance<SpellCopiedEvent>().single().copyEntityId shouldBe c
        result.events.filterIsInstance<SpellCastEvent>().size shouldBe 0
        result.events.filterIsInstance<ManaSpentEvent>().size shouldBe 0
        d.bothPass().error shouldBe null
        d.getLifeTotal(d.player2) shouldBe 21
        (c in d.state.stack) shouldBe false
        (source in d.state.stack) shouldBe true
    }
    test("a plain copy of an excepted copy inherits red and not an extra exception") {
        val d = driver(); val source = cast(d, noTargets.name)
        copy(d, source); val first = copies(d).single()
        copy(d, first, CopyExceptions.None)
        copies(d).size shouldBe 2
        copies(d).forEach { card(d, it).colors shouldBe setOf(Color.RED) }
        card(d, source).colors shouldBe setOf(Color.BLUE)
    }
    test("overrides replace multicolor and remove copied devoid while additions retain other colors") {
        val d = driver(); val source = cast(d, noTargets.name)
        d.replaceState(d.state.updateEntity(source) { it.with(card(d, source).copy(
            colors = setOf(Color.BLUE, Color.GREEN), baseKeywords = setOf(Keyword.DEVOID))) })
        copy(d, source)
        card(d, copies(d).single()).baseKeywords.contains(Keyword.DEVOID) shouldBe false
        card(d, copies(d).single()).colors shouldBe setOf(Color.RED)
        copy(d, source, CopyExceptions(addedColors = setOf(Color.WHITE)))
        card(d, copies(d).last()).colors shouldBe setOf(Color.BLUE, Color.GREEN, Color.WHITE)
    }
    test("a colorless override stays empty rather than falling back to mana colors") {
        val d = driver(); val source = cast(d, noTargets.name)
        copy(d, source, CopyExceptions(overrideColors = emptySet()))
        card(d, copies(d).single()).colors shouldBe emptySet()
        card(d, copies(d).single()).manaCost shouldBe ManaCost.parse("{U}")
    }
    test("zero copies do not allocate or prompt and uncopyable spells do not make a copy") {
        val d = driver(); val source = cast(d, noTargets.name); val before = d.state
        copy(d, source, count = 0).events shouldBe emptyList()
        d.state shouldBe before
        d.replaceState(d.state.updateEntity(source) { it.with(CantBeCopiedComponent) })
        copy(d, source).events shouldBe emptyList()
        copies(d) shouldBe emptyList()
    }
    test("new targets use the red copy rather than the blue original or the copying effect") {
        val d = driver()
        val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val redGuard = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        val blueGuard = d.putPermanentOnBattlefield(d.player1, guards[1].name)
        val source = cast(d, blueBolt.name, listOf(bear))
        copy(d, source)
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        (redGuard in decision.legalTargets.getValue(0)) shouldBe false
        (blueGuard in decision.legalTargets.getValue(0)) shouldBe true
        (bear in decision.legalTargets.getValue(0)) shouldBe true
        d.submitTargetSelection(d.player2, listOf(blueGuard)).error shouldBe null
        val c = copies(d).single()
        card(d, c).colors shouldBe setOf(Color.RED)
        d.state.getEntity(c)!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(blueGuard))
        card(d, source).colors shouldBe setOf(Color.BLUE)
        d.bothPass().error shouldBe null
        d.state.getEntity(blueGuard)!!.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()!!.amount shouldBe 2
    }
    test("every targeted copy keeps its exceptions after serialized continuation and independent choices") {
        val d = driver(); val a = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val b = d.putPermanentOnBattlefield(d.player1, "Hill Giant")
        val redGuard = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        val source = cast(d, blueBolt.name, listOf(a))
        copy(d, source, count = 2)
        roundTrip(d)
        d.submitTargetSelection(d.player2, listOf(a)).error shouldBe null
        (redGuard in d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>().legalTargets.getValue(0)) shouldBe false
        roundTrip(d)
        d.submitTargetSelection(d.player2, listOf(b)).error shouldBe null
        copies(d).size shouldBe 2
        copies(d).forEach { card(d, it).colors shouldBe setOf(Color.RED) }
        copies(d).map { d.state.getEntity(it)!!.get<TargetsComponent>()!!.targets }.toSet() shouldBe
            setOf(listOf(ChosenTarget.Permanent(a)), listOf(ChosenTarget.Permanent(b)))
    }
    test("the no-replacement path still creates red copies carrying the original targets") {
        val d = driver(); val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = cast(d, blueBolt.name, listOf(bear))
        d.replaceState(d.state.removeEntity(bear).copy(zones = d.state.zones.mapValues { (_, ids) -> ids - bear }))
        copy(d, source, count = 2)
        d.state.pendingDecision shouldBe null
        copies(d).size shouldBe 2
        copies(d).forEach {
            card(d, it).colors shouldBe setOf(Color.RED)
            d.state.getEntity(it)!!.get<TargetsComponent>()!!.targets shouldBe listOf(ChosenTarget.Permanent(bear))
        }
    }
    test("name and mana-cost exceptions are reflected in the placement event and remain copiable") {
        val d = driver(); val source = cast(d, noTargets.name)
        val result = copy(d, source, red.copy(nameOverride = "Renamed Copy", noManaCost = true))
        val c = copies(d).single()
        card(d, c).name shouldBe "Renamed Copy"
        card(d, c).manaValue shouldBe 0
        val event = result.events.filterIsInstance<SpellCopiedEvent>().single()
        event.cardName shouldBe "Renamed Copy"
        event.manaValue shouldBe 0
        card(d, source).name shouldBe noTargets.name
    }
    test("X additional-cost choices and modes survive characteristic exceptions") {
        val d = driver(); val source = cast(d, noTargets.name)
        val old = d.state.getEntity(source)!!.get<SpellOnStackComponent>()!!
        val spell = old.copy(xValue = 7, declaredCostSlot = com.wingedsheep.sdk.scripting.ChoiceSlot.KICKED,
            chosenModes = listOf(1, 0), modeTargetsOrdered = listOf(emptyList(), emptyList()))
        d.replaceState(d.state.updateEntity(source) { it.with(spell) })
        copy(d, source)
        val copied = d.state.getEntity(copies(d).single())!!.get<SpellOnStackComponent>()!!
        copied.xValue shouldBe 7
        copied.declaredCostSlot shouldBe spell.declaredCostSlot
        copied.chosenModes shouldBe listOf(1, 0)
        copied.castFromZone shouldBe null
        copied.manaSpentBlue shouldBe 0
    }
    test("modal target pauses carry exceptions across every copy and preserve chosen modes") {
        val d = driver(); val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val redGuard = d.putPermanentOnBattlefield(d.player1, guards[0].name)
        val source = castModal(d, bear)
        copy(d, source, count = 2)
        repeat(2) {
            val question = d.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            (redGuard in question.legalTargets.getValue(0)) shouldBe false
            roundTrip(d)
            d.submitTargetSelection(d.player2, listOf(bear)).error shouldBe null
        }
        copies(d).size shouldBe 2
        copies(d).forEach {
            card(d, it).colors shouldBe setOf(Color.RED)
            d.state.getEntity(it)!!.get<SpellOnStackComponent>()!!.chosenModes shouldBe listOf(0, 1)
        }
    }
    test("modal no-legal-target fallback also applies exceptions") {
        val d = driver(); val bear = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
        val source = castModal(d, bear)
        d.replaceState(d.state.removeEntity(bear).copy(zones = d.state.zones.mapValues { (_, ids) -> ids - bear }))
        copy(d, source)
        d.state.pendingDecision shouldBe null
        card(d, copies(d).single()).colors shouldBe setOf(Color.RED)
    }
    for (removeBeforeEntry in listOf(false, true)) {
        for (copyAgain in listOf(false, true)) {
            test("bestow exceptions survive host removal before entry $removeBeforeEntry and recopy $copyAgain") {
                val d = driver()
                val spirit = card("Test Excepted Bestow Spirit") {
                    manaCost = "{U}"; typeLine = "Enchantment Creature — Spirit"; power = 2; toughness = 3
                    keywordAbility(KeywordAbility.bestow("{0}"))
                }
                val removal = card("Test Bestow Copy Host Removal") {
                    manaCost = "{0}"; typeLine = "Instant"
                    spell {
                        val host = target(TargetFilter.Creature)
                        effect = Effects.Move(host, Zone.GRAVEYARD)
                    }
                }
                d.registerCards(listOf(spirit, removal))
                val host = d.putPermanentOnBattlefield(d.player1, "Grizzly Bears")
                val source = d.putCardInHand(d.player1, spirit.name)
                d.submit(CastSpell(playerId = d.player1, cardId = source,
                    targets = listOf(ChosenTarget.Permanent(host)), useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.BESTOW,
                    paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null
                val exceptions = red.copy(powerOverride = 4, toughnessOverride = 5,
                    addedCardTypes = setOf(CardType.ARTIFACT), addedSubtypes = setOf(Subtype.GOLEM))
                copy(d, source, exceptions)
                d.submitTargetSelection(d.player2, listOf(host)).error shouldBe null
                var copied = copies(d).single()
                if (copyAgain) {
                    copy(d, copied, CopyExceptions.None)
                    d.submitTargetSelection(d.player2, listOf(host)).error shouldBe null
                    copied = copies(d).last()
                }
                if (!removeBeforeEntry) {
                    d.bothPass().error shouldBe null
                    d.state.getEntity(copied)!!.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()!!.targetId shouldBe host
                    card(d, copied).baseStats!!.basePower shouldBe 4
                    card(d, copied).typeLine.cardTypes.contains(CardType.ARTIFACT) shouldBe true
                }
                val removingPlayer = d.priorityPlayer!!
                val removalId = d.putCardInHand(removingPlayer, removal.name)
                d.castSpell(removingPlayer, removalId, listOf(host)).error shouldBe null
                d.bothPass().error shouldBe null
                if (removeBeforeEntry) d.bothPass().error shouldBe null
                d.state.getEntity(copied)!!.has<TokenComponent>() shouldBe true
                d.state.projectedState.getPower(copied) shouldBe 4
                d.state.projectedState.getToughness(copied) shouldBe 5
                d.state.projectedState.hasType(copied, "ARTIFACT") shouldBe true
                d.state.projectedState.hasSubtype(copied, "Golem") shouldBe true
                card(d, copied).colors shouldBe setOf(Color.RED)
                card(d, source).colors shouldBe setOf(Color.BLUE)
            }
        }
    }
    test("permanent spell exceptions survive becoming a token and a subsequent spell copy") {
        val d = driver()
        val source = cast(d, "Grizzly Bears")
        val e = red.copy(powerOverride = 4, toughnessOverride = 5, addedKeywords = setOf(Keyword.HASTE),
            addedNumericKeywords = listOf(KeywordAbility.Numeric(Keyword.TOXIC, 2)))
        copy(d, source, e)
        val first = copies(d).single()
        copy(d, first, CopyExceptions.None)
        val last = copies(d).last()
        d.bothPass().error shouldBe null
        val token = d.state.getEntity(last)!!
        token.has<TokenComponent>() shouldBe true
        d.state.projectedState.getPower(last) shouldBe 4
        d.state.projectedState.getToughness(last) shouldBe 5
        card(d, last).colors shouldBe setOf(Color.RED)
        card(d, last).baseKeywords.contains(Keyword.HASTE) shouldBe true
        token.get<ToxicComponent>()!!.amount shouldBe 2
        card(d, source).baseStats!!.basePower shouldBe 2
    }

})
