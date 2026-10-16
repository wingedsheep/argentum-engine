package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.JamesWanderingDad
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * James, Wandering Dad // Follow Him (PIP) — the Adventure face is an X instant whose Investigate
 * count is the spell's X: the first X-scaled Investigate, read off an Adventure face's X.
 */
class JamesWanderingDadScenarioTest : FunSpec({

    fun newDriver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(PredefinedTokens.allTokens)
        driver.registerCard(JamesWanderingDad)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun GameTestDriver.clueCount(player: EntityId): Int =
        getPermanents(player).count { getCardName(it) == "Clue" }

    fun castFollowHim(driver: GameTestDriver, player: EntityId, x: Int): EntityId {
        val james = driver.putCardInHand(player, "James, Wandering Dad")
        driver.giveMana(player, Color.BLUE, x + 2)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = james,
                xValue = x,
                faceIndex = 0,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()
        return james
    }

    test("Follow Him with X = 3 investigates three times and goes on an adventure") {
        val (driver, you) = newDriver()
        val james = castFollowHim(driver, you, 3)

        withClue("three Clue tokens") { driver.clueCount(you) shouldBe 3 }
        withClue("the card is exiled on its adventure, castable later as the creature") {
            driver.getExile(you) shouldContain james
            driver.state.mayPlayPermissions.any { james in it.cardIds && it.controllerId == you } shouldBe true
        }
    }

    test("Follow Him with X = 0 investigates zero times") {
        val (driver, you) = newDriver()
        val james = castFollowHim(driver, you, 0)

        driver.clueCount(you) shouldBe 0
        driver.getExile(you) shouldContain james
    }
})
