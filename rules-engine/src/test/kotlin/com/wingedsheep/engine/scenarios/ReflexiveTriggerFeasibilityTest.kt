package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine-level coverage for `ReflexiveTriggerEffectExecutor.isActionFeasible` — the walker that
 * decides whether a "[action]. When you do, [reflexive]" ability's action can happen at all.
 *
 * An impossible action never happens, so CR 603.12's "when you do" never triggers. The bug this
 * guards against is the opposite of a missing prompt: a discard pipeline on an empty hand
 * auto-selects nothing and *reports success*, so without the walker the reflexive payoff fires for
 * a discard that never occurred (Inti, Seneschal of the Sun).
 *
 * The walker must be conservative in the other direction too — it only ever removes an impossible
 * prompt, never a possible one. These cards are defined inline because each pins one branch of that
 * conservatism, and no printed card currently exercises them:
 *
 *  - a **nested composite** (`draw, then discard`): `Effect.then` only flattens when its receiver is
 *    already a composite, so this is `Composite[Draw, Composite[Gather, Select, Move]]`. The gather
 *    sizes are read off pre-action state, so once a non-gather step is seen the bookkeeping must stop
 *    *and stay stopped inside the nested walk* — otherwise the inner discard is judged against the
 *    pre-draw hand and an empty hand wrongly suppresses the whole ability.
 *  - a **count larger than the hand**: `SelectFromCollectionExecutor` clamps a `ChooseExactly` count
 *    down to the collection size, so "discard two cards" with one card in hand discards that one and
 *    succeeds. Only an *empty* collection makes the action impossible.
 *
 * The mandatory (`optional = false`) case is here too: it shares the same gate, so a vacuous action
 * must not pay out even when there was never a yes/no question to skip.
 *
 * Counter removal is the other family with the same "no-ops and reports success" hazard, and it
 * pins the boundary between the walker's two answers. **Zero counters** is knowledge — the removal
 * is impossible, so the prompt must be absent (Leatherhead, Swamp Stalker would otherwise keep
 * destroying an artifact each combat after her last counter was gone). **An unresolvable target**
 * is not: feasibility runs before the action does, so a pipeline slot the same composite fills a
 * step earlier is simply not stored yet, and reading that as zero would delete a working ability.
 */
