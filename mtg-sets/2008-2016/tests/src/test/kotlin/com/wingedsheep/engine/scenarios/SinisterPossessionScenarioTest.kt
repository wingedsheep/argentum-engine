package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.SinisterPossession
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Sinister Possession (DGM #29) — "Whenever enchanted creature attacks or blocks, its controller
 * loses 2 life."
 *
 * A punisher Aura goes on someone *else's* creature, so the two things that can silently break it
 * are (a) the blocks half never firing — the engine's attachment trigger detector has no block
 * branch, which is why the card grants the triggers to the creature instead of keeping them on the
 * Aura — and (b) the life loss landing on the Aura's controller rather than the creature's. Both
 * are pinned here from the enchanter's seat.
 */
class SinisterPossessionScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SinisterPossession)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.enchant(caster: EntityId, creature: EntityId) {
        val aura = putCardInHand(caster, "Sinister Possession")
        giveMana(caster, Color.BLACK, 1)
        castSpell(caster, aura, listOf(creature)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("the enchanted creature's controller loses 2 when it attacks — not the enchanter") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears") // 2/2
        d.removeSummoningSickness(victim)
        // player1 owns the Aura but controls no creature; the attack is player2's, on their turn.
        d.enchant(d.player1, victim)

        val enchanterLifeBefore = d.getLifeTotal(d.player1)
        val victimLifeBefore = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, listOf(victim), defendingPlayer = d.player1).error shouldBe null
        resolveStack(d)

        // The trigger resolves in the declare-attackers step, before any combat damage, so the
        // enchanter's life is a clean read of "did the loss land on the wrong player".
        withClue("'its controller' is the creature's controller") {
            d.getLifeTotal(d.player2) shouldBe victimLifeBefore - 2
        }
        withClue("the Aura's controller pays nothing for the trigger") {
            d.getLifeTotal(d.player1) shouldBe enchanterLifeBefore
        }
    }

    test("the blocks half fires too") {
        val d = driver()
        val attacker = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears") // 2/2
        val victim = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3, survives
        d.removeSummoningSickness(attacker)
        d.enchant(d.player1, victim)

        val victimLifeBefore = d.getLifeTotal(d.player2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.activePlayer shouldBe d.player1
        d.declareAttackers(d.player1, listOf(attacker), defendingPlayer = d.player2).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, mapOf(victim to listOf(attacker)))
        resolveStack(d)

        withClue("blocking triggers the Possession; the block itself keeps all combat damage off player2") {
            d.getLifeTotal(d.player2) shouldBe victimLifeBefore - 2
        }
    }

    test("an unenchanted creature attacking costs its controller nothing") {
        val d = driver()
        val plain = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(plain)

        val before = d.getLifeTotal(d.player2)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, listOf(plain), defendingPlayer = d.player1).error shouldBe null
        resolveStack(d)

        withClue("negative control: the trigger comes from the Aura, not from attacking") {
            d.getLifeTotal(d.player2) shouldBe before
        }
    }
})
