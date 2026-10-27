package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c17.cards.CurseOfOpulence
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Curse of Opulence (C17 #24): "Whenever enchanted player is attacked, create a Gold token. Each
 * opponent attacking that player does the same."
 *
 * The trigger belongs to the Curse's controller but watches the *enchanted* player's defence, so
 * the tests put the Curse, the cursed player and the attacker in three different seats — the only
 * arrangement where a controller-scoped "you are attacked" reading and the attachment-scoped one
 * disagree.
 */
class CurseOfOpulenceScenarioTest : FunSpec({

    fun driver(): Pair<GameTestDriver, List<EntityId>> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(PredefinedTokens.allTokens)
        d.registerCard(CurseOfOpulence)
        val players = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe players[0]
        return d to players
    }

    /** The Curse under [controller], attached straight to [cursed] — enchant player needs a player host. */
    fun GameTestDriver.curse(controller: EntityId, cursed: EntityId): EntityId {
        val curse = putPermanentOnBattlefield(controller, "Curse of Opulence")
        replaceState(state.updateEntity(curse) { it.with(AttachedToComponent(cursed)) })
        return curse
    }

    fun GameTestDriver.ready(player: EntityId, name: String): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.gold(player: EntityId): Int =
        getPermanents(player).count { state.getEntity(it)?.get<CardComponent>()?.name == "Gold" }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (pendingDecision == null && stackSize > 0 && guard++ < 20) bothPass()
    }

    test("an opponent attacks the cursed player: the Curse's controller and the attacker each get Gold") {
        val (d, players) = driver()
        val (p1, p2, p3) = players
        d.curse(controller = p2, cursed = p3)
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        withClue("enchanted player is attacked") { d.stackSize shouldBe 1 }
        d.resolveAll()

        withClue("you create a Gold token") { d.gold(p2) shouldBe 1 }
        withClue("each opponent attacking that player does the same") { d.gold(p1) shouldBe 1 }
        withClue("the cursed player gets nothing") { d.gold(p3) shouldBe 0 }
    }

    test("triggers once no matter how many creatures attack the cursed player") {
        val (d, players) = driver()
        val (p1, p2, p3) = players
        d.curse(controller = p2, cursed = p3)
        val a = d.ready(p1, "Grizzly Bears")
        val b = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(a to p3, b to p3)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()

        d.gold(p2) shouldBe 1
        d.gold(p1) shouldBe 1
    }

    test("an attack on a player the Curse doesn't enchant — including its controller — doesn't trigger") {
        val (d, players) = driver()
        val (p1, p2, p3) = players
        d.curse(controller = p2, cursed = p3)
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2)).error shouldBe null
        d.stackSize shouldBe 0
        d.gold(p1) shouldBe 0
        d.gold(p2) shouldBe 0
    }

    test("attacking the player you cursed yourself makes one Gold — you aren't your own opponent") {
        val (d, players) = driver()
        val (p1, _, p3) = players
        d.curse(controller = p1, cursed = p3)
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()

        d.gold(p1) shouldBe 1
    }

    test("a Curse on yourself pays off whenever another player attacks you") {
        val (d, players) = driver()
        val (p1, p2, _) = players
        d.curse(controller = p2, cursed = p2)
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p2)).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()

        d.gold(p2) shouldBe 1
        d.gold(p1) shouldBe 1
    }

    test("an attacker gone by resolution leaves only the Curse's controller with Gold") {
        val (d, players) = driver()
        val (p1, p2, p3) = players
        d.curse(controller = p2, cursed = p3)
        val bears = d.ready(p1, "Grizzly Bears")

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(bears to p3)).error shouldBe null
        d.stackSize shouldBe 1
        // The ruling: if no creatures are attacking the enchanted player as the ability resolves,
        // only the Curse's controller performs its actions.
        d.replaceState(d.state.updateEntity(bears) {
            it.without<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
        })
        d.resolveAll()

        d.gold(p2) shouldBe 1
        d.gold(p1) shouldBe 0
    }
})
