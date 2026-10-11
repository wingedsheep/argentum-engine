package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CreaturesAttackYouEvent(unblocked = true)` — "whenever one or more creatures attack you and
 * aren't blocked" (Coveted Jewel), spelled `Triggers.you.isAttacked(unblocked = true)`.
 *
 * Pins the three things the axis adds over the declare-attackers form: it waits for the block
 * declaration and counts only attackers left unblocked (CR 509.1h); it binds the attackers'
 * controller as the triggering player; and in multiplayer, where each defending player declares
 * blockers in turn, it fires once — on its controller's own declaration — and never for attackers
 * aimed at someone else.
 */
class CreaturesAttackYouUnblockedTriggerTest : FunSpec({

    /** "Whenever one or more creatures attack you and aren't blocked, you gain 1 life and that player loses 1 life." */
    val watcher = card("Test Unblocked Watcher") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.isAttacked(unblocked = true)
            effect = Effects.GainLife(1) then
                Effects.LoseLife(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun twoPlayer(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + watcher)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    test("an unblocked attacker fires it after blocks, binding the attacking player") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, "Test Unblocked Watcher")
        val bears = d.ready(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(bears), opp).error shouldBe null
        withClue("nothing triggers at declare attackers") { d.stackSize shouldBe 0 }

        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opp).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()

        d.getLifeTotal(opp) shouldBe 21
        d.getLifeTotal(active) shouldBe 19
    }

    test("a blocked attacker doesn't count; one unblocked attacker among blocked ones fires once") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, "Test Unblocked Watcher")
        val blocker = d.ready(opp, "Grizzly Bears")
        val first = d.ready(active, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(first), opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(first))).error shouldBe null
        withClue("the only attacker is blocked") { d.stackSize shouldBe 0 }
    }

    test("two unblocked attackers are one batch") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, "Test Unblocked Watcher")
        val a = d.ready(active, "Grizzly Bears")
        val b = d.ready(active, "Grizzly Bears")
        val blocker = d.ready(opp, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(a, b), opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(a))).error shouldBe null
        withClue("b is unblocked — one trigger for the batch") { d.stackSize shouldBe 1 }
    }

    test("multiplayer: fires once, on its controller's declaration, and ignores attacks on others") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + watcher)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        d.putPermanentOnBattlefield(p2, "Test Unblocked Watcher")
        d.putPermanentOnBattlefield(p3, "Test Unblocked Watcher")
        val atP2 = d.ready(p1, "Grizzly Bears")
        val atP3 = d.ready(p1, "Grizzly Bears")
        val p3Blocker = d.ready(p3, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(atP2 to p2, atP3 to p3)).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)

        d.declareNoBlockers(p2).error shouldBe null
        d.declareBlockers(p3, mapOf(p3Blocker to listOf(atP3))).error shouldBe null
        withClue("p2's watcher fires once; p3's attacker was blocked, so p3's doesn't") {
            d.stackSize shouldBe 1
        }
        d.resolveAll()
        d.getLifeTotal(p2) shouldBe 21
        d.getLifeTotal(p3) shouldBe 20
        d.getLifeTotal(p1) shouldBe 19
    }

    test("2HG: unblocked attackers from both attacking teammates trigger once per attacking player") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + watcher)
        val p = d.initMultiplayer(
            List(4) { Deck.of("Plains" to 40) },
            format = com.wingedsheep.sdk.core.Format.TwoHeadedGiant(),
            teams = listOf(listOf(0, 1), listOf(2, 3))
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p[0]

        d.putPermanentOnBattlefield(p[2], "Test Unblocked Watcher")
        val fromP0 = d.ready(p[0], "Grizzly Bears")
        val fromP1 = d.ready(p[1], "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p[0], mapOf(fromP0 to p[2], fromP1 to p[2])).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(d.state.priorityPlayerId!!).error shouldBe null
        d.declareNoBlockers(d.state.priorityPlayerId!!).error shouldBe null

        withClue("one trigger for each attacking player (Coveted Jewel ruling)") { d.stackSize shouldBe 2 }
    }
})
