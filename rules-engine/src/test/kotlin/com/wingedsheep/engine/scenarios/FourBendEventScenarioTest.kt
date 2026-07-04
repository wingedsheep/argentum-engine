package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BendPerformedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.BendsThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * Substrate tests for the four-bend event system (CR 701.65b Airbend / 701.66b Earthbend /
 * 701.67c Waterbend / 702.189b Firebending): each keyword action emits a [BendPerformedEvent] and
 * folds its [BendType] into the player's per-turn distinct-bend set ([BendsThisTurnComponent]).
 * That drives [Triggers.YouBend] ("Whenever you waterbend, earthbend, firebend, or airbend, …") and
 * [TurnTracker.DISTINCT_BENDS] ("if you've done all four this turn" — Avatar Aang).
 *
 * The core machinery is exercised through synthetic `Effects.EmitBend` spells (the exact effect the
 * earthbend/airbend/firebend composites resolve). The final block proves the four *real* keyword
 * actions each reach that machinery end-to-end.
 */
class FourBendEventScenarioTest : FunSpec({

    // --- Synthetic bend spells: resolve EmitBend directly (the composites' tail) ------------------
    fun bendSpell(name: String, type: BendType) = card(name) {
        manaCost = "{0}"
        typeLine = "Sorcery"
        oracleText = "You ${type.oracleVerb}."
        spell { effect = Effects.EmitBend(type) }
    }
    val DoWaterbend = bendSpell("Do Waterbend", BendType.WATER)
    val DoEarthbend = bendSpell("Do Earthbend", BendType.EARTH)
    val DoFirebend = bendSpell("Do Firebend", BendType.FIRE)
    val DoAirbend = bendSpell("Do Airbend", BendType.AIR)

    // "Whenever you waterbend, earthbend, firebend, or airbend, put a +1/+1 counter on this."
    val BendWatcher = card("Bend Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.YouBend()
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }
    // "Whenever you earthbend, put a +1/+1 counter on this." — a single-element YouBend subset.
    val EarthWatcher = card("Earth Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.YouBend(setOf(BendType.EARTH))
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        }
    }
    // Mirrors Avatar Aang's payoff: "Whenever you bend, then if you've done all four this turn, …".
    val AllFourWatcher = card("All Four Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Spirit"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.YouBend()
            effect = ConditionalEffect(
                condition = Conditions.CompareAmounts(
                    DynamicAmount.TurnTracking(Player.You, TurnTracker.DISTINCT_BENDS),
                    ComparisonOperator.GTE,
                    DynamicAmount.Fixed(4)
                ),
                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 5, EffectTarget.Self)
            )
        }
    }

    // Real keyword-action drivers.
    val earthbendSorcery = CardDefinition.sorcery(
        name = "Test Earthbender",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Earthbend 1.",
        script = CardScript.spell(
            effect = Effects.Earthbend(1, EffectTarget.ContextTarget(0)),
            TargetObject(filter = TargetFilter(GameObjectFilter.Land), id = "target")
        )
    )
    val airbendAllSorcery = CardDefinition.sorcery(
        name = "Test Airbender",
        manaCost = ManaCost.parse("{0}"),
        oracleText = "Airbend all creatures.",
        script = CardScript.spell(effect = Effects.AirbendAll(GameObjectFilter.Creature, excludeSelf = false))
    )
    val firebrand = card("Test Firebrand") {
        manaCost = "{0}"; typeLine = "Creature — Elemental"; power = 1; toughness = 1
        firebending(1)
    }
    val waterbendTester = card("Waterbend Tester X") {
        manaCost = "{0}"; typeLine = "Creature — Wizard"; power = 1; toughness = 1
        oracleText = "Waterbend {2}: Draw a card."
        activatedAbility {
            cost = Costs.Mana("{2}")
            hasWaterbend = true
            effect = Effects.DrawCards(1)
        }
    }
    val vanilla = card("Test Wall") {
        manaCost = "{0}"; typeLine = "Creature — Wall"; power = 0; toughness = 4
    }
    // "Airbend target spell." — the airbend stack branch (Aang, Swift Savior), via Effects.AirbendSpell.
    val airbendSpellTester = card("Airbend Spell Tester") {
        manaCost = "{0}"; typeLine = "Instant"; oracleText = "Airbend target spell."
        spell {
            target("target spell", TargetObject(count = 1, filter = TargetFilter.SpellOnStack))
            effect = Effects.AirbendSpell()
        }
    }

    fun createDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all + listOf(
                DoWaterbend, DoEarthbend, DoFirebend, DoAirbend,
                BendWatcher, EarthWatcher, AllFourWatcher,
                earthbendSorcery, airbendAllSorcery, firebrand, waterbendTester, vanilla,
                airbendSpellTester
            )
        )
        return d
    }

    fun GameTestDriver.plusOne(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    fun GameTestDriver.bends(pid: EntityId): Set<BendType> =
        state.getEntity(pid)?.get<BendsThisTurnComponent>()?.types ?: emptySet()
    fun GameTestDriver.resolveStack() { var g = 0; while (state.stack.isNotEmpty() && g++ < 30) bothPass() }
    fun GameTestDriver.castBend(pid: EntityId, name: String) {
        val id = putCardInHand(pid, name); castSpell(pid, id); resolveStack()
    }
    fun GameTestDriver.bendEventsSince(mark: Int): List<BendType> =
        events.drop(mark).filterIsInstance<BendPerformedEvent>().map { it.bendType }

    // --- Core machinery via EmitBend --------------------------------------------------------------

    test("each bend emits a BendPerformedEvent for the actor and folds into the per-turn set") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mark = d.events.size
        d.castBend(me, "Do Earthbend")
        val ev = d.events.drop(mark).filterIsInstance<BendPerformedEvent>()
        ev.map { it.bendType } shouldBe listOf(BendType.EARTH)
        ev.single().playerId shouldBe me
        d.bends(me) shouldBe setOf(BendType.EARTH)

        d.castBend(me, "Do Waterbend")
        d.bends(me) shouldBe setOf(BendType.EARTH, BendType.WATER)
    }

    test("casting the same bend twice keeps the distinct-bend set at one entry") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castBend(me, "Do Firebend")
        d.castBend(me, "Do Firebend")
        d.bends(me) shouldBe setOf(BendType.FIRE)
    }

    test("Triggers.YouBend() fires once per bend, whatever the element") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = d.putCreatureOnBattlefield(me, "Bend Watcher")

        d.castBend(me, "Do Waterbend"); d.plusOne(watcher) shouldBe 1
        d.castBend(me, "Do Airbend"); d.plusOne(watcher) shouldBe 2
        d.castBend(me, "Do Firebend"); d.plusOne(watcher) shouldBe 3
    }

    test("a single-element YouBend subset fires only for its own element") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = d.putCreatureOnBattlefield(me, "Earth Watcher")

        d.castBend(me, "Do Waterbend"); d.plusOne(watcher) shouldBe 0
        d.castBend(me, "Do Earthbend"); d.plusOne(watcher) shouldBe 1
        d.castBend(me, "Do Firebend"); d.plusOne(watcher) shouldBe 1
    }

    test("the all-four condition flips only once all four distinct bends happen this turn") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = d.putCreatureOnBattlefield(me, "All Four Watcher")

        d.castBend(me, "Do Waterbend")
        d.castBend(me, "Do Earthbend")
        d.castBend(me, "Do Firebend")
        d.plusOne(watcher) shouldBe 0            // only three distinct — condition false
        d.castBend(me, "Do Airbend")             // fourth distinct — DISTINCT_BENDS == 4
        d.plusOne(watcher) shouldBe 5
    }

    test("the distinct-bend set resets at the start of the next turn") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.castBend(me, "Do Earthbend")
        d.bends(me) shouldBe setOf(BendType.EARTH)

        var g = 0
        while (d.activePlayer == me && g++ < 300) d.bothPass()
        d.activePlayer shouldBe d.getOpponent(me)
        d.bends(me) shouldBe emptySet()
    }

    // --- The four real keyword actions reach the machinery ----------------------------------------

    test("earthbend (real keyword action) fires the bend trigger") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val land = d.putLandOnBattlefield(me, "Mountain")
        val mark = d.events.size
        val spell = d.putCardInHand(me, "Test Earthbender")
        d.castSpell(me, spell, listOf(land))
        d.resolveStack()
        d.bendEventsSince(mark) shouldContainExactly listOf(BendType.EARTH)
        d.bends(me) shouldBe setOf(BendType.EARTH)
    }

    test("firebending (real keyword action) fires the bend trigger when the ability resolves") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val fb = d.putCreatureOnBattlefield(me, "Test Firebrand")
        d.removeSummoningSickness(fb)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val mark = d.events.size
        d.declareAttackers(me, listOf(fb), opp)
        d.resolveStack()
        d.bendEventsSince(mark) shouldContainExactly listOf(BendType.FIRE)
        d.bends(me) shouldBe setOf(BendType.FIRE)
    }

    test("airbend (real keyword action) fires only when one or more objects are exiled") {
        // Nothing to exile → no airbend (CR 701.65b).
        run {
            val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val mark = d.events.size
            d.castBend(me, "Test Airbender")
            d.bendEventsSince(mark) shouldContainExactly emptyList()
            d.bends(me) shouldBe emptySet()
        }
        // A creature on the battlefield is exiled → airbend fires.
        run {
            val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.putCreatureOnBattlefield(me, "Test Wall")
            val mark = d.events.size
            d.castBend(me, "Test Airbender")
            d.bendEventsSince(mark) shouldContainExactly listOf(BendType.AIR)
            d.bends(me) shouldBe setOf(BendType.AIR)
        }
    }

    test("airbending a spell (real keyword action) fires the bend trigger once the spell is exiled") {
        val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val watcher = d.putCreatureOnBattlefield(me, "Bend Watcher")
        // Put a {0} spell on the stack, then airbend that spell (I hold priority after casting it).
        val wallSpell = d.putCardInHand(me, "Test Wall")
        d.submit(CastSpell(playerId = me, cardId = wallSpell, paymentStrategy = PaymentStrategy.AutoPay)).error shouldBe null
        val onStack = d.state.stack.first()
        val airbend = d.putCardInHand(me, "Airbend Spell Tester")
        val mark = d.events.size
        d.submit(
            CastSpell(
                playerId = me, cardId = airbend,
                targets = listOf(ChosenTarget.Spell(onStack)),
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).error shouldBe null
        d.resolveStack()
        d.bendEventsSince(mark) shouldContainExactly listOf(BendType.AIR)
        d.bends(me) shouldBe setOf(BendType.AIR)
        d.plusOne(watcher) shouldBe 1                             // the YouBend trigger fired
    }

    test("waterbend (real keyword action) fires when the cost is paid — by taps or by pure mana") {
        // Paid by tapping two creatures for the {2}.
        run {
            val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val src = d.putCreatureOnBattlefield(me, "Waterbend Tester X")
            val c1 = d.putCreatureOnBattlefield(me, "Test Wall")
            val c2 = d.putCreatureOnBattlefield(me, "Test Wall")
            val abilityId = waterbendTester.script.activatedAbilities.first().id
            val mark = d.events.size
            d.submit(
                ActivateAbility(
                    playerId = me, sourceId = src, abilityId = abilityId,
                    alternativePayment = AlternativePaymentChoice(waterbendPermanents = setOf(c1, c2))
                )
            ).error shouldBe null
            d.resolveStack()
            d.bendEventsSince(mark) shouldContainExactly listOf(BendType.WATER)
        }
        // Paid entirely with mana (no permanents tapped) still fires the trigger (CR 701.67c).
        run {
            val d = createDriver(); d.initMirrorMatch(Deck.of("Mountain" to 40)); val me = d.activePlayer!!
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val src = d.putCreatureOnBattlefield(me, "Waterbend Tester X")
            d.giveMana(me, Color.RED, 2)
            val abilityId = waterbendTester.script.activatedAbilities.first().id
            val mark = d.events.size
            d.submit(ActivateAbility(playerId = me, sourceId = src, abilityId = abilityId)).error shouldBe null
            d.resolveStack()
            d.bendEventsSince(mark) shouldContainExactly listOf(BendType.WATER)
        }
    }
})
