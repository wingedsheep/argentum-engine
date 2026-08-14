package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.KeyToTheSideDoor
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Key to the Side-Door (HOB #175) — {1} Artifact.
 *
 *   {2}, {T}: Target creature can't be blocked this turn.
 *   {1}, {T}, Discard a legendary card with the same name as a legendary permanent you control:
 *     Draw two cards.
 *
 * The second ability carries the new `SharesNameWithPermanentYouControl` cost filter, so these
 * tests pin down what the discard cost will and won't accept: a hand card whose name matches a
 * legendary permanent you control pays it; a legendary card with a *different* name doesn't; and
 * neither does a name match against a permanent an opponent controls.
 */
class KeyToTheSideDoorScenarioTest : FunSpec({

    val drawAbilityId = KeyToTheSideDoor.activatedAbilities[1].id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun startAtMain(driver: GameTestDriver): EntityId {
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40, "Grizzly Bears" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return player
    }

    test("discarding a legendary card that names a legendary permanent you control draws two") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val key = driver.putPermanentOnBattlefield(player, "Key to the Side-Door")
        driver.giveMana(player, Color.WHITE, 1)
        driver.putCreatureOnBattlefield(player, "Bilbo, Luckwearer")

        val handSizeBefore = driver.getHand(player).size
        val duplicate = driver.putCardInHand(player, "Bilbo, Luckwearer")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = key,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(duplicate))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // +1 duplicate put in hand, -1 discarded as the cost, +2 drawn on resolution.
        driver.getHand(player).size shouldBe handSizeBefore + 2
        driver.getGraveyardCardNames(player).contains("Bilbo, Luckwearer") shouldBe true
    }

    test("a legendary card with a different name can't pay the discard cost") {
        val driver = createDriver()
        val player = startAtMain(driver)

        val key = driver.putPermanentOnBattlefield(player, "Key to the Side-Door")
        driver.giveMana(player, Color.WHITE, 1)
        driver.putCreatureOnBattlefield(player, "Bilbo, Luckwearer")

        val other = driver.putCardInHand(player, "Bilbo Baggins, Burglar")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = key,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(other))
            )
        ).isSuccess shouldBe false
    }

    test("a name match on a permanent an OPPONENT controls doesn't pay the cost") {
        val driver = createDriver()
        val player = startAtMain(driver)
        val opponent = driver.getOpponent(player)

        val key = driver.putPermanentOnBattlefield(player, "Key to the Side-Door")
        driver.giveMana(player, Color.WHITE, 1)
        driver.putCreatureOnBattlefield(opponent, "Bilbo, Luckwearer")

        val duplicate = driver.putCardInHand(player, "Bilbo, Luckwearer")

        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = key,
                abilityId = drawAbilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(duplicate))
            )
        ).isSuccess shouldBe false
    }
})
