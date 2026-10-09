package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.Enlist
import com.wingedsheep.sdk.scripting.AttackTax
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf

/** Optional attack costs, independent instances, live power, departure identity, and real stack timing. */
class EnlistTest : FunSpec({
    fun enlisting(name: String, instances: Int = 1, payoff: Boolean = false) = card(name) {
        typeLine = "Creature — Soldier"; power = 1; toughness = 4
        repeat(instances) { staticAbility { ability = Enlist } }
        if (payoff) triggeredAbility { trigger = Triggers.self.enlists(); effect = Effects.GainLife(2) }
    }
    val elixir = card("Enlist Haste Elixir") {
        typeLine = "Artifact"
        staticAbility {
            ability = com.wingedsheep.sdk.scripting.GrantKeyword(
                AbilityFlag.MAY_ACTIVATE_ABILITIES_AS_THOUGH_HASTY.name,
                com.wingedsheep.sdk.scripting.filters.unified.GroupFilter.AllCreaturesYouControl,
            )
        }
    }
    val tapWatcher = card("Enlist Tap Watcher") {
        typeLine = "Creature — Soldier"; power = 1; toughness = 4
        staticAbility { ability = Enlist }
        triggeredAbility { trigger = Triggers.self.becomesTapped(); effect = Effects.GainLife(2) }
    }
    val manaRock = card("Enlist Mana Rock") {
        typeLine = "Artifact"
        activatedAbility { cost = Costs.Tap; effect = Effects.AddColorlessMana(1); manaAbility = true }
        triggeredAbility { trigger = Triggers.self.becomesTapped(); effect = Effects.GainLife(3) }
    }
    val recruit = enlisting("Enlist Recruit")
    val double = enlisting("Double Enlist", 2)
    val watcher = enlisting("Enlist Watcher", payoff = true)
    val haste = card("Hasty Helper") {
        typeLine = "Creature — Scout"; power = 3; toughness = 3; keywords(Keyword.HASTE)
    }
    val negative = card("Negative Helper") { typeLine = "Creature — Wall"; power = -2; toughness = 4 }
    val tax = card("Enlist Toll") { typeLine = "Enchantment"; staticAbility { ability = AttackTax(DynamicAmount.Fixed(1)) } }
    fun instant(name: String, makeEffect: (EffectTarget) -> com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val victim = target(TargetFilter.Creature); effect = makeEffect(victim) }
    }
    val kill = instant("Enlist Kill") { Effects.Destroy(it) }
    val blank = instant("Enlist Blank") { Effects.RemoveAllAbilities(it) }

    val blink = instant("Enlist Blink") { Effects.Exile(it) then Effects.PutOntoBattlefield(it) }
    val grantHaste = instant("Enlist Haste") { Effects.GrantKeyword(Keyword.HASTE, it) }
    val grantEnlist = instant("Enlist Grant") { Effects.GrantStaticAbility(Enlist, it) }
    val animate = card("Enlist Animation") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { val land = target(TargetFilter.Land); effect = Effects.AnimateLand(land, 4, 4) }
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(elixir, tapWatcher, manaRock, recruit, double, watcher, haste, negative, tax, kill, blank, blink, grantHaste, grantEnlist, animate))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun ready(d: GameTestDriver, name: String, player: EntityId = d.activePlayer!!) =
        d.putCreatureOnBattlefield(player, name).also(d::removeSummoningSickness)
    fun attack(d: GameTestDriver, ids: List<EntityId>) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.activePlayer!!, ids, d.getOpponent(d.activePlayer!!)).error shouldBe null
    }
    fun order(d: GameTestDriver) {
        val q = d.pendingDecision as? OrderObjectsDecision ?: return
        d.submitOrderedResponse(q.playerId, q.objects).error shouldBe null
    }
    fun choose(d: GameTestDriver, ids: List<EntityId>) {
        val player = d.pendingDecision!!.playerId
        d.submitCardSelection(player, ids).error shouldBe null
        order(d)
    }
    fun power(d: GameTestDriver, id: EntityId) = d.state.projectedState.getPower(id)
    fun resolveAll(d: GameTestDriver) {
        order(d)
        repeat(10) { if (d.state.stack.isNotEmpty()) d.bothPass() }
        d.state.stack.size shouldBe 0
    }
    fun cast(d: GameTestDriver, spell: String, target: EntityId) {
        val player = d.state.priorityPlayerId!!
        val id = d.putCardInHand(player, spell)
        d.castSpell(player, id, listOf(target)).error shouldBe null
        d.bothPass()
    }

    test("selection and its rollback checkpoint survive serialization") {
        val d = setup(); val a = ready(d, recruit.name); val b = ready(d, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.getOpponent(d.activePlayer!!), tax.name)
        d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        attack(d, listOf(a))
        val json = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val encoded = json.encodeToString(com.wingedsheep.engine.state.GameState.serializer(), d.state)
        d.replaceState(json.decodeFromString(com.wingedsheep.engine.state.GameState.serializer(), encoded))
        choose(d, listOf(b))
        d.submitManaAutoPayOrDecline(d.activePlayer!!, true).error shouldBe null
        resolveAll(d); power(d,a) shouldBe 3
    }
    test("a projected animated land with granted haste is eligible") {
        val d = setup(); val a = ready(d, recruit.name); val land = d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        val spell = d.putCardInHand(d.activePlayer!!, animate.name)
        d.castSpell(d.activePlayer!!, spell, listOf(land)).error shouldBe null; d.bothPass()
        cast(d, grantHaste.name, land)
        attack(d,listOf(a)); d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options shouldBe listOf(land)
        choose(d,listOf(land)); resolveAll(d); power(d,a) shouldBe 5
    }
    test("an enlist ability granted by an effect offers the same attack cost") {
        val d = setup(); val a = ready(d, "Grizzly Bears"); val b = ready(d, haste.name)
        cast(d, grantEnlist.name, a); attack(d,listOf(a)); choose(d,listOf(b)); resolveAll(d)
        power(d,a) shouldBe 5
    }
    test("enlisted creature blink uses its original departure power") {
        val d = setup(); val a = ready(d, recruit.name); val b = ready(d, "Grizzly Bears")
        attack(d,listOf(a)); choose(d,listOf(b))
        d.addComponent(b, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
        cast(d,blink.name,b); power(d,b) shouldBe 2
        resolveAll(d); power(d,a) shouldBe 7
    }
    test("a blinked attacker is a new object and receives no old enlist bonus") {
        val d = setup(); val a = ready(d,recruit.name); val b = ready(d,"Grizzly Bears")
        attack(d,listOf(a)); choose(d,listOf(b)); cast(d,blink.name,a)
        resolveAll(d); power(d,a) shouldBe 1
    }
    test("removing enlist after payment cannot erase the linked trigger") {
        val d = setup(); val a = ready(d,recruit.name); val b = ready(d,"Grizzly Bears")
        attack(d,listOf(a)); choose(d,listOf(b)); cast(d,blank.name,a)
        resolveAll(d); power(d,a) shouldBe 3
    }

    test("enlisting a face-down creature never exposes its name in public events") {
        val d = setup(); val a = ready(d,recruit.name); val b = ready(d,"Grizzly Bears")
        d.addComponent(b, com.wingedsheep.engine.state.components.identity.FaceDownComponent)
        attack(d,listOf(a))
        val result = d.submitCardSelection(d.pendingDecision!!.playerId, listOf(b))
        result.error shouldBe null
        val event = result.events.filterIsInstance<EnlistedEvent>().single()
        event.enlistedName.contains("Grizzly") shouldBe false
        result.events.filterIsInstance<ReflexiveAbilityTriggeredEvent>().single()
            .descriptionOverride!!.contains("Grizzly") shouldBe false
        resolveAll(d); power(d,a) shouldBe 3
    }

    test("declining enlist still declares the attacker and creates no boost") {
        val d = setup(); val a = ready(d, recruit.name); ready(d, "Grizzly Bears")
        attack(d, listOf(a))
        d.isTapped(a) shouldBe true // Declaration taps precede the optional cost choice.
        choose(d, emptyList())
        d.state.getEntity(a)?.has<AttackingComponent>() shouldBe true
        d.state.stack.size shouldBe 0
        power(d, a) shouldBe 1
    }
    test("eligibility excludes sick creatures even with as-though-hasty permission, but accepts actual haste") {
        val d = setup(); val a = ready(d, recruit.name); val otherAttacker = ready(d, "Grizzly Bears")
        val helper = ready(d, "Grizzly Bears"); val hasty = d.putCreatureOnBattlefield(d.activePlayer!!, haste.name)
        d.putCreatureOnBattlefield(d.activePlayer!!, "Grizzly Bears")
        ready(d, "Grizzly Bears").also(d::tapPermanent)
        ready(d, "Grizzly Bears", d.getOpponent(d.activePlayer!!))
        d.putPermanentOnBattlefield(d.activePlayer!!, elixir.name)
        attack(d, listOf(a, otherAttacker))
        val q = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        q.options.shouldContainExactlyInAnyOrder(helper, hasty)
        q.useTargetingUI shouldBe true
        (d.submitCardSelection(q.playerId, listOf(otherAttacker)).error != null) shouldBe true
        (d.submitCardSelection(q.playerId, listOf(helper, hasty)).error != null) shouldBe true
        choose(d, listOf(hasty)); d.isTapped(hasty) shouldBe true
        power(d, a) shouldBe 1; d.state.stack.size shouldBe 1
        resolveAll(d); power(d, a) shouldBe 4
    }
    test("power is read at resolution and expires at end of turn") {
        val d = setup(); val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.addComponent(helper, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
        resolveAll(d); power(d, a) shouldBe 6
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, d.getOpponent(d.activePlayer!!))
        power(d, a) shouldBe 1
    }
    test("negative enlisted power grants zero and still fires enlisting payoffs") {
        val d = setup(); val p = d.activePlayer!!; val a = ready(d, watcher.name); val helper = ready(d, negative.name)
        val life = d.getLifeTotal(p)
        attack(d, listOf(a)); choose(d, listOf(helper)); resolveAll(d)
        power(d, a) shouldBe 1; d.getLifeTotal(p) shouldBe life + 2
    }
    test("two independent instances may enlist different creatures but cannot reuse one payment") {
        val d = setup(); val a = ready(d, double.name); val b = ready(d, "Grizzly Bears"); val c = ready(d, haste.name)
        attack(d, listOf(a)); choose(d, listOf(b))
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>().options.shouldContainExactlyInAnyOrder(c)
        choose(d, listOf(c)); d.state.stack.size shouldBe 2
        resolveAll(d); power(d, a) shouldBe 6
    }
    test("two attackers cannot enlist the same creature") {
        val d = setup(); val a = ready(d, recruit.name); val b = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        attack(d, listOf(a,b)); choose(d, listOf(helper)); resolveAll(d)
        (power(d,a)!! + power(d,b)!!) shouldBe 4
    }
    test("losing abilities removes the enlist option") {
        val d = setup(); val a = ready(d, recruit.name); ready(d, "Grizzly Bears")
        cast(d, blank.name, a); attack(d, listOf(a))
        d.pendingDecision shouldBe null; d.state.stack.size shouldBe 0
    }
    test("enlisted creature dying in response uses its departure power including later counters") {
        val d = setup(); val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.addComponent(helper, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 4)))
        cast(d, kill.name, helper); resolveAll(d)
        power(d, a) shouldBe 7
    }
    test("enlisting source leaving in response does not grant the bonus to anything else") {
        val d = setup(); val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        attack(d, listOf(a)); choose(d, listOf(helper)); cast(d, kill.name, a); resolveAll(d)
        power(d, helper) shouldBe 2
    }
    test("attack-tax cancellation leaves both attacker and chosen helper untapped") {
        val d = setup(); val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.getOpponent(d.activePlayer!!), tax.name)
        d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitManaAutoPayOrDecline(d.activePlayer!!, false).error shouldBe null
        d.isTapped(a) shouldBe false; d.isTapped(helper) shouldBe false
        d.state.stack.size shouldBe 0
    }
    test("cancelling removes declaration triggers but preserves independent mana actions") {
        val d = setup(); val player = d.activePlayer!!
        val a = ready(d, tapWatcher.name); val helper = ready(d, "Grizzly Bears")
        val rock = d.putPermanentOnBattlefield(player, manaRock.name)
        d.putPermanentOnBattlefield(d.getOpponent(player), tax.name)
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.submit(ActivateAbility(player, rock, manaRock.activatedAbilities.single().id)).error shouldBe null
        val q = d.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        d.submitDecision(player, ManaSourcesSelectedResponse(q.id, declined = true)).error shouldBe null
        d.isTapped(a) shouldBe false
        d.isTapped(rock) shouldBe true
        d.state.getEntity(player)!!.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!.colorless shouldBe 1
        resolveAll(d)
        d.getLifeTotal(player) shouldBe 23
    }
    test("pending enlist checkpoint contains no hidden hand or library references") {
        val d = setup(); val a = ready(d, recruit.name); ready(d, "Grizzly Bears")
        val secret = d.putCardInHand(d.getOpponent(d.activePlayer!!), "Grizzly Bears")
        d.putPermanentOnBattlefield(d.getOpponent(d.activePlayer!!), tax.name)
        d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        attack(d, listOf(a))
        val checkpoint = d.state.attackDeclarationCheckpoint()!!
        val json = kotlinx.serialization.json.Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true }
        val encoded = json.encodeToString(AttackDeclarationCheckpoint.serializer(), checkpoint)
        encoded.contains(secret.value.toString()) shouldBe false
        checkpoint.changes.flatMap { it.before.components.values + it.after.components.values }
            .any { it is com.wingedsheep.engine.state.components.identity.CardComponent } shouldBe false
    }
    test("payment rechecks helper control and rejects without spending tax mana") {
        val d = setup(); val player = d.activePlayer!!
        val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        val land = d.putLandOnBattlefield(player, "Forest")
        d.putPermanentOnBattlefield(d.getOpponent(player), tax.name)
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.addComponent(helper, com.wingedsheep.engine.state.components.identity.ControllerComponent(d.getOpponent(player)))
        val before = d.state
        (d.submitManaAutoPayOrDecline(player, true).error != null) shouldBe true
        d.state shouldBe before
        d.isTapped(land) shouldBe false
        d.isTapped(helper) shouldBe false
    }
    test("attack-tax payment completes enlist and declares the attack") {
        val d = setup(); val a = ready(d, recruit.name); val helper = ready(d, "Grizzly Bears")
        d.putPermanentOnBattlefield(d.getOpponent(d.activePlayer!!), tax.name)
        val land = d.putLandOnBattlefield(d.activePlayer!!, "Forest")
        attack(d, listOf(a)); choose(d, listOf(helper))
        d.submitManaAutoPayOrDecline(d.activePlayer!!, true).error shouldBe null
        d.isTapped(land) shouldBe true; d.isTapped(helper) shouldBe true
        resolveAll(d); power(d, a) shouldBe 3
    }
})
