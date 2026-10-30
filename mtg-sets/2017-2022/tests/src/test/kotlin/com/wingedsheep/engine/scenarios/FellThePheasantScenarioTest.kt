package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.FellThePheasant
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Fell the Pheasant (ELD #153) — {1}{G} Instant.
 *
 * "Fell the Pheasant deals 5 damage to target creature with flying. Create a Food token."
 */
class FellThePheasantScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FellThePheasant) + PredefinedTokens.allTokens)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("deals 5 damage to a flyer and creates a Food token for the caster") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val birds = driver.putCreatureOnBattlefield(opponent, "Birds of Paradise")

        val spell = driver.putCardInHand(me, "Fell the Pheasant")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, spell, listOf(birds)).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Birds of Paradise") shouldBe null
        driver.getGraveyardCardNames(opponent).contains("Birds of Paradise") shouldBe true
        driver.findPermanent(me, "Food").shouldNotBeNull()
    }

    test("a creature without flying isn't a legal target") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Fell the Pheasant")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, spell, listOf(bears)).error shouldNotBe null
    }
})
