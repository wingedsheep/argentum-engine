package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TrashTheTown
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Trash the Town (OTJ Spree instant), {G}.
 *
 * + {2} — Put two +1/+1 counters on target creature.
 * + {1} — Target creature gains trample until end of turn.
 * + {1} — Until end of turn, target creature gains "Whenever this creature deals combat
 *   damage to a player, draw two cards."
 */
class TrashTheTownScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TrashTheTown)
        return driver
    }

    fun GameTestDriver.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("Mode 1 ({2}) puts two +1/+1 counters on target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2
        driver.plusCounters(bear) shouldBe 0

        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 2)      // {2} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
            )
        )
        driver.bothPass()

        driver.plusCounters(bear) shouldBe 2
    }

    test("Mode 2 ({1}) grants trample until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe false

        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 1)      // {1} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
            )
        )
        driver.bothPass()

        driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
    }

    test("Mode 3 ({1}) grants 'deals combat damage to a player → draw two' for the turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(bear)

        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 1)      // {1} for mode 2
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
            )
        )
        driver.bothPass()

        val handBefore = driver.getHandSize(player)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bear), opponent)
        // Resolve combat damage; the granted trigger fires on combat damage to the opponent.
        driver.passPriorityUntil(Step.END_COMBAT)

        // Drew two cards from the granted ability.
        driver.getHandSize(player) shouldBe handBefore + 2
    }

    test("Multiple modes ({2}+{1}) apply both counters and trample to the same creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")

        val spell = driver.putCardInHand(player, "Trash the Town")
        driver.giveMana(player, Color.GREEN, 1) // {G}
        driver.giveColorlessMana(player, 3)      // {2} + {1}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear), ChosenTarget.Permanent(bear)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(bear)),
                    listOf(ChosenTarget.Permanent(bear))
                )
            )
        )
        driver.bothPass()

        driver.plusCounters(bear) shouldBe 2
        driver.state.projectedState.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
    }
})
