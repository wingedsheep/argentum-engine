package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Tests for Carbonize (SCG #83).
 *
 * Carbonize: {2}{R}
 * Instant
 * Carbonize deals 3 damage to any target. If it's a creature, it can't be
 * regenerated this turn, and if it would die this turn, exile it instead.
 */
class CarbonizeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Carbonize deals 3 damage to a creature that survives") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 5/5 creature on opponent's battlefield (survives 3 damage)
        val creature = driver.putCreatureOnBattlefield(opponent, "Force of Nature")

        // Cast Carbonize targeting the creature
        driver.giveMana(activePlayer, Color.RED, 3)
        val carbonize = driver.putCardInHand(activePlayer, "Carbonize")
        driver.castSpellWithTargets(activePlayer, carbonize, listOf(ChosenTarget.Permanent(creature)))
        driver.bothPass()

        // Creature should have 3 damage but still be alive
        val damage = driver.state.getEntity(creature)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 3

        // Creature should still be on the battlefield
        driver.findPermanent(opponent, "Force of Nature") shouldBe creature
    }

    test("Carbonize kills creature and exiles it instead of graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a 2/2 creature on opponent's battlefield (dies to 3 damage)
        val creature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cast Carbonize targeting the creature
        driver.giveMana(activePlayer, Color.RED, 3)
        val carbonize = driver.putCardInHand(activePlayer, "Carbonize")
        driver.castSpellWithTargets(activePlayer, carbonize, listOf(ChosenTarget.Permanent(creature)))
        driver.bothPass()

        // Creature should be in exile, not graveyard
        val exileZone = ZoneKey(opponent, Zone.EXILE)
        val graveyardZone = ZoneKey(opponent, Zone.GRAVEYARD)
        driver.state.getZone(exileZone) shouldContain creature
        driver.state.getZone(graveyardZone) shouldNotContain creature
    }

    test("Carbonize deals 3 damage to a player (no exile effect)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Carbonize targeting the opponent
        driver.giveMana(activePlayer, Color.RED, 3)
        val carbonize = driver.putCardInHand(activePlayer, "Carbonize")
        driver.castSpellWithTargets(activePlayer, carbonize, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        // Opponent should have lost 3 life
        val life = driver.state.getEntity(opponent)?.get<LifeTotalComponent>()?.life ?: 0
        life shouldBe 17
    }
})
