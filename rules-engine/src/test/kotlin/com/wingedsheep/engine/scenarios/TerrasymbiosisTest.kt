package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoe.cards.Terrasymbiosis
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Terrasymbiosis ({2}{G} Enchantment):
 * "Whenever you put one or more +1/+1 counters on a creature you control, you may draw
 *  that many cards. Do this only once each turn."
 *
 * Regression: the card declared its draw with a bare `optional = true` on a no-target
 * triggered ability. The engine silently ignores that flag for no-target / no-elseEffect
 * abilities (it only wires `optional` for targeted abilities or ones with an elseEffect),
 * so the draw happened unconditionally and the player was never offered the "may" choice.
 * The fix wraps the draw in `MayEffect`; these tests pin both branches of the choice and
 * the once-per-turn gate.
 */
class TerrasymbiosisTest : FunSpec({

    // Inline instant placing TWO +1/+1 counters on a creature you control, so we can verify
    // the player draws "that many" (the dynamic count), not just a single card.
    val counterInfusion = card("Counter Infusion") {
        manaCost = "{G}"
        typeLine = "Instant"
        oracleText = "Put two +1/+1 counters on target creature you control."
        spell {
            val target = target("target creature you control", Targets.CreatureYouControl)
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, target)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Terrasymbiosis, counterInfusion))
        return driver
    }

    fun plusCounters(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("choosing yes draws one card per +1/+1 counter placed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Terrasymbiosis")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 1)

        val handBefore = driver.getHandSize(player)

        // Place two +1/+1 counters → Terrasymbiosis triggers.
        driver.castSpell(player, spell, targets = listOf(creature))
        driver.bothPass()   // resolve Counter Infusion; the trigger goes on the stack
        driver.bothPass()   // resolve the trigger → MayEffect yes/no decision

        // The "may" choice must be offered. Accept it.
        driver.submitYesNo(player, true)

        plusCounters(driver, creature) shouldBe 2
        // Cast the instant from hand (-1), then drew 2 (+2): net +1.
        driver.getHandSize(player) shouldBe handBefore - 1 + 2
    }

    test("choosing no draws nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Terrasymbiosis")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 1)

        val handBefore = driver.getHandSize(player)

        driver.castSpell(player, spell, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()

        // Decline the "may" — no cards are drawn.
        driver.submitYesNo(player, false)

        plusCounters(driver, creature) shouldBe 2
        // Only the cast instant left hand; nothing drawn.
        driver.getHandSize(player) shouldBe handBefore - 1
    }

    test("triggers only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Terrasymbiosis")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell1 = driver.putCardInHand(player, "Counter Infusion")
        val spell2 = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 2)

        val handBefore = driver.getHandSize(player)

        // First counter placement: trigger fires, draw 2.
        driver.castSpell(player, spell1, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, true)

        // Second counter placement this turn: "Do this only once each turn" — no trigger,
        // so no second yes/no decision and no further draw.
        driver.castSpell(player, spell2, targets = listOf(creature))
        driver.bothPass()

        plusCounters(driver, creature) shouldBe 4
        // Two instants cast (-2) and exactly one draw of 2 (+2): net zero.
        driver.getHandSize(player) shouldBe handBefore - 2 + 2
    }

    test("declining does not spend the turn — a later placement still offers the draw") {
        // CR 603.2h keys "Do this only once each turn" to the *draw*, not to the trigger. Under
        // the old `oncePerTurn` modelling the first declined trigger burned the turn and the
        // second placement was never offered, which is the defect this pins.
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)

        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(player, "Terrasymbiosis")
        val creature = driver.putCreatureOnBattlefield(player, "Savannah Lions")
        val spell1 = driver.putCardInHand(player, "Counter Infusion")
        val spell2 = driver.putCardInHand(player, "Counter Infusion")
        driver.giveMana(player, Color.GREEN, 2)

        val handBefore = driver.getHandSize(player)

        // Decline the first placement — nothing drawn, and the turn's use is left unspent.
        driver.castSpell(player, spell1, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, false)

        // Second placement the same turn: the ability triggers again and the draw is on offer.
        driver.castSpell(player, spell2, targets = listOf(creature))
        driver.bothPass()
        driver.bothPass()
        driver.submitYesNo(player, true)

        plusCounters(driver, creature) shouldBe 4
        // Two instants cast (-2), and the accepted second draw of 2 (+2).
        driver.getHandSize(player) shouldBe handBefore - 2 + 2
    }
})
