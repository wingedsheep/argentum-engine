package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RakishScoundrel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Rakish Scoundrel — {2}{B}{G} 3/3 Elf Rogue with deathtouch, Disguise {4}{B/G}{B/G}, and "when
 * this creature enters **or** is turned face up, target creature gains indestructible until end of
 * turn."
 *
 * The trigger's two conditions are one ability, so the hard-cast line gets the indestructible on
 * entry and can also target the Scoundrel itself — a 3/3 deathtouch blocker that survives anything.
 */
class RakishScoundrelScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RakishScoundrel))
        return driver
    }

    /**
     * Drive the game until the entry trigger's target prompt has been answered with the creature
     * [chooseTarget] returns and the stack is empty again. Stops there deliberately: the granted
     * indestructible lasts only until end of turn, so passing priority further would expire it.
     */
    fun GameTestDriver.settleEntryTrigger(
        player: com.wingedsheep.sdk.model.EntityId,
        chooseTarget: () -> com.wingedsheep.sdk.model.EntityId
    ) {
        var answered = false
        repeat(8) {
            if (pendingDecision is ChooseTargetsDecision) {
                submitTargetSelection(player, listOf(chooseTarget()))
                answered = true
            } else if (answered && pendingDecision == null && stackSize == 0) {
                return
            } else if (state.priorityPlayerId != null && !isPaused) {
                bothPass()
            }
        }
    }

    test("hard-cast: a 3/3 deathtouch whose entry grants indestructible to a chosen creature") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val card = driver.putCardInHand(player, "Rakish Scoundrel")
        driver.giveMana(player, Color.BLACK, 3)
        driver.giveMana(player, Color.GREEN, 1)

        driver.castSpell(player, card).error shouldBe null
        driver.settleEntryTrigger(player) { bear }

        val scoundrel = driver.findPermanent(player, "Rakish Scoundrel")
        scoundrel.shouldNotBeNull()
        val projected = driver.state.projectedState
        projected.getPower(scoundrel) shouldBe 3
        projected.getToughness(scoundrel) shouldBe 3
        projected.hasKeyword(scoundrel, Keyword.DEATHTOUCH) shouldBe true
        projected.hasKeyword(bear, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("the entry trigger can target the Scoundrel itself") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = driver.putCardInHand(player, "Rakish Scoundrel")
        driver.giveMana(player, Color.BLACK, 3)
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, card).error shouldBe null

        driver.settleEntryTrigger(player) { driver.findPermanent(player, "Rakish Scoundrel")!! }

        val scoundrel = driver.findPermanent(player, "Rakish Scoundrel")
        scoundrel.shouldNotBeNull()
        driver.state.projectedState.hasKeyword(scoundrel, Keyword.INDESTRUCTIBLE) shouldBe true
    }
})
