package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bng.cards.Crypsis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Crypsis (BNG) — {1}{U} Instant. "Target creature you control gains protection from creatures
 * your opponents control until end of turn. Untap it."
 *
 * Pins each leg of the opponents-scoped protection (CR 702.16b–f): it untaps the creature, it
 * can't be blocked by an opponent's creature, an opponent's creature's ability can't target it,
 * its controller's own creature's ability still can, an opponent's non-creature spell still can,
 * and combat damage from an opponent's creature it blocks is prevented.
 */
class CrypsisScenarioTest : FunSpec({

    /** "{T}: This creature deals 1 damage to any target." — a creature ability source. */
    val pinger = card("Test Pinger") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Human Wizard"
        oracleText = "{T}: This creature deals 1 damage to any target."
        power = 1
        toughness = 1
        activatedAbility {
            cost = Costs.Tap
            val t = target(Targets.Any)
            effect = Effects.DealDamage(1, t)
        }
    }

    class Setup(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && pendingDecision == null && guard++ < 10) bothPass()
    }

    /** `me` casts Crypsis; with [onOpponentsTurn] the opponent is the active player. */
    fun setUp(onOpponentsTurn: Boolean = false): Setup {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + Crypsis + pinger)
            initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        }
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        return if (onOpponentsTurn) Setup(d, d.getOpponent(active), active) else Setup(d, active, d.getOpponent(active))
    }

    fun Setup.castCrypsis(on: EntityId) {
        val crypsis = d.putCardInHand(me, "Crypsis")
        d.giveMana(me, Color.BLUE, 2)
        d.castSpell(me, crypsis, listOf(on)).error shouldBe null
        d.resolveAll()
    }

    fun Setup.pingWith(controller: EntityId, pingerId: EntityId, target: EntityId) =
        d.submit(
            ActivateAbility(controller, pingerId, pinger.activatedAbilities.first().id, listOf(ChosenTarget.Permanent(target)))
        )

    test("untaps the creature, and an opponent's creature can't block it") {
        val s = setUp()
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        s.d.removeSummoningSickness(courser)
        val bear = s.d.putCreatureOnBattlefield(s.opp, "Savannah Lions")
        s.d.tapPermanent(courser)

        s.castCrypsis(courser)
        withClue("Untap it") { s.d.isTapped(courser) shouldBe false }

        s.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        s.d.declareAttackers(s.me, listOf(courser), s.opp).error shouldBe null
        s.d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("the Lions are a creature an opponent controls") {
            s.d.declareBlockers(s.opp, mapOf(bear to listOf(courser))).error shouldNotBe null
        }
    }

    test("an opponent's creature's ability can't target it; its controller's own creature's can") {
        val s = setUp()
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        val theirPinger = s.d.putCreatureOnBattlefield(s.opp, "Test Pinger")
        val myPinger = s.d.putCreatureOnBattlefield(s.me, "Test Pinger")
        s.d.removeSummoningSickness(theirPinger)
        s.d.removeSummoningSickness(myPinger)
        s.castCrypsis(courser)

        s.d.passPriority(s.me)
        withClue("protection from creatures your opponents control stops the opponent's pinger") {
            s.pingWith(s.opp, theirPinger, courser).error shouldNotBe null
        }
        s.d.passPriority(s.opp)

        withClue("a creature its own controller controls doesn't have the quality") {
            s.pingWith(s.me, myPinger, courser).error shouldBe null
        }
        s.d.resolveAll()
        s.d.state.getEntity(courser)?.get<DamageComponent>()?.amount shouldBe 1
    }

    test("an opponent's instant still targets and damages it — the quality is creatures only") {
        val s = setUp()
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        s.castCrypsis(courser)

        val bolt = s.d.putCardInHand(s.opp, "Lightning Bolt")
        s.d.giveMana(s.opp, Color.RED, 1)
        s.d.passPriority(s.me)
        s.d.castSpell(s.opp, bolt, listOf(courser)).error shouldBe null
        s.d.resolveAll()
        s.d.getGraveyard(s.me).contains(courser) shouldBe true
    }

    test("combat damage from an opponent's attacker it blocks is prevented") {
        val s = setUp(onOpponentsTurn = true)
        val courser = s.d.putCreatureOnBattlefield(s.me, "Centaur Courser")
        val force = s.d.putCreatureOnBattlefield(s.opp, "Force of Nature")
        s.d.removeSummoningSickness(force)

        s.d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        s.d.declareAttackers(s.opp, listOf(force), s.me).error shouldBe null
        s.d.passPriority(s.opp)
        s.castCrypsis(courser)

        s.d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("protection doesn't stop the protected creature from blocking") {
            s.d.declareBlockers(s.me, mapOf(courser to listOf(force))).error shouldBe null
        }
        s.d.passPriorityUntil(Step.END_COMBAT)
        withClue("Force of Nature's 8 damage is prevented") {
            s.d.getGraveyard(s.me).contains(courser) shouldBe false
            (s.d.state.getEntity(courser)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        }
        withClue("the Courser still deals its 3") {
            s.d.state.getEntity(force)?.get<DamageComponent>()?.amount shouldBe 3
        }
    }
})
