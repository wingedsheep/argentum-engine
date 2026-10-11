package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.event.GrantedStaticAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `CantAttackYou(filter, coversPlaneswalkers)` — the attacker-side, player-scoped attack
 * restriction (CR 508.1c), resolved by `CantAttackYouDefenderRule`.
 *
 * Covers the two sources it reads — a static printed on a battlefield permanent ("you" = that
 * permanent's controller) and a grant from a resolving effect ("you" = the granting effect's
 * controller, gated by the grant's "for as long as …" duration) — the planeswalker axis, and the
 * multiplayer point of the whole type: the restricted creature stays free to attack anyone else.
 *
 * Every test keeps an unrestricted attacker on the attacking side so the declare-attackers step is
 * reached and a rejection means the restriction, not "no legal attack".
 */
class CantAttackYouRestrictionTest : FunSpec({

    /** "Creatures your opponents control with flying can't attack you or planeswalkers you control." */
    val skyOath = card("Test Sky Oath") {
        manaCost = "{2}{W}"
        typeLine = "Enchantment"
        oracleText = "Creatures your opponents control with flying can't attack you or planeswalkers you control."
        staticAbility {
            ability = CantAttackYou(
                GroupFilter(GameObjectFilter.Creature.opponentControls().withKeyword(Keyword.FLYING)),
                coversPlaneswalkers = true
            )
        }
    }

    val flier = card("Test Oath Flier") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING)
    }

    val grounder = card("Test Oath Grounder") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    val walker = card("Test Oath Walker") {
        manaCost = "{2}"
        typeLine = "Legendary Planeswalker — Tester"
        startingLoyalty = 3
        loyaltyAbility(1) {
            effect = Effects.GainLife(1)
        }
    }

    val extras = listOf(skyOath, flier, grounder, walker)

    fun GameTestDriver.attackStepOf(player: EntityId) {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        while (activePlayer != player) {
            passPriorityUntil(Step.POSTCOMBAT_MAIN)
            passPriorityUntil(Step.DECLARE_ATTACKERS)
        }
        currentStep shouldBe Step.DECLARE_ATTACKERS
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.walkerFor(player: EntityId): EntityId {
        val id = putPermanentOnBattlefield(player, "Test Oath Walker")
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 3))
        })
        return id
    }

    fun twoPlayer(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + extras)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("printed: a covered creature can't attack the holder's controller or their planeswalker") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)

        d.putPermanentOnBattlefield(opp, "Test Sky Oath")
        val pw = d.walkerFor(opp)
        val bird = d.ready(active, "Test Oath Flier")
        val beast = d.ready(active, "Test Oath Grounder")

        d.attackStepOf(active)
        withClue("the flier can't attack the oath's controller") {
            d.declareAttackers(active, listOf(bird), opp).error shouldNotBe null
        }
        withClue("coversPlaneswalkers: nor a planeswalker that player controls") {
            d.declareAttackers(active, mapOf(bird to pw)).error shouldNotBe null
        }
        withClue("a creature outside the filter is untouched") {
            d.declareAttackers(active, listOf(beast), opp).error shouldBe null
        }
    }

    test("printed: the restriction is scoped to one player — the creature may attack another opponent") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + extras)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(p2, "Test Sky Oath")
        val bird = d.ready(p1, "Test Oath Flier")
        d.ready(p1, "Test Oath Grounder")

        d.attackStepOf(p1)
        withClue("can't attack the oath's controller") {
            d.declareAttackers(p1, listOf(bird), p2).error shouldNotBe null
        }
        withClue("free to attack the third player") {
            d.declareAttackers(p1, listOf(bird), p3).error shouldBe null
        }
    }

    fun GameTestDriver.grantVow(creature: EntityId, protectedPlayer: EntityId) {
        replaceState(
            state.updateEntity(creature) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.VOW, 1))
            }.copy(
                grantedStaticAbilities = state.grantedStaticAbilities + GrantedStaticAbility(
                    entityId = creature,
                    ability = CantAttackYou(),
                    duration = Duration.WhileAffectedHasCounter(CounterType.VOW),
                    controllerId = protectedPlayer
                )
            )
        )
    }

    test("granted: \"you\" is the granting effect's controller, and the counter-keyed duration ends it") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)

        val beast = d.ready(active, "Test Oath Grounder")
        d.ready(active, "Test Oath Flier")
        d.grantVow(beast, protectedPlayer = opp)

        d.attackStepOf(active)
        withClue("the grant's controller is protected, though the creature's own controller holds it") {
            d.declareAttackers(active, listOf(beast), opp).error shouldNotBe null
        }

        d.replaceState(d.state.updateEntity(beast) { c ->
            c.with(c.get<CountersComponent>()!!.withRemoved(CounterType.VOW, 1))
        })
        withClue("the vow counter gone, the restriction is gone") {
            d.declareAttackers(active, listOf(beast), opp).error shouldBe null
        }
    }

    test("granted without coversPlaneswalkers: the protected player's planeswalker is fair game") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)

        val beast = d.ready(active, "Test Oath Grounder")
        d.ready(active, "Test Oath Flier")
        val pw = d.walkerFor(opp)
        d.grantVow(beast, protectedPlayer = opp)

        d.attackStepOf(active)
        d.declareAttackers(active, mapOf(beast to pw)).error shouldBe null
    }
})
