package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.RaggedVeins
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ragged Veins (CHK #139) — "Flash. Enchant creature. Whenever enchanted creature is dealt
 * damage, its controller loses that much life."
 *
 * Pins: the loss lands on the *creature's* controller (not the Aura's), it is the full damage
 * amount, it still happens when the damage is lethal (last-known controller), and flash lets the
 * Aura be cast with a spell already on the stack.
 */
class RaggedVeinsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + RaggedVeins)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.enchant(caster: EntityId, creature: EntityId) {
        val aura = putCardInHand(caster, "Ragged Veins")
        giveMana(caster, Color.BLACK, 2)
        castSpell(caster, aura, listOf(creature)).outcome shouldBe Outcome.Done
        bothPass()
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        val bolt = putCardInHand(caster, "Lightning Bolt")
        giveMana(caster, Color.RED, 1)
        castSpell(caster, bolt, listOf(target)).outcome shouldBe Outcome.Done
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("the enchanted creature's controller loses life equal to the damage dealt") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Force of Nature") // 5/5, survives a Bolt
        d.enchant(d.player1, victim)

        val enchanterBefore = d.getLifeTotal(d.player1)
        val victimBefore = d.getLifeTotal(d.player2)

        d.bolt(d.player1, victim)
        resolveStack(d)

        withClue("'its controller' is the creature's controller, and 'that much' is 3") {
            d.getLifeTotal(d.player2) shouldBe victimBefore - 3
        }
        withClue("the Aura's controller loses nothing") {
            d.getLifeTotal(d.player1) shouldBe enchanterBefore
        }
    }

    test("lethal damage still makes the dead creature's controller lose that much life") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3, dies to a Bolt
        d.enchant(d.player1, victim)

        val victimBefore = d.getLifeTotal(d.player2)

        d.bolt(d.player1, victim)
        resolveStack(d)

        withClue("the creature died to the Bolt") {
            d.getCreatures(d.player2).contains(victim) shouldBe false
        }
        withClue("the trigger reads the controller as the damage was dealt") {
            d.getLifeTotal(d.player2) shouldBe victimBefore - 3
        }
    }

    test("flash: it can be cast in response to a burn spell and punish that damage") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        val victimBefore = d.getLifeTotal(d.player2)

        d.bolt(d.player1, victim)
        withClue("the Bolt is on the stack, so only an instant-speed Aura can be cast now") {
            d.state.stack.isNotEmpty() shouldBe true
        }
        val aura = d.putCardInHand(d.player1, "Ragged Veins")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, aura, listOf(victim)).outcome shouldBe Outcome.Done
        resolveStack(d)

        withClue("the Aura resolved first, then the Bolt's damage triggered it") {
            d.getLifeTotal(d.player2) shouldBe victimBefore - 3
        }
    }

    test("damage to an unenchanted creature costs its controller nothing") {
        val d = driver()
        val enchanted = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        val other = d.putCreatureOnBattlefield(d.player2, "Force of Nature")
        d.enchant(d.player1, enchanted)
        val before = d.getLifeTotal(d.player2)

        d.bolt(d.player1, other)
        resolveStack(d)

        d.getLifeTotal(d.player2) shouldBe before
    }
})
