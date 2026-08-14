package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shadow of the Goblin (SPM), Undying Vengeance — "Whenever you play a land … from anywhere other
 * than your hand, this enchantment deals 1 damage to each opponent." Pins the new
 * `Triggers.youPlayLand(fromZoneOtherThan = Zone.HAND)` (`LandPlayedEvent`) end-to-end, exercised by
 * playing Oscorp Industries from the graveyard via Mayhem.
 *
 * Shadow is placed *after* reaching the first main phase so its own first-main loot trigger (which
 * already fired at the step boundary) doesn't interfere.
 */
class ShadowOfTheGoblinScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun markDiscarded(driver: GameTestDriver, playerId: EntityId, cardId: EntityId) {
        val prior = driver.state.getEntity(playerId)
            ?.get<CardsDiscardedThisTurnComponent>() ?: CardsDiscardedThisTurnComponent()
        driver.addComponent(playerId, prior.copy(cardIds = prior.cardIds + cardId, count = prior.count + 1))
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("playing a land from the graveyard (non-hand) deals 1 to each opponent") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        val oscorp = driver.putCardInGraveyard(player, "Oscorp Industries")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Shadow of the Goblin")
        markDiscarded(driver, player, oscorp)

        driver.submit(PlayLand(player, oscorp)).error shouldBe null
        resolveStack(driver) // Shadow's damage trigger + Oscorp's enters-from-graveyard both resolve

        // Shadow dealt 1 to the opponent; Oscorp's −2 hit the player (its controller), not the opponent.
        driver.getLifeTotal(opponent) shouldBe 19
    }

    test("playing a land from hand does not trigger Undying Vengeance") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.state.turnOrder.first { it != player }

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.putPermanentOnBattlefield(player, "Shadow of the Goblin")
        val swamp = driver.putCardInHand(player, "Swamp")

        driver.submit(PlayLand(player, swamp)).error shouldBe null
        resolveStack(driver)

        // Land played from hand — no damage to the opponent.
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
