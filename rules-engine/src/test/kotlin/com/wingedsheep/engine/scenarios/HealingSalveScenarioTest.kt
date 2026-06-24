package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.HealingSalve
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Healing Salve — {W} Instant
 * Choose one —
 * • Target player gains 3 life.              (mode 0)
 * • Prevent the next 3 damage that would be  (mode 1)
 *   dealt to any target this turn.
 *
 * Mode 1 uses "any target", so the prevention shield must land on either a creature or a
 * player. Both are exercised below by following Healing Salve with a 3-damage Lightning Bolt.
 */
class HealingSalveScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(HealingSalve)
        return d
    }

    fun setup(d: GameTestDriver): Pair<com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        d.initMirrorMatch(deck = Deck.of("Plains" to 20, "Mountain" to 20), startingLife = 20)
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return p1 to d.getOpponent(p1)
    }

    test("mode 0 — target player gains 3 life") {
        val d = driver()
        val (p1, _) = setup(d)
        d.giveMana(p1, Color.WHITE, 1)

        val salve = d.putCardInHand(p1, "Healing Salve")
        val result = d.submit(CastSpell(
            playerId = p1,
            cardId = salve,
            targets = listOf(ChosenTarget.Player(p1)),
            chosenModes = listOf(0),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(p1)))
        ))
        if (!result.isSuccess) throw AssertionError("cast failed: ${result.error}")
        d.bothPass()

        d.getLifeTotal(p1) shouldBe 23
    }

    test("mode 1 — prevent the next 3 damage to a creature (any target)") {
        val d = driver()
        val (p1, _) = setup(d)
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.RED, 1)

        val giant = d.putCreatureOnBattlefield(p1, "Hill Giant") // 3/3

        // Healing Salve: prevent the next 3 damage to the Hill Giant this turn.
        val salve = d.putCardInHand(p1, "Healing Salve")
        val cast = d.submit(CastSpell(
            playerId = p1,
            cardId = salve,
            targets = listOf(ChosenTarget.Permanent(giant)),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(giant)))
        ))
        if (!cast.isSuccess) throw AssertionError("cast failed: ${cast.error}")
        d.bothPass()

        // Lightning Bolt deals 3 → shield prevents all 3 → no damage marked, creature survives.
        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Permanent(giant)))
        d.bothPass()

        (d.state.getEntity(giant)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
        // 3/3 took no damage → survives state-based actions.
        d.findPermanent(p1, "Hill Giant").shouldNotBeNull()
    }

    test("mode 1 — prevent the next 3 damage to a player (any target)") {
        val d = driver()
        val (p1, p2) = setup(d)
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.RED, 1)

        // Shield the opponent from the next 3 damage this turn.
        val salve = d.putCardInHand(p1, "Healing Salve")
        val cast = d.submit(CastSpell(
            playerId = p1,
            cardId = salve,
            targets = listOf(ChosenTarget.Player(p2)),
            chosenModes = listOf(1),
            modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(p2)))
        ))
        if (!cast.isSuccess) throw AssertionError("cast failed: ${cast.error}")
        d.bothPass()

        // Lightning Bolt deals 3 to p2 → all prevented → life stays at 20.
        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        d.castSpellWithTargets(p1, bolt, listOf(ChosenTarget.Player(p2)))
        d.bothPass()

        d.getLifeTotal(p2) shouldBe 20
    }
})
