package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A `CreaturesAttackYouEvent` firing captures the attackers behind it as the trigger's captured
 * collection, so "that many" reads the creatures **declared** attacking you (CR 508.1) — Struggle
 * for Project Purity's ruling: removing an attacker from combat doesn't lower the count, and only
 * attackers aimed at the trigger's controller count. The `unblocked` form captures, per attacking
 * player, that player's unblocked attackers.
 */
class CreaturesAttackYouCapturedAttackersTest : FunSpec({

    val capturedCount = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured)

    /** "Whenever a player attacks you with one or more creatures, that player loses 1 life for each of them." */
    val watcher = card("Test Captured Attack Watcher") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.isAttacked()
            effect = Effects.LoseLife(capturedCount, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    /** The unblocked form: "…attack you and aren't blocked, that player loses 1 life for each of them." */
    val unblockedWatcher = card("Test Captured Unblocked Watcher") {
        manaCost = "{2}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.isAttacked(unblocked = true)
            effect = Effects.LoseLife(capturedCount, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        }
    }

    val removeFromCombat = card("Test Captured Remove From Combat") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.RemoveFromCombat(creature)
        }
    }

    val cards = TestCards.all + listOf(watcher, unblockedWatcher, removeFromCombat)

    fun GameTestDriver.ready(player: EntityId, name: String = "Grizzly Bears"): EntityId =
        putCreatureOnBattlefield(player, name).also { removeSummoningSickness(it) }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (stackSize > 0 && guard++ < 20) bothPass()
    }

    fun twoPlayer(): GameTestDriver = GameTestDriver().apply {
        registerCards(cards)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("counts every creature declared attacking you") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, watcher.name)
        val attackers = List(3) { d.ready(active) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, attackers, opp).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(active) shouldBe 17
    }

    test("the count is fixed at declaration: an attacker removed from combat still counts") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, watcher.name)
        val attackers = List(2) { d.ready(active) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, attackers, opp).error shouldBe null
        d.passPriority(active).error shouldBe null
        val spell = d.putCardInHand(opp, removeFromCombat.name)
        d.castSpell(opp, spell, listOf(attackers.first())).error shouldBe null
        d.bothPass()
        withClue("the removal resolved first") {
            d.state.getEntity(attackers.first())?.has<AttackingComponent>() shouldBe false
        }
        d.resolveAll()
        d.getLifeTotal(active) shouldBe 18
    }

    test("multiplayer: only the attackers aimed at the watcher's controller count") {
        val d = GameTestDriver()
        d.registerCards(cards)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe p1

        d.putPermanentOnBattlefield(p2, watcher.name)
        val atP2 = d.ready(p1)
        val atP3 = List(2) { d.ready(p1) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, mapOf(atP2 to p2) + atP3.associateWith { p3 }).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(p1) shouldBe 19
    }

    test("unblocked form: captures only the unblocked attackers") {
        val d = twoPlayer()
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.putPermanentOnBattlefield(opp, unblockedWatcher.name)
        val blocker = d.ready(opp)
        val attackers = List(3) { d.ready(active) }

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, attackers, opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(opp, mapOf(blocker to listOf(attackers.first()))).error shouldBe null
        d.stackSize shouldBe 1
        d.resolveAll()
        d.getLifeTotal(active) shouldBe 18
    }
})
