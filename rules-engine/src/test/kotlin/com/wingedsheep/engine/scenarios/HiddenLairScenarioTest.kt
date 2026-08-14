package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hidden Lair (MSH) — "{T}: Add {C}.\n{T}: Add {U} or {B}. Activate only if this land entered
 * this turn or if you control a basic land."
 *
 * The colored ability's gate ([com.wingedsheep.sdk.scripting.ActivationRestriction.OnlyIfCondition]
 * over `Conditions.SourceEnteredThisTurn` OR `Conditions.YouControl(Filters.BasicLand)`) exposed a
 * real engine gap: [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] never stamped
 * `EnteredThisTurnComponent` on a played land — it bypasses `ZoneTransitionService`, which is where
 * every other zone-change path sets that component, and the manual re-baking `PlayLandHandler` does
 * for statics/replacements never covered it. So *any* land-based "entered this turn" check silently
 * never fired, even on the very turn the land was played. Fixed alongside this card; these tests
 * pin the fix down.
 */
class HiddenLairScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun coloredAbilityOffered(driver: GameTestDriver, playerId: EntityId, hiddenLairId: EntityId): Boolean =
        driver.legalActions(playerId).any {
            val action = it.action
            action is ActivateAbility && action.sourceId == hiddenLairId && it.description.contains("{U}")
        }

    test("a freshly played Hidden Lair offers the colored ability the same turn, with no basic land in play") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val hiddenLair = driver.putCardInHand(you, "Hidden Lair")
        driver.submitSuccess(PlayLand(playerId = you, cardId = hiddenLair))

        coloredAbilityOffered(driver, you, hiddenLair) shouldBe true
    }

    test("a Hidden Lair that has been in play since before this turn loses the colored ability without a basic land") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val hiddenLair = driver.putLandOnBattlefield(you, "Hidden Lair")

        coloredAbilityOffered(driver, you, hiddenLair) shouldBe false
    }

    test("a basic land in play restores the colored ability even when Hidden Lair didn't enter this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val hiddenLair = driver.putLandOnBattlefield(you, "Hidden Lair")
        coloredAbilityOffered(driver, you, hiddenLair) shouldBe false

        driver.putLandOnBattlefield(you, "Island")
        coloredAbilityOffered(driver, you, hiddenLair) shouldBe true
    }
})
