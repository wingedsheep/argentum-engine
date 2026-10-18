package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.BitingPalmNinja
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Biting-Palm Ninja — {2}{B} Creature — Human Ninja 3/3
 * Ninjutsu {2}{B}
 * This creature enters with a menace counter on it.
 * Whenever this creature deals combat damage to a player, you may remove a menace counter from it.
 * When you do, that player reveals their hand and you choose a nonland card from it. Exile that card.
 *
 * Pins the CR 603.12 gate: the exile is paid for with the menace counter, so once the counter is
 * gone the may-prompt never appears and the damaged player's hand is untouched.
 */
class BitingPalmNinjaScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + BitingPalmNinja)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun menaceCounters(d: GameTestDriver, id: EntityId): Int =
        d.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.MENACE) ?: 0

    fun addMenaceCounter(d: GameTestDriver, id: EntityId) {
        d.replaceState(
            d.state.updateEntity(id) { c ->
                c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.MENACE, 1))
            }
        )
    }

    fun attackUnblocked(d: GameTestDriver, attacker: EntityId) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(attacker), d.player2).outcome shouldBe Outcome.Done
        d.bothPass()
        d.declareNoBlockers(d.player2)
    }

    /** Resolve everything, answering the may with [yes]; returns how many yes/no prompts appeared. */
    fun settle(d: GameTestDriver, yes: Boolean, pick: EntityId? = null): Int {
        var prompts = 0
        var guard = 0
        while (guard++ < 30 && (d.state.stack.isNotEmpty() || d.pendingDecision != null ||
                d.state.step != Step.POSTCOMBAT_MAIN)) {
            when (val pd = d.pendingDecision) {
                is YesNoDecision -> { prompts++; d.submitYesNo(pd.playerId, yes).error shouldBe null }
                is SelectCardsDecision -> {
                    pd.options shouldContain pick!!
                    d.submitCardSelection(pd.playerId, listOf(pick)).error shouldBe null
                }
                null -> d.bothPass()
                else -> error("unexpected decision $pd")
            }
        }
        return prompts
    }

    test("enters with a menace counter, which grants menace") {
        val d = driver()
        val ninja = d.putCardInHand(d.player1, BitingPalmNinja.name)
        repeat(3) { d.putLandOnBattlefield(d.player1, "Swamp") }
        d.castSpell(d.player1, ninja).outcome shouldBe Outcome.Done
        while (d.state.stack.isNotEmpty()) d.bothPass()

        val onField = d.findPermanent(d.player1, BitingPalmNinja.name)!!
        menaceCounters(d, onField) shouldBe 1
        d.state.projectedState.hasKeyword(onField, Keyword.MENACE) shouldBe true
    }

    test("removing the menace counter exiles a chosen nonland card from the damaged player's hand") {
        val d = driver()
        val bears = d.putCardInHand(d.player2, "Grizzly Bears")
        val swamp = d.putCardInHand(d.player2, "Swamp")
        val ninja = d.putCreatureOnBattlefield(d.player1, BitingPalmNinja.name).also(d::removeSummoningSickness)
        addMenaceCounter(d, ninja)
        attackUnblocked(d, ninja)

        settle(d, yes = true, pick = bears) shouldBe 1

        menaceCounters(d, ninja) shouldBe 0
        d.getExileCardNames(d.player2) shouldContain "Grizzly Bears"
        d.getHand(d.player2) shouldNotContain bears
        d.getHand(d.player2) shouldContain swamp
    }

    test("with no menace counter left there is no prompt and nothing is exiled") {
        val d = driver()
        val bears = d.putCardInHand(d.player2, "Grizzly Bears")
        val ninja = d.putCreatureOnBattlefield(d.player1, BitingPalmNinja.name).also(d::removeSummoningSickness)
        menaceCounters(d, ninja) shouldBe 0
        attackUnblocked(d, ninja)

        settle(d, yes = true) shouldBe 0

        d.getLifeTotal(d.player2) shouldBe 17
        d.getHand(d.player2) shouldContain bears
    }
})
