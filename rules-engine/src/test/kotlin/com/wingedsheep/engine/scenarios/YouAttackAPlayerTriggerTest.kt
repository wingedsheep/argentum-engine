package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `YouAttackEvent(perAttackedPlayer = true)` — "whenever you attack a player" (CR 508.3e), spelled
 * `Triggers.you.attacksAPlayer(with, minAttackers)`.
 *
 * Pins what the axis adds over the once-per-declaration "whenever you attack": it fires once for
 * each player attacked (Echoing Assault / Meriadoc Brandybuck rulings) and binds that player as
 * `Player.TriggeringPlayer`; an attacker aimed at a planeswalker counts toward no player; and the
 * attacker count and filter are per attacked player, not summed across the declaration.
 */
class YouAttackAPlayerTriggerTest : FunSpec({

    /** "Whenever you attack a player, that player loses 1 life." */
    val drain = card("Test Attack-a-Player Drain") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.attacksAPlayer()
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    /** "Whenever two or more creatures you control attack a player, that player loses 1 life." */
    val pairDrain = card("Test Pair Drain") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.attacksAPlayer(minAttackers = 2)
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    /** "Whenever one or more Centaurs you control attack a player, that player loses 1 life." */
    val centaurDrain = card("Test Centaur Drain") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.attacksAPlayer(GameObjectFilter.Creature.youControl().withSubtype("Centaur"))
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    val walker = card("Test Attacked Walker") {
        manaCost = "{3}"
        typeLine = "Legendary Planeswalker — Test"
        startingLoyalty = 3
        oracleText = ""
    }

    val cards = TestCards.all + drain + pairDrain + centaurDrain + walker

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun threePlayer(): Triple<GameTestDriver, EntityId, Pair<EntityId, EntityId>> {
        val d = GameTestDriver()
        d.registerCards(cards)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1
        return Triple(d, p1, p2 to p3)
    }

    test("two-player: several attackers at one player fire it once, binding that player") {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(active, "Test Attack-a-Player Drain")
        val a = d.ready(active, "Grizzly Bears")
        val b = d.ready(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a, b), opp).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(opp) shouldBe 19
        d.getLifeTotal(active) shouldBe 20
    }

    test("multiplayer: attacking two players fires it once for each, each binding its own player") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Attack-a-Player Drain")
        val atP2 = d.ready(p1, "Grizzly Bears")
        val atP3 = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(atP2 to p2, atP3 to p3)).error shouldBe null
        d.stackSize shouldBe 2
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 19
        d.getLifeTotal(p3) shouldBe 19
    }

    test("an attack on a planeswalker attacks no player") {
        val d = GameTestDriver()
        d.registerCards(cards)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(active, "Test Attack-a-Player Drain")
        val pw = d.putPermanentOnBattlefield(opp, "Test Attacked Walker")
        d.replaceState(d.state.updateEntity(pw) { it.with(CountersComponent().withAdded(CounterType.LOYALTY, 3)) })
        val bears = d.ready(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, mapOf(bears to pw)).error shouldBe null
        withClue("CR 508.3e: attacking a planeswalker isn't attacking its controller") { d.stackSize shouldBe 0 }
    }

    test("the attacker count is per player: one each at two players doesn't make two") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Pair Drain")
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p2, b to p3)).error shouldBe null
        d.stackSize shouldBe 0
    }

    test("the attacker count is per player: two at one player and one at another fires for the first only") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Pair Drain")
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")
        val c = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p2, b to p2, c to p3)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 19
        d.getLifeTotal(p3) shouldBe 20
    }

    test("the attacker filter is per player: only the player a matching creature attacks is bound") {
        val (d, p1, others) = threePlayer()
        val (p2, p3) = others
        d.putPermanentOnBattlefield(p1, "Test Centaur Drain")
        val centaur = d.ready(p1, "Centaur Courser")
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2, centaur to p3)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 20
        d.getLifeTotal(p3) shouldBe 19
    }
})
