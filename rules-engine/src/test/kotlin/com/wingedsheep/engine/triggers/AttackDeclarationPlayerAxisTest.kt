package com.wingedsheep.engine.triggers

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `YouAttackEvent.player` — whose attack declaration a "whenever … attack(s) with N or more"
 * trigger counts. Only the active player (team) declares attackers, so the axis reads the active
 * turn: [Triggers.you] is your declaration, [Triggers.anOpponent] an opponent's, and
 * [Triggers.anyPlayer] every declaration — "whenever two or more creatures attack" (Argent Dais,
 * whose ruling says it is not just when *you* attack). Each fires once per declaration.
 */
class AttackDeclarationPlayerAxisTest : FunSpec({

    fun watcher(name: String, trigger: com.wingedsheep.sdk.scripting.TriggerSpec) = card(name) {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "Whenever … attack, you gain 1 life."
        triggeredAbility {
            this.trigger = trigger
            effect = Effects.GainLife(1)
        }
    }

    val YouWatcher = watcher("You Attack Watcher", Triggers.you.attacks(minAttackers = 2))
    val OpponentWatcher = watcher("Opponent Attack Watcher", Triggers.anOpponent.attacks(minAttackers = 2))
    val AnyWatcher = watcher("Any Attack Watcher", Triggers.anyPlayer.attacks(minAttackers = 2))

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(YouWatcher, OpponentWatcher, AnyWatcher))
        initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    /** Active player attacks with [attackers] Grizzly Bears; [watcherOwner] holds one of [watchers]. */
    fun attack(attackers: Int, watcherIsActive: Boolean, watcher: String): Int {
        val d = driver()
        val active = d.activePlayer!!
        val other = d.getOpponent(active)
        val owner: EntityId = if (watcherIsActive) active else other
        d.putPermanentOnBattlefield(owner, watcher)
        val bears = (1..attackers).map {
            d.putCreatureOnBattlefield(active, "Grizzly Bears").also { b -> d.removeSummoningSickness(b) }
        }
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val before = d.getLifeTotal(owner)
        d.declareAttackers(active, bears, other)
        d.drain()
        return d.getLifeTotal(owner) - before
    }

    test("anyPlayer fires on an opponent's declaration of two attackers") {
        attack(2, watcherIsActive = false, watcher = "Any Attack Watcher") shouldBe 1
    }

    test("anyPlayer fires on your own declaration of two attackers") {
        attack(2, watcherIsActive = true, watcher = "Any Attack Watcher") shouldBe 1
    }

    test("anyPlayer ignores a declaration below the threshold") {
        attack(1, watcherIsActive = false, watcher = "Any Attack Watcher") shouldBe 0
    }

    test("you fires only on your own declaration") {
        attack(2, watcherIsActive = true, watcher = "You Attack Watcher") shouldBe 1
        attack(2, watcherIsActive = false, watcher = "You Attack Watcher") shouldBe 0
    }

    test("anOpponent fires only on an opponent's declaration") {
        attack(2, watcherIsActive = false, watcher = "Opponent Attack Watcher") shouldBe 1
        attack(2, watcherIsActive = true, watcher = "Opponent Attack Watcher") shouldBe 0
    }

    test("descriptions read per player") {
        (AnyWatcher.triggeredAbilities.single().trigger as EventPattern.YouAttackEvent).description shouldBe
            "2 or more creatures attack"
        (OpponentWatcher.triggeredAbilities.single().trigger as EventPattern.YouAttackEvent).description shouldBe
            "an opponent attacks with 2 or more creatures"
        (YouWatcher.triggeredAbilities.single().trigger as EventPattern.YouAttackEvent).description shouldBe
            "you attack with 2 or more creatures"
    }
})
