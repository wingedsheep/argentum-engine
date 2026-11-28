package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dka.cards.SpitefulShadows
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spiteful Shadows (DKA #75) — "Enchant creature. Whenever enchanted creature is dealt damage, it
 * deals that much damage to its controller."
 *
 * Pins: the damage lands on the *creature's* controller (not the Aura's), it is the full amount,
 * and it still happens when the damage was lethal — the creature, by then in the graveyard with
 * this Aura, deals it as it last existed.
 */
class SpitefulShadowsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SpitefulShadows)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.enchant(caster: EntityId, creature: EntityId) {
        val aura = putCardInHand(caster, "Spiteful Shadows")
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

    test("the enchanted creature deals the damage it was dealt to its controller") {
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
        withClue("the Aura's controller takes nothing") {
            d.getLifeTotal(d.player1) shouldBe enchanterBefore
        }
    }

    test("lethal damage still comes back at the dead creature's controller") {
        val d = driver()
        val victim = d.putCreatureOnBattlefield(d.player2, "Centaur Courser") // 3/3, dies to a Bolt
        d.enchant(d.player1, victim)

        val victimBefore = d.getLifeTotal(d.player2)

        d.bolt(d.player1, victim)
        resolveStack(d)

        withClue("the creature died to the Bolt") {
            d.getCreatures(d.player2).contains(victim) shouldBe false
        }
        withClue("the departed creature still deals that much damage to its controller") {
            d.getLifeTotal(d.player2) shouldBe victimBefore - 3
        }
    }

    test("enchanting your own creature punishes you, not the opponent who damaged it") {
        val d = driver()
        val mine = d.putCreatureOnBattlefield(d.player1, "Force of Nature")
        d.enchant(d.player1, mine)

        val meBefore = d.getLifeTotal(d.player1)
        val themBefore = d.getLifeTotal(d.player2)

        d.bolt(d.player1, mine)
        resolveStack(d)

        d.getLifeTotal(d.player1) shouldBe meBefore - 3
        d.getLifeTotal(d.player2) shouldBe themBefore
    }
})
