package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.StruggleForProjectPurity
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class StruggleForProjectPurityScenarioTest : FunSpec({
    val removeFromCombat = card("Purity Remove From Combat") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.RemoveFromCombat(creature)
        }
    }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(StruggleForProjectPurity, removeFromCombat))
        initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.chooseMode(mode: String): EntityId {
        repeat(4) { putLandOnBattlefield(player1, "Island") }
        val id = putCardInHand(player1, StruggleForProjectPurity.name)
        castSpell(player1, id).error shouldBe null
        bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(player1, OptionChosenResponse(decision.id,
            decision.options.indexOfFirst { it.startsWith(mode) })).error shouldBe null
        return id
    }

    fun GameTestDriver.rad(player: EntityId): Int =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0

    fun GameTestDriver.attackers(count: Int): List<EntityId> =
        List(count) { putCreatureOnBattlefield(player2, "Grizzly Bears").also { removeSummoningSickness(it) } }

    test("Enclave gives the attacking player two rad counters per creature they attack you with") {
        val d = setup()
        d.chooseMode("Enclave")
        val bears = d.attackers(3)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, bears, d.player1).error shouldBe null
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.rad(d.player2) shouldBe 6
        d.rad(d.player1) shouldBe 0
    }

    test("Enclave's count is fixed at declaration: removing an attacker from combat doesn't lower it") {
        val d = setup()
        d.chooseMode("Enclave")
        val bears = d.attackers(2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, bears, d.player1).error shouldBe null
        d.passPriority(d.player2).error shouldBe null
        val removal = d.putCardInHand(d.player1, removeFromCombat.name)
        d.castSpell(d.player1, removal, listOf(bears.first())).error shouldBe null
        d.bothPass()
        d.state.getEntity(bears.first())?.has<AttackingComponent>() shouldBe false
        d.bothPass()
        d.rad(d.player2) shouldBe 4
    }

    test("Brotherhood has no attack trigger, and Enclave has no upkeep trigger") {
        val d = setup()
        d.chooseMode("Brotherhood")
        val bears = d.attackers(2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, bears, d.player1).error shouldBe null
        d.state.stack.isEmpty() shouldBe true
        d.rad(d.player2) shouldBe 0

        val e = setup()
        e.chooseMode("Enclave")
        e.passPriorityUntil(Step.UPKEEP, e.player1)
        e.state.stack.isEmpty() shouldBe true
    }

    test("Brotherhood: at your upkeep each opponent draws a card, then you draw one for each card drawn") {
        val d = setup()
        d.chooseMode("Brotherhood")
        d.passPriorityUntil(Step.UPKEEP, d.player1)
        d.state.stack.size shouldBe 1
        val yours = d.getHandSize(d.player1)
        val theirs = d.getHandSize(d.player2)
        d.bothPass()
        d.getHandSize(d.player2) shouldBe theirs + 1
        d.getHandSize(d.player1) shouldBe yours + 1
    }
})
