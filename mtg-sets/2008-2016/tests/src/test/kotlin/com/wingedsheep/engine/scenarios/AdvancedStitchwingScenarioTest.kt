package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.AdvancedStitchwing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Advanced Stitchwing (Eldritch Moon #49) — {3}{U}{U} Creature — Zombie Horror, 3/4.
 *
 * "Flying"
 * "{2}{U}, Discard two cards: Return this card from your graveyard to the battlefield tapped."
 *
 * The ability works from the graveyard: paying {2}{U} and discarding two cards returns it tapped.
 */
class AdvancedStitchwingScenarioTest : FunSpec({

    val abilityId = AdvancedStitchwing.activatedAbilities.first().id

    test("{2}{U}, discard two cards: returns from the graveyard to the battlefield tapped") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.player1

        val stitchwing = driver.putCardInGraveyard(player, "Advanced Stitchwing")
        val discardA = driver.putCardInHand(player, "Grizzly Bears")
        val discardB = driver.putCardInHand(player, "Grizzly Bears")
        driver.giveMana(player, Color.BLUE, 3) // {2}{U}

        driver.submitSuccess(
            ActivateAbility(
                playerId = player,
                sourceId = stitchwing,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(discardedCards = listOf(discardA, discardB)),
            )
        )
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(stitchwing) shouldBe true
        driver.isTapped(stitchwing) shouldBe true
        driver.getGraveyard(player) shouldContainAll listOf(discardA, discardB)
    }
})
