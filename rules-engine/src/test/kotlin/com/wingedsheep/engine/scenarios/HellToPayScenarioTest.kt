package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HellToPay
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hell to Pay (OTJ) — {X}{R} Sorcery.
 *
 * "Hell to Pay deals X damage to target creature. Create a number of tapped Treasure tokens
 *  equal to the amount of excess damage dealt to that creature this way."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.values.EntityNumericProperty.ExcessMarkedDamage]
 * read: excess = max(0, marked − toughness), computed post-damage in the same composite resolution.
 */
class HellToPayScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + HellToPay + PredefinedTokens.Treasure)
        return driver
    }

    fun treasureCount(driver: GameTestDriver, playerId: com.wingedsheep.sdk.model.EntityId): Int =
        driver.state.getZone(ZoneKey(playerId, Zone.BATTLEFIELD)).count { entityId ->
            driver.state.getEntity(entityId)?.get<CardComponent>()?.name == "Treasure"
        }

    test("X=5 to a 2/2 creature → 3 excess → 3 tapped Treasures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Hell to Pay")
        driver.giveMana(player, Color.RED, 1) // {R}
        driver.giveColorlessMana(player, 5)    // X = 5
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                xValue = 5
            )
        )
        driver.bothPass()

        // 5 damage to toughness 2 → 3 excess → 3 Treasures.
        treasureCount(driver, player) shouldBe 3
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }

    test("X exactly lethal (X=2 to a 2/2) → no excess → no Treasures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = driver.putCreatureOnBattlefield(opponent, "Black Creature") // 2/2

        val spell = driver.putCardInHand(player, "Hell to Pay")
        driver.giveMana(player, Color.RED, 1)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(victim)),
                xValue = 2
            )
        )
        driver.bothPass()

        treasureCount(driver, player) shouldBe 0
        driver.findPermanent(opponent, "Black Creature") shouldBe null
    }
})
