package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SpellCastEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Awaken N—[cost] (CR 702.113) — an alternative cost that adds a spell ability.
 *
 * - CR 702.113a: paying the awaken cost instead of the mana cost makes the spell, after its own
 *   effect, put N +1/+1 counters on target land you control; that land becomes a 0/0 Elemental
 *   creature with haste that's still a land — with no duration, so it stays one.
 * - CR 702.113b: the land is targeted only when the awaken cost is paid; cast normally, the spell
 *   has no such target.
 * - CR 608.2b: an awakened spell whose targets are all illegal doesn't resolve; one with a legal
 *   target left does what it can.
 * - CR 118.9c: the awaken cost doesn't change the spell's mana value.
 * - CR 707.10: a copy of an awakened spell copies the alternative cost paid, so it awakens too.
 */
class AwakenTest : FunSpec({

    val path = card("Awaken Path") {
        manaCost = "{1}{B}{B}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.awaken(4, "{5}{B}{B}"))
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.Destroy(creature)
        }
    }

    val discovery = card("Awaken Discovery") {
        manaCost = "{1}{U}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.awaken(1, "{5}{U}"))
        spell {
            effect = Effects.DrawCards(2)
        }
    }

    val twin = card("Awaken Twin") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(spell, mayChooseNewTargets = false)
        }
    }

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    fun setup(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + path + discovery + twin)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Table(d, me, d.getOpponent(me))
    }

    fun Table.awaken(cardId: EntityId, targets: List<ChosenTarget>) = d.submit(
        CastSpell(
            me, cardId, targets,
            paymentStrategy = PaymentStrategy.FromPool,
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.AWAKEN,
        )
    )

    fun Table.counters(id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun Table.isAwakened(id: EntityId): Boolean {
        val p = d.state.projectedState
        return p.isCreature(id) && p.hasType(id, "LAND") && p.hasSubtype(id, "Elemental") &&
            p.hasKeyword(id, Keyword.HASTE)
    }

    test("awakened, the spell resolves its own effect, then the land becomes a 0/0 Elemental with N counters") {
        val t = setup()
        val bears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val pathId = t.d.putCardInHand(t.me, "Awaken Path")
        t.d.giveMana(t.me, Color.BLACK, 7)

        t.awaken(pathId, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(land))).error shouldBe null
        t.d.state.getEntity(pathId)?.get<SpellOnStackComponent>()?.alternativeCost shouldBe AlternativeCostType.AWAKEN
        t.d.bothPass()

        t.d.state.getBattlefield().contains(bears) shouldBe false
        t.counters(land) shouldBe 4
        t.isAwakened(land) shouldBe true
        t.d.state.projectedState.getPower(land) shouldBe 4
        t.d.state.projectedState.getToughness(land) shouldBe 4
    }

    test("the awakened land stays a creature on later turns — the effect has no duration") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 6)

        t.awaken(discoveryId, listOf(ChosenTarget.Permanent(land))).error shouldBe null
        t.d.bothPass()
        t.isAwakened(land) shouldBe true

        t.d.passPriorityUntil(Step.UPKEEP, t.opp)
        t.isAwakened(land) shouldBe true
        t.d.state.projectedState.getPower(land) shouldBe 1
    }

    test("cast normally, the spell has no land target and no land is awakened") {
        val t = setup()
        val bears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val pathId = t.d.putCardInHand(t.me, "Awaken Path")
        t.d.giveMana(t.me, Color.BLACK, 3)

        val normal = t.d.legalActions(t.me).single {
            val a = it.action as? CastSpell
            a?.cardId == pathId && !a.useAlternativeCost
        }
        // One target slot — the creature; the land isn't targeted (only multi-slot casts surface a list).
        normal.targetRequirements shouldBe null
        normal.validTargets shouldBe listOf(bears)

        t.d.submit(CastSpell(t.me, pathId, listOf(ChosenTarget.Permanent(bears)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        t.d.bothPass()
        t.d.state.getBattlefield().contains(bears) shouldBe false
        t.counters(land) shouldBe 0
        t.d.state.projectedState.isCreature(land) shouldBe false
    }

    test("the awaken cast is offered with the land target after the spell's own, only while you control a land") {
        val t = setup()
        t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 6)

        fun awakenOffer() = t.d.legalActions(t.me).singleOrNull {
            (it.action as? CastSpell)?.let { a -> a.cardId == discoveryId && a.alternativeCostType == AlternativeCostType.AWAKEN } == true
        }
        awakenOffer() shouldBe null

        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        t.d.putLandOnBattlefield(t.opp, "Swamp")
        val offer = awakenOffer()!!
        offer.affordable shouldBe true
        offer.requiresTargets shouldBe true
        offer.validTargets shouldBe listOf(land)
    }

    test("the land must be one you control") {
        val t = setup()
        t.d.putLandOnBattlefield(t.me, "Swamp")
        val theirLand = t.d.putLandOnBattlefield(t.opp, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 6)

        t.awaken(discoveryId, listOf(ChosenTarget.Permanent(theirLand))).error shouldNotBe null
    }

    test("the awaken cost replaces the mana cost: the printed cost alone can't pay it") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 2)

        val offer = t.d.legalActions(t.me).single {
            (it.action as? CastSpell)?.alternativeCostType == AlternativeCostType.AWAKEN
        }
        offer.affordable shouldBe false
        t.awaken(discoveryId, listOf(ChosenTarget.Permanent(land))).error shouldNotBe null
    }

    test("paying the awaken cost doesn't change the spell's mana value") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 6)

        val cast = t.awaken(discoveryId, listOf(ChosenTarget.Permanent(land)))
        cast.events.filterIsInstance<SpellCastEvent>().single().manaValue shouldBe 2
    }

    test("an awakened spell whose only target — the land — is gone doesn't resolve at all") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 6)
        val handBefore = t.d.getHandSize(t.me)

        t.awaken(discoveryId, listOf(ChosenTarget.Permanent(land))).error shouldBe null
        t.d.moveToGraveyard(land)
        t.d.bothPass()

        t.d.getHandSize(t.me) shouldBe handBefore - 1
    }

    test("with the creature target gone, the land is still awakened") {
        val t = setup()
        val bears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val pathId = t.d.putCardInHand(t.me, "Awaken Path")
        t.d.giveMana(t.me, Color.BLACK, 7)

        t.awaken(pathId, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(land))).error shouldBe null
        t.d.moveToGraveyard(bears)
        t.d.bothPass()

        t.counters(land) shouldBe 4
        t.isAwakened(land) shouldBe true
    }

    test("with the land target gone, the spell's own effect still happens") {
        val t = setup()
        val bears = t.d.putCreatureOnBattlefield(t.opp, "Grizzly Bears")
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val otherLand = t.d.putLandOnBattlefield(t.me, "Swamp")
        val pathId = t.d.putCardInHand(t.me, "Awaken Path")
        t.d.giveMana(t.me, Color.BLACK, 7)

        t.awaken(pathId, listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(land))).error shouldBe null
        t.d.moveToGraveyard(land)
        t.d.bothPass()

        t.d.state.getBattlefield().contains(bears) shouldBe false
        t.counters(otherLand) shouldBe 0
        t.d.state.projectedState.isCreature(otherLand) shouldBe false
    }

    test("a land that is already a creature gets base 0/0 plus the counters") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val first = t.d.putCardInHand(t.me, "Awaken Discovery")
        val second = t.d.putCardInHand(t.me, "Awaken Discovery")
        t.d.giveMana(t.me, Color.BLUE, 12)

        t.awaken(first, listOf(ChosenTarget.Permanent(land))).error shouldBe null
        t.d.bothPass()
        t.awaken(second, listOf(ChosenTarget.Permanent(land))).error shouldBe null
        t.d.bothPass()

        t.counters(land) shouldBe 2
        t.d.state.projectedState.getPower(land) shouldBe 2
    }

    test("a copy of an awakened spell awakens too") {
        val t = setup()
        val land = t.d.putLandOnBattlefield(t.me, "Swamp")
        val discoveryId = t.d.putCardInHand(t.me, "Awaken Discovery")
        val twinId = t.d.putCardInHand(t.me, "Awaken Twin")
        t.d.giveMana(t.me, Color.BLUE, 7)

        t.awaken(discoveryId, listOf(ChosenTarget.Permanent(land))).error shouldBe null
        t.d.submit(CastSpell(t.me, twinId, listOf(ChosenTarget.Spell(discoveryId)), paymentStrategy = PaymentStrategy.FromPool))
            .error shouldBe null
        // Twin resolves (the copy goes on the stack), then the copy, then the original.
        repeat(3) { t.d.bothPass() }

        t.counters(land) shouldBe 2
        t.isAwakened(land) shouldBe true
    }
})