class ReflexiveTriggerFeasibilityTest : FunSpec({

    /** "Whenever you attack, you may discard a card. When you do, you gain 3 life." */
    val Prompter = card("Feasibility Test Prompter") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, you may discard a card. When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.Discard(1),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /** "Whenever you attack, you may draw a card, then discard a card. When you do, you gain 3 life." */
    val DrawThenDiscard = card("Feasibility Test Draw Then Discard") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, you may draw a card, then discard a card. " +
            "When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.DrawCards(1).then(Patterns.Hand.discardCards(1)),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /** "Whenever you attack, you may discard two cards. When you do, you gain 3 life." */
    val DiscardTwo = card("Feasibility Test Discard Two") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, you may discard two cards. When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.Discard(2),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /**
     * "Whenever you attack, you may remove a counter from a creature you control. When you do, gain
     * 3 life." — the removal target is a pipeline slot the *same composite* fills a step earlier
     * (Mister Hyde, Monster Within's shape), so at feasibility time it is not yet stored. That is a
     * "don't know", not a "no counters", and must be offered rather than silently suppressed.
     */
    val RemoveFromSelectedCreature = card("Feasibility Test Remove From Selected") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, you may remove a counter from a creature you control. " +
            "When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.Composite(
                    Effects.SelectTarget(Targets.CreatureYouControl, storeAs = "counterSource"),
                    Effects.RemoveCounterOfAnyKind(EffectTarget.PipelineTarget("counterSource", 0))
                ),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /**
     * "Whenever you attack, you may remove a -1/-1 counter from this. When you do, gain 3 life."
     *
     * 3/3 rather than 1/1 like its siblings: the tests put a -1/-1 counter on it, and a 1/1 would
     * be a 0/0 that dies to state-based actions before it ever attacks.
     */
    val RemoveNamedCounter = card("Feasibility Test Remove Named Counter") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 3
        toughness = 3
        oracleText = "Whenever you attack, you may remove a -1/-1 counter from this creature. " +
            "When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.RemoveCounters(Counters.MINUS_ONE_MINUS_ONE, 1, EffectTarget.Self),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /** "Whenever you attack, you may remove a counter from this. When you do, gain 3 life." */
    val RemoveAnyCounter = card("Feasibility Test Remove Any Counter") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, you may remove a counter from this creature. " +
            "When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.RemoveCounterOfAnyKind(EffectTarget.Self),
                optional = true,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    /** "Whenever you attack, discard a card. When you do, you gain 3 life." (mandatory) */
    val MandatoryDiscard = card("Feasibility Test Mandatory Discard") {
        manaCost = "{1}"
        typeLine = "Creature — Human Soldier"
        power = 1
        toughness = 1
        oracleText = "Whenever you attack, discard a card. When you do, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.YouAttack
            effect = ReflexiveTriggerEffect(
                action = Effects.Discard(1),
                optional = false,
                reflexiveEffect = Effects.GainLife(3)
            )
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                Prompter, DrawThenDiscard, DiscardTwo, MandatoryDiscard,
                RemoveNamedCounter, RemoveAnyCounter, RemoveFromSelectedCreature
            )
        )
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    fun GameTestDriver.emptyHand(playerId: EntityId) {
        val handZone = ZoneKey(playerId, Zone.HAND)
        var emptied = state
        getHand(playerId).toList().forEach { card -> emptied = emptied.removeFromZone(handZone, card) }
        replaceState(emptied)
    }

    /** Drain the attack triggers, answering any "may" yes/no with [accept]. Returns whether one was asked. */
    fun GameTestDriver.resolveAttackTriggers(you: EntityId, accept: Boolean): Boolean {
        var asked = false
        var guard = 0
        while (guard++ < 60) {
            when (val dec = pendingDecision) {
                is YesNoDecision -> { asked = true; submitYesNo(you, accept) }
                is SelectCardsDecision ->
                    submitCardSelection(you, dec.options.take(dec.minSelections.coerceAtLeast(1)))
                // Answer counter-kind prompts with the least the floor allows — a gate that only
                // fires because the test over-paid would prove nothing.
                is ChooseNumberDecision ->
                    submitDecision(you, NumberChosenResponse(dec.id, dec.minValue))
                else -> if (state.stack.isNotEmpty()) bothPass() else return asked
            }
        }
        return asked
    }

    /** Attack with a freshly-made [cardName], after [setUpHand] has arranged the hand. */
    fun attackWith(cardName: String, setUpHand: GameTestDriver.(EntityId) -> Unit): GameTestDriver {
        val d = driver()
        val you = d.activePlayer!!
        val attacker = d.putCreatureOnBattlefield(you, cardName)
        d.removeSummoningSickness(attacker)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.setUpHand(you)
        d.declareAttackers(you, listOf(attacker), d.getOpponent(you))
        return d
    }

    /** Attack with a freshly-made [cardName] carrying [counters]. Returns the driver and attacker. */
    fun attackWithCounters(
        cardName: String,
        counters: Map<CounterType, Int>
    ): Pair<GameTestDriver, EntityId> {
        val d = driver()
        val you = d.activePlayer!!
        val attacker = d.putCreatureOnBattlefield(you, cardName)
        d.removeSummoningSickness(attacker)
        counters.forEach { (type, count) ->
            d.replaceState(
                d.state.updateEntity(attacker) { container ->
                    val existing = container.get<CountersComponent>() ?: CountersComponent()
                    container.with(existing.withAdded(type, count))
                }
            )
        }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(attacker), d.getOpponent(you))
        return d to attacker
    }

    fun GameTestDriver.counterTotal(entityId: EntityId): Int =
        state.getEntity(entityId)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0

    test("an empty hand is never asked to discard, and never pays out") {
        val d = attackWith("Feasibility Test Prompter") { emptyHand(it) }
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe false
        d.getLifeTotal(you) shouldBe 20
    }

    test("a mandatory discard on an empty hand pays out nothing either") {
        val d = attackWith("Feasibility Test Mandatory Discard") { emptyHand(it) }
        val you = d.activePlayer!!

        // No yes/no to skip here — the gate has to stop the vacuous discard from reporting success.
        d.resolveAttackTriggers(you, accept = true) shouldBe false
        d.getLifeTotal(you) shouldBe 20
        d.getGraveyard(you).isEmpty() shouldBe true
    }

    test("draw-then-discard is still offered on an empty hand — the draw supplies the card") {
        val d = attackWith("Feasibility Test Draw Then Discard") { emptyHand(it) }
        val you = d.activePlayer!!

        // The gather is scored against the pre-action (empty) hand, so the bookkeeping must have
        // stopped at the Draw step — including inside the nested composite the discard pipeline is.
        d.resolveAttackTriggers(you, accept = true) shouldBe true
        d.getLifeTotal(you) shouldBe 23
        d.getHandSize(you) shouldBe 0
        d.getGraveyard(you).size shouldBe 1
    }

    test("discard two with only one card in hand discards that one and pays out") {
        val d = attackWith("Feasibility Test Discard Two") { you ->
            emptyHand(you)
            putCardInHand(you, "Mountain")
        }
        val you = d.activePlayer!!

        // The executor clamps ChooseExactly(2) to the single eligible card, so the action succeeds —
        // feasibility must not demand the full count.
        d.resolveAttackTriggers(you, accept = true) shouldBe true
        d.getLifeTotal(you) shouldBe 23
        d.getHandSize(you) shouldBe 0
        d.getGraveyard(you).size shouldBe 1
    }

    test("a permanent with no counters is never asked to remove a named one") {
        val (d, _) = attackWithCounters("Feasibility Test Remove Named Counter", emptyMap())
        val you = d.activePlayer!!

        // Both removal executors no-op on an empty permanent and report success, so without the
        // gate the payoff fires for a removal that never happened.
        d.resolveAttackTriggers(you, accept = true) shouldBe false
        d.getLifeTotal(you) shouldBe 20
    }

    test("a permanent with no counters is never asked to remove one of any kind") {
        val (d, _) = attackWithCounters("Feasibility Test Remove Any Counter", emptyMap())
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe false
        d.getLifeTotal(you) shouldBe 20
    }

    test("one -1/-1 counter is enough to be offered, and paying it earns the payoff") {
        val (d, attacker) = attackWithCounters(
            "Feasibility Test Remove Named Counter",
            mapOf(CounterType.MINUS_ONE_MINUS_ONE to 1)
        )
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe true
        d.getLifeTotal(you) shouldBe 23
        d.counterTotal(attacker) shouldBe 0
    }

    test("a named removal is not offered when the permanent is short of the full count") {
        // "Remove a -1/-1 counter" on a permanent carrying only +1/+1 counters: the kind that would
        // pay isn't there, so the count check must look at that kind, not the total.
        val (d, attacker) = attackWithCounters(
            "Feasibility Test Remove Named Counter",
            mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)
        )
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe false
        d.getLifeTotal(you) shouldBe 20
        d.counterTotal(attacker) shouldBe 2
    }

    test("an any-kind removal takes whichever counter is there and earns the payoff") {
        val (d, attacker) = attackWithCounters(
            "Feasibility Test Remove Any Counter",
            mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)
        )
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe true
        d.getLifeTotal(you) shouldBe 23
        d.counterTotal(attacker) shouldBe 1
    }

    test("a not-yet-stored pipeline target fails open — the ability is still offered") {
        // The removal points at a slot the composite's own earlier step fills, so at feasibility
        // time it resolves to nothing. Reading that as "zero counters" would delete the whole
        // ability; "don't know" has to mean "offer it".
        val (d, attacker) = attackWithCounters(
            "Feasibility Test Remove From Selected",
            mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)
        )
        val you = d.activePlayer!!

        d.resolveAttackTriggers(you, accept = true) shouldBe true
        d.getLifeTotal(you) shouldBe 23
        d.counterTotal(attacker) shouldBe 0
    }
})
