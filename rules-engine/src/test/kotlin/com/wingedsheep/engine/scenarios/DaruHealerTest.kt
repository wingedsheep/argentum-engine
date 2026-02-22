package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.DaruHealer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Daru Healer.
 *
 * Daru Healer: {2}{W}
 * Creature — Human Cleric
 * 1/2
 * {T}: Prevent the next 1 damage that would be dealt to any target this turn.
 * Morph {W}
 */
class DaruHealerTest : FunSpec({

    val healerAbilityId = DaruHealer.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("prevents 1 damage to target creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        val target = driver.putCreatureOnBattlefield(activePlayer, "Hill Giant")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the Hill Giant
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Deal 3 damage via Lightning Bolt - shield prevents 1, 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(target)))
        driver.bothPass()

        val damage = driver.state.getEntity(target)?.get<DamageComponent>()?.amount ?: 0
        damage shouldBe 2
    }

    test("prevents 1 damage to target player") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the opponent
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Deal 3 damage to opponent via Lightning Bolt - shield prevents 1, so 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("shield is consumed after one use") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Mountain" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val healer = driver.putCreatureOnBattlefield(activePlayer, "Daru Healer")
        driver.removeSummoningSickness(healer)

        // Activate healer's ability targeting the opponent
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = healer,
                abilityId = healerAbilityId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // First bolt: 3 damage, shield prevents 1, 2 gets through
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt1 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt1, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18

        // Second bolt: full 3 damage, no shield remaining
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt2 = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt2, listOf(ChosenTarget.Player(opponent)))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 15
    }
})
