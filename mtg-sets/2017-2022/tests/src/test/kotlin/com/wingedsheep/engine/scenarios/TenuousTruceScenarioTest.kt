package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ncc.cards.TenuousTruce
import com.wingedsheep.mtg.sets.definitions.war.cards.NissaWhoShakesTheWorld
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tenuous Truce {1}{W} — Enchantment — Aura
 *   Enchant opponent
 *   At the beginning of enchanted opponent's end step, you and that player each draw a card.
 *   When you attack enchanted opponent or a planeswalker they control or when they attack you or a
 *   planeswalker you control, sacrifice this Aura.
 *
 * Three players, player 1 always active. Each test puts the Truce under some player's control and
 * attaches it to another, then has player 1 declare attackers. Both sacrifice legs are scoped by
 * the attachment, so attacks involving a third player must leave the Truce in play.
 */
class TenuousTruceScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val p1: EntityId, val p2: EntityId, val p3: EntityId)

    fun table(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TenuousTruce + NissaWhoShakesTheWorld)
        val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Plains" to 40) })
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Table(d, p1, p2, p3)
    }

    fun Table.truce(controller: EntityId, enchanted: EntityId): EntityId {
        val truce = d.putPermanentOnBattlefield(controller, "Tenuous Truce")
        d.addComponent(truce, AttachedToComponent(enchanted))
        return truce
    }

    fun Table.bears(): EntityId =
        d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }

    fun Table.nissa(controller: EntityId): EntityId {
        val nissa = d.putPermanentOnBattlefield(controller, "Nissa, Who Shakes the World")
        d.addComponent(nissa, CountersComponent(mapOf(CounterType.LOYALTY to 5)))
        return nissa
    }

    fun Table.attack(attackers: Map<EntityId, EntityId>) {
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, attackers).error shouldBe null
        var guard = 0
        while (guard++ < 10 && d.stackSize > 0) d.bothPass()
    }

    fun Table.truceInPlay(truce: EntityId): Boolean = truce in d.state.getBattlefield()

    context("sacrifice — you attack the enchanted opponent") {
        test("attacking the enchanted opponent sacrifices it") {
            val t = table()
            val truce = t.truce(controller = t.p1, enchanted = t.p2)
            t.attack(mapOf(t.bears() to t.p2))
            t.truceInPlay(truce) shouldBe false
            t.d.getGraveyardCardNames(t.p1).contains("Tenuous Truce") shouldBe true
        }

        test("attacking a planeswalker the enchanted opponent controls sacrifices it") {
            val t = table()
            val truce = t.truce(controller = t.p1, enchanted = t.p2)
            val nissa = t.nissa(t.p2)
            t.attack(mapOf(t.bears() to nissa))
            t.truceInPlay(truce) shouldBe false
        }

        test("attacking a different opponent, or their planeswalker, leaves it in play") {
            val t = table()
            val truce = t.truce(controller = t.p1, enchanted = t.p2)
            val nissa = t.nissa(t.p3)
            val bears = t.bears()
            val courser = t.d.putCreatureOnBattlefield(t.p1, "Centaur Courser")
                .also { t.d.removeSummoningSickness(it) }
            t.attack(mapOf(bears to t.p3, courser to nissa))
            withClue("player 3 isn't the enchanted opponent") { t.truceInPlay(truce) shouldBe true }
        }
    }

    context("sacrifice — the enchanted opponent attacks you") {
        test("the enchanted player attacking you sacrifices it") {
            val t = table()
            // Player 2 controls the Truce on player 1 (the active player), who attacks player 2.
            val truce = t.truce(controller = t.p2, enchanted = t.p1)
            t.attack(mapOf(t.bears() to t.p2))
            t.truceInPlay(truce) shouldBe false
        }

        test("the enchanted player attacking a planeswalker you control sacrifices it") {
            val t = table()
            val truce = t.truce(controller = t.p2, enchanted = t.p1)
            val nissa = t.nissa(t.p2)
            t.attack(mapOf(t.bears() to nissa))
            t.truceInPlay(truce) shouldBe false
        }

        test("the enchanted player attacking someone else leaves it in play") {
            val t = table()
            val truce = t.truce(controller = t.p2, enchanted = t.p1)
            t.attack(mapOf(t.bears() to t.p3))
            withClue("player 1 attacked player 3, not the Truce's controller") {
                t.truceInPlay(truce) shouldBe true
            }
        }

        test("a player who isn't enchanted attacking you leaves it in play") {
            val t = table()
            // Player 2 controls the Truce on player 3; player 1 (not enchanted) attacks player 2.
            val truce = t.truce(controller = t.p2, enchanted = t.p3)
            t.attack(mapOf(t.bears() to t.p2))
            t.truceInPlay(truce) shouldBe true
        }
    }

    test("at the enchanted opponent's end step, you and that player each draw a card") {
        val t = table()
        // Player 2 controls the Truce on player 1, whose turn it is.
        val truce = t.truce(controller = t.p2, enchanted = t.p1)
        val hands = listOf(t.p1, t.p2, t.p3).associateWith { t.d.getHandSize(it) }
        t.d.passPriorityUntil(Step.END)
        var guard = 0
        while (guard++ < 10 && t.d.stackSize > 0) t.d.bothPass()
        t.truceInPlay(truce) shouldBe true
        t.d.getHandSize(t.p1) shouldBe hands.getValue(t.p1) + 1
        t.d.getHandSize(t.p2) shouldBe hands.getValue(t.p2) + 1
        withClue("player 3 is neither the Truce's controller nor the enchanted player") {
            t.d.getHandSize(t.p3) shouldBe hands.getValue(t.p3)
        }
    }
})
