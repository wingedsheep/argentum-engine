package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RequisitionRaid
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Requisition Raid (OTJ Spree sorcery).
 *
 * Mode 0/1: destroy target artifact / enchantment. Mode 2 ({1}): put a +1/+1 counter
 * on each creature the targeted player controls (`ForEachInGroup` over a target-relative
 * group filter, counter placed on each iterated permanent via `EffectTarget.Self`).
 */
class OtjRequisitionRaidScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RequisitionRaid)
        return driver
    }

    test("Mode 2 puts a +1/+1 counter on each creature the targeted player controls") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val a = driver.putCreatureOnBattlefield(player, "Black Creature") // 2/2
        val b = driver.putCreatureOnBattlefield(player, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Requisition Raid")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 2
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Player(player)),
                chosenModes = listOf(2),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Player(player)))
            )
        )
        driver.bothPass()

        // Both 2/2 creatures get a +1/+1 counter -> 3/3.
        projector.getProjectedPower(driver.state, a) shouldBe 3
        projector.getProjectedToughness(driver.state, a) shouldBe 3
        projector.getProjectedPower(driver.state, b) shouldBe 3
        projector.getProjectedToughness(driver.state, b) shouldBe 3
    }

    test("Mode 0 destroys target artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putCreatureOnBattlefield(opponent, "Artifact Creature") // 2/2 artifact

        val spell = driver.putCardInHand(player, "Requisition Raid")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 1)      // {1} for mode 0
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(artifact)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(artifact)))
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
    }

    test("Multiple modes: destroy an artifact AND counter up your team") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val artifact = driver.putCreatureOnBattlefield(opponent, "Artifact Creature")
        val mine = driver.putCreatureOnBattlefield(player, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Requisition Raid")
        driver.giveMana(player, Color.WHITE, 1) // {W} base
        driver.giveColorlessMana(player, 2)      // {1} (mode 0) + {1} (mode 2)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(artifact), ChosenTarget.Player(player)),
                chosenModes = listOf(0, 2),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(artifact)),
                    listOf(ChosenTarget.Player(player))
                )
            )
        )
        driver.bothPass()

        driver.findPermanent(opponent, "Artifact Creature") shouldBe null
        projector.getProjectedPower(driver.state, mine) shouldBe 3
    }
})
