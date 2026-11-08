package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.FumulusTheInfestation
import com.wingedsheep.mtg.sets.definitions.mrd.cards.BarterInBlood
import com.wingedsheep.mtg.sets.definitions.tmp.cards.DiabolicEdict
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fumulus, the Infestation (Foundations Jumpstart #42).
 *
 * Whenever a player sacrifices a nontoken creature, create a 1/1 black Insect creature token with
 * flying.
 *
 * Covers an opponent's sacrifice, a sacrificed token not counting, and Fumulus sacrificed in the
 * same each-player edict as an opponent's creature — the sacrifice look-back (CR 603.10a) sees both,
 * not only its own controller's half of the batch.
 */
class FumulusTheInfestationScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FumulusTheInfestation, DiabolicEdict, BarterInBlood))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun cast(driver: GameTestDriver, player: EntityId, cardName: String, mana: Int, targets: List<EntityId> = emptyList()) {
        val spell = driver.putCardInHand(player, cardName)
        driver.giveMana(player, Color.BLACK, mana)
        driver.castSpell(player, spell, targets).outcome shouldBe Outcome.Done
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()
    }

    fun tokens(driver: GameTestDriver, player: EntityId): Int =
        driver.getPermanents(player).count { driver.state.getEntity(it)?.has<TokenComponent>() == true }

    test("an opponent sacrificing a nontoken creature makes an Insect") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Fumulus, the Infestation")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        cast(driver, me, "Diabolic Edict", 2, listOf(opponent))

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        tokens(driver, me) shouldBe 1
    }

    test("a sacrificed token makes no Insect") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Fumulus, the Infestation")
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.replaceState(driver.state.updateEntity(bears) { it.with(TokenComponent) })

        cast(driver, me, "Diabolic Edict", 2, listOf(opponent))

        driver.getCreatures(opponent).isEmpty() shouldBe true
        tokens(driver, me) shouldBe 0
    }

    test("Fumulus sacrificed alongside an opponent's creature sees both") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Fumulus, the Infestation")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Each player sacrifices every creature they control, all at once.
        cast(driver, me, "Barter in Blood", 4)

        driver.findPermanent(me, "Fumulus, the Infestation") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        tokens(driver, me) shouldBe 2
    }
})
