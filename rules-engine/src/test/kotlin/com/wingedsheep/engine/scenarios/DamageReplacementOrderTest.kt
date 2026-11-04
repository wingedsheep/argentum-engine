package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.damage.DamageReplacementChain
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CombatResolutionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.player.PlayerProtectionComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.events.AmountFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class DamageReplacementOrderTest : FunSpec({
    fun host(name: String, effect: ReplacementEffect) = card(name) {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(effect)
    }
    val double = host("Order Doubler", DoubleDamage(appliesTo = EventPattern.DamageEvent()))
    val half = host("Order Halver", HalveDamage(appliesTo = EventPattern.DamageEvent()))
    val cap = host("Order Cap", CapDamage(2, EventPattern.DamageEvent()))
    val threshold = host("Order Threshold", PreventDamage(appliesTo = EventPattern.DamageEvent(
        amount = AmountFilter.AtLeast(4)
    )))
    val oneWard = host("Order One Ward", PreventDamage(amount = 1, appliesTo = EventPattern.DamageEvent()))
    val lifeWard = host("Order Life Ward", PreventDamage(
        appliesTo = EventPattern.DamageEvent(recipient = Recipient.You),
        onPrevented = Effects.GainLife(DynamicAmounts.preventedDamage())
    ))
    val mill = host("Order Mill", ReplaceDamageWithMill(
        appliesTo = EventPattern.DamageEvent(recipient = Recipient.You), miller = Player.You
    ))
    val shield = card("Order Shield Spell") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(Targets.Any); effect = Effects.PreventNextDamage(3, t) }
    }
    val largeShield = card("Order Large Shield Spell") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { val t = target(Targets.Any); effect = Effects.PreventNextDamage(5, t) }
    }
    val redirect = card("Order Redirect Spell") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val from = target(Targets.Player)
            val to = target(Targets.Player)
            effect = Effects.RedirectNextDamage(listOf(from), to)
        }
    }
    val burnEach = card("Order Burn Each") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.Each)) }
    }
    val sturdy = card("Order Sturdy Creature") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        power = 1
        toughness = 10
    }

    val counterWard = card("Order Counter Ward") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        power = 0
        toughness = 10
        replacementEffect(PreventDamagePerCounter(CounterType.PLUS_ONE_PLUS_ONE))
    }
    val limitedRedirect = card("Order Limited Redirect") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val from = target(Targets.Any)
            val to = target(Targets.Any)
            effect = Effects.RedirectNextDamage(listOf(from), to, amount = 3)
        }
    }

    val broadRedirect = card("Order Broad Redirect") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val to = target(TargetFilter.Creature)
            effect = Effects.RedirectNextDamage(listOf(EffectTarget.Controller), to,
                scope = com.wingedsheep.sdk.scripting.effects.RedirectScope.CONTINUOUS,
                protectedFilter = GameObjectFilter.Permanent.youControl())
        }
    }
    val burstWard = card("Order Burst Ward") {
        manaCost = "{0}"
        typeLine = "Creature — Test"
        power = 0
        toughness = 10
        replacementEffect(PreventDamageByRemovingCounter(removalAmount = CounterRemovalAmount.EqualToDamage,
            requiresCounter = true, onRemoved = Effects.GainLife(DynamicAmounts.triggerCountersRemoved())))
    }

    val spill = card("Order Excess Damage") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.DealDamageExcessToController(DynamicAmount.Fixed(4), creature)
        }
    }

    val counterStore = card("Order Counter Store") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(ReplaceDamageWithCounters(CounterType.PLUS_ONE_PLUS_ONE,
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.You)))
        replacementEffect(ModifyDamageAmount(dynamicModifier = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE),
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Opponent)))
    }
    val millStore = card("Order Mill Store") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        replacementEffect(ReplaceDamageWithMill(appliesTo = EventPattern.DamageEvent(recipient = Recipient.You), miller = Player.You))
        replacementEffect(ModifyDamageAmount(dynamicModifier = DynamicAmounts.cardsInYourGraveyard(),
            appliesTo = EventPattern.DamageEvent(recipient = Recipient.Opponent)))
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(double, half, cap, threshold, oneWard, lifeWard, mill, shield, redirect, burnEach, sturdy, spill, counterStore, millStore, largeShield, counterWard, limitedRedirect, broadRedirect, burstWard))
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, targets: List<EntityId> = emptyList()) {
        if (name == "Lightning Bolt") giveMana(player1, Color.RED)
        castSpell(player1, putCardInHand(player1, name), targets).error shouldBe null
        bothPass().error shouldBe null
    }

    fun GameTestDriver.choose(text: String, player: EntityId = player2) {
        val choice = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.playerId shouldBe player
        val index = choice.options.indexOfFirst { it.contains(text, ignoreCase = true) }
        (index >= 0) shouldBe true
        submitDecision(player, OptionChosenResponse(choice.id, index)).error shouldBe null
    }

    test("excess damage is split before doubling either recipient's share") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        val bear = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.cast("Order Excess Damage", listOf(bear))
        d.getLifeTotal(d.player2) shouldBe 16
        d.assertInGraveyard(d.player2, "Grizzly Bears")
        d.pendingDecision shouldBe null
    }

    for ((first, damage) in listOf("Order Doubler" to 3, "Order Halver" to 2)) {
        test("affected player chooses $first before the other modifier") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, "Order Doubler")
            d.putPermanentOnBattlefield(d.player1, "Order Halver")
            d.cast("Lightning Bolt", listOf(d.player2))
            d.choose(first)
            d.getLifeTotal(d.player2) shouldBe 20 - damage
            d.pendingDecision shouldBe null
        }
    }

    for ((first, damage) in listOf("Order Doubler" to 2, "Order Cap" to 4)) {
        test("cap and multiplication apply once in the selected order starting with $first") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, "Order Doubler")
            d.putPermanentOnBattlefield(d.player2, "Order Cap")
            d.cast("Lightning Bolt", listOf(d.player2))
            d.choose(first)
            d.getLifeTotal(d.player2) shouldBe 20 - damage
            d.pendingDecision shouldBe null
        }
    }

    test("replacement applicability is checked again after the damage amount changes") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        d.putPermanentOnBattlefield(d.player2, "Order Threshold")
        d.cast("Lightning Bolt", listOf(d.player2))
        // Initially only doubling applies; at six the threshold prevents the damage.
        d.getLifeTotal(d.player2) shouldBe 20
        d.pendingDecision shouldBe null
    }

    test("halving and prevention can reduce damage to zero without reapplying the halver") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Halver")
        d.putPermanentOnBattlefield(d.player2, "Order One Ward")
        d.cast("Lightning Bolt", listOf(d.player2))
        d.choose("Order Halver")
        d.getLifeTotal(d.player2) shouldBe 20
        d.pendingDecision shouldBe null
    }

    for ((first, cards) in listOf("Order Doubler" to 6, "Order Mill" to 3)) {
        test("damage-to-mill replacement sees the amount at its selected point after $first") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, "Order Doubler")
            d.putPermanentOnBattlefield(d.player2, "Order Mill")
            d.cast("Lightning Bolt", listOf(d.player2))
            d.choose(first)
            d.getLifeTotal(d.player2) shouldBe 20
            d.getGraveyard(d.player2).size shouldBe cards
            d.pendingDecision shouldBe null
        }
    }

    for ((first, life) in listOf("Order Life Ward" to 23, "protection" to 20)) {
        test("player can choose $first when protection competes with a prevention rider") {
            val d = driver()
            d.putPermanentOnBattlefield(d.player1, "Order Life Ward")
            d.replaceState(d.state.updateEntity(d.player1) { it.with(
                PlayerProtectionComponent(scopes = listOf(ProtectionScope.Everything))
            ) })
            d.cast("Order Burn Each")
            d.choose(first, d.player1)
            d.getLifeTotal(d.player1) shouldBe life
            d.getLifeTotal(d.player2) shouldBe 17
            d.pendingDecision shouldBe null
        }
    }

    test("redirection transfers the choice to the new damaged player") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        d.putPermanentOnBattlefield(d.player2, "Order Cap")
        d.cast("Order Redirect Spell", listOf(d.player1, d.player2))
        d.cast("Lightning Bolt", listOf(d.player1))
        d.choose("redirect", d.player1)
        d.choose("Order Cap", d.player2)
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 16
        d.pendingDecision shouldBe null
    }

    test("a multiplier applied before redirection is not applied again to the redirected event") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        d.putPermanentOnBattlefield(d.player2, "Order Cap")
        d.cast("Order Redirect Spell", listOf(d.player1, d.player2))
        d.cast("Lightning Bolt", listOf(d.player1))
        d.choose("Order Doubler", d.player1)
        d.choose("redirect", d.player1)
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 18
        d.pendingDecision shouldBe null
    }

    test("a shield counter remains when another prevention effect consumes the whole event first") {
        val d = driver()
        val recipient = d.putCreatureOnBattlefield(d.player2, "Order Sturdy Creature")
        d.replaceState(d.state.updateEntity(recipient) { it.with(CountersComponent().withAdded(CounterType.SHIELD, 1)) })
        d.cast("Order Shield Spell", listOf(recipient))
        d.cast("Lightning Bolt", listOf(recipient))
        // The finite shield is spent, so the shield counter should not also be removed.
        d.choose("prevent 3")
        d.state.getEntity(recipient)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) shouldBe 1
        (d.state.getEntity(recipient)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
    }

    test("simultaneous combat sources spend a shared finite shield only once across paused choices") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        val attackers = List(2) { d.putCreatureOnBattlefield(d.player1, "Centaur Courser") }
        attackers.forEach(d::removeSummoningSickness)
        d.cast("Order Shield Spell", listOf(d.player2))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2).error shouldBe null
        d.bothPass()
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null
        d.choose("prevent up to 3")
        val allocation = d.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        d.submitDecision(allocation.playerId, com.wingedsheep.engine.core.NumberChosenResponse(allocation.id, 3)).error shouldBe null
        // One three-point source is prevented before doubling; the other deals six.
        d.getLifeTotal(d.player2) shouldBe 14
        d.pendingDecision shouldBe null
    }
    test("simultaneous counter replacement results cannot resize another source before the event happens") {
        val d = driver()
        val store = d.putPermanentOnBattlefield(d.player1, "Order Counter Store")
        d.cast("Order Burn Each")
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 17
        d.state.getEntity(store)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
    }

    test("simultaneous milling results cannot resize later damage in the same event") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Mill Store")
        d.cast("Order Burn Each")
        d.pendingDecision shouldBe null
        d.getLifeTotal(d.player1) shouldBe 20
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("a shared shield reserves its exact total and rejects leaving prevention unassigned") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        val attackers = List(2) { d.putCreatureOnBattlefield(d.player1, "Centaur Courser") }
        attackers.forEach(d::removeSummoningSickness)
        d.cast("Order Large Shield Spell", listOf(d.player2))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2).error shouldBe null
        d.bothPass()
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null
        d.choose("prevent up to 5")
        val allocation = d.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        allocation.minValue shouldBe 2
        allocation.maxValue shouldBe 3
        // The other source can absorb at most three, so reserving all five for it is illegal.
        val rejected = d.submitDecision(allocation.playerId, com.wingedsheep.engine.core.NumberChosenResponse(allocation.id, 0))
        (rejected.error != null) shouldBe true
        d.submitDecision(allocation.playerId, com.wingedsheep.engine.core.NumberChosenResponse(allocation.id, 2)).error shouldBe null
        // Exactly five points are prevented before doubling the single surviving point.
        d.getLifeTotal(d.player2) shouldBe 18
        d.pendingDecision shouldBe null
    }

    test("a later simultaneous source may be doubled before allocating a shared shield") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        val attackers = listOf(
            d.putCreatureOnBattlefield(d.player1, "Grizzly Bears"),
            d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        )
        attackers.forEach(d::removeSummoningSickness)
        d.cast("Order Shield Spell", listOf(d.player2))
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, attackers, d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.player2).error shouldBe null
        d.bothPass()
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null
        val order = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val laterDouble = order.options.indexOfFirst { it.contains("double", true) && it.contains("Centaur Courser") }
        (laterDouble >= 0) shouldBe true
        d.submitDecision(order.playerId, OptionChosenResponse(order.id, laterDouble)).error shouldBe null
        d.choose("prevent up to 3")
        val allocation = d.pendingDecision.shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        allocation.prompt.contains("Centaur Courser") shouldBe true
        d.submitDecision(allocation.playerId, com.wingedsheep.engine.core.NumberChosenResponse(allocation.id, 1)).error shouldBe null
        // Prevent one of the Courser's doubled six and both of the Bears' original two.
        d.getLifeTotal(d.player2) shouldBe 15
        d.pendingDecision shouldBe null
    }

    test("counter prevention may split its capacity between simultaneous sources") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player2, "Order Counter Ward")
        d.replaceState(d.state.updateEntity(ward) { it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3)) })
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val incoming = listOf(first, second).map { DamageReplacementChain.Instance(it, ward, 3) }
        val ask = DamageReplacementChain.resolve(d.zones, d.state, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Ask>()
        val number = ask.question("allocation").shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        number.minValue shouldBe 0
        number.maxValue shouldBe 3
        val recorded = DamageReplacementChain.record(ask.state, ask.choiceKey, ask.numberPrefix!! + "1")
        val done = DamageReplacementChain.resolve(d.zones, recorded, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Done>()
        done.instances.associate { it.sourceId to it.amount } shouldBe mapOf(first to 2, second to 1)
        done.state.getEntity(ward)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
    }

    test("finite redirection may split its capacity between simultaneous sources") {
        val d = driver()
        d.cast("Order Limited Redirect", listOf(d.player2, d.player1))
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val incoming = listOf(first, second).map { DamageReplacementChain.Instance(it, d.player2, 3) }
        val ask = DamageReplacementChain.resolve(d.zones, d.state, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Ask>()
        ask.question("allocation").shouldBeInstanceOf<com.wingedsheep.engine.core.ChooseNumberDecision>()
        val recorded = DamageReplacementChain.record(ask.state, ask.choiceKey, ask.numberPrefix!! + "1")
        val done = DamageReplacementChain.resolve(d.zones, recorded, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Done>()
        done.instances.associate { (it.sourceId to it.targetId) to it.amount } shouldBe mapOf(
            (first to d.player1) to 1, (first to d.player2) to 2,
            (second to d.player1) to 2, (second to d.player2) to 1
        )
    }

    test("whole-event redirects keep simultaneous sources together after a source-specific multiplier") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Order Doubler")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(d.player1, "Hill Giant")
        d.cast("Order Broad Redirect", listOf(bears))
        d.cast("Order Broad Redirect", listOf(giant))
        val sourceOne = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        val sourceTwo = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val incoming = listOf(sourceOne, sourceTwo).map { DamageReplacementChain.Instance(it, d.player1, 3) }
        var state = d.state
        for ((action, recipient) in listOf("double" to "Centaur Courser", "redirect" to "Grizzly Bears", "redirect" to "Hill Giant")) {
            val ask = DamageReplacementChain.resolve(d.zones, state, incoming, inBatch = true)
                .shouldBeInstanceOf<DamageReplacementChain.Result.Ask>()
            val decision = ask.question("order").shouldBeInstanceOf<ChooseOptionDecision>()
            val choice = decision.options.indexOfFirst { it.contains(action, ignoreCase = true) && it.contains(recipient) }
            (choice >= 0) shouldBe true
            state = DamageReplacementChain.record(ask.state, ask.choiceKey, ask.optionKeys[choice])
        }
        val done = DamageReplacementChain.resolve(d.zones, state, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Done>()
        done.instances.size shouldBe 2
        done.instances.map { it.targetId }.toSet() shouldBe setOf(giant)
        done.instances.associate { it.sourceId to it.amount } shouldBe mapOf(sourceOne to 6, sourceTwo to 6)
    }

    test("whole-event counter prevention removes counters and queues its rider once") {
        val d = driver()
        val ward = d.putCreatureOnBattlefield(d.player2, "Order Burst Ward")
        d.replaceState(d.state.updateEntity(ward) { it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 6)) })
        val first = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val second = d.putCreatureOnBattlefield(d.player1, "Centaur Courser")
        val incoming = listOf(DamageReplacementChain.Instance(first, ward, 2), DamageReplacementChain.Instance(second, ward, 3))
        val done = DamageReplacementChain.resolve(d.zones, d.state, incoming, inBatch = true)
            .shouldBeInstanceOf<DamageReplacementChain.Result.Done>()
        done.instances shouldBe emptyList()
        done.events.filterIsInstance<com.wingedsheep.engine.core.CountersRemovedEvent>().single().amount shouldBe 5
        done.state.pendingReplacementRiders.single().countersRemoved shouldBe 5
        done.state.getEntity(ward)!!.get<CountersComponent>()!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
    }

})
