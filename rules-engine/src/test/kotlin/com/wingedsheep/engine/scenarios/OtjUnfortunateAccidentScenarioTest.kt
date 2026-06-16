package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.UnfortunateAccident
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Unfortunate Accident (OTJ Spree instant).
 *
 * Mode 0 ({2}{B}): destroy target creature. Mode 1 ({1}): create a 1/1 red Mercenary
 * token with the standard OTJ "{T}: target creature you control gets +1/+0" pump.
 */
class OtjUnfortunateAccidentScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + UnfortunateAccident)
        return driver
    }

    test("Mode 0 destroys target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Unfortunate Accident")
        driver.giveMana(player, Color.BLACK, 2) // {B} base + {B} of {2}{B}
        driver.giveColorlessMana(player, 2)      // {2} of {2}{B}
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(victim)))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("Mode 1 creates a 1/1 red Mercenary token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val battlefieldBefore = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size

        val spell = driver.putCardInHand(player, "Unfortunate Accident")
        driver.giveMana(player, Color.BLACK, 1) // {B} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 1
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(emptyList())
            )
        )
        driver.bothPass()

        // One new permanent on the battlefield (the Mercenary token).
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size shouldBe battlefieldBefore + 1
    }

    test("Both modes: destroy a creature AND create a token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature")
        val battlefieldBefore = driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size

        val spell = driver.putCardInHand(player, "Unfortunate Accident")
        driver.giveMana(player, Color.BLACK, 2) // {B} base + {B}
        driver.giveColorlessMana(player, 3)      // {2} (mode 0) + {1} (mode 1)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(victim)),
                    emptyList()
                )
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Black Creature") shouldBe null
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).size shouldBe battlefieldBefore + 1
    }
})
