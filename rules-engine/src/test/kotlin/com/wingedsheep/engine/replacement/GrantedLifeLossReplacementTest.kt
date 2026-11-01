package com.wingedsheep.engine.replacement

import com.wingedsheep.engine.event.GrantedReplacementEffect
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.LifeLossFloor
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Durational grants of the damage → life-loss replacements (`Effects.GrantReplacementEffect` of a
 * [LifeLossFloor] or [ModifyLifeLoss]) apply like printed ones: Angel of Grace's "until end of
 * turn, damage that would reduce your life total to less than 1 reduces it to 1 instead". The
 * grant's "you" is the player recorded on it, and it needs no permanent on the battlefield.
 */
class GrantedLifeLossReplacementTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Mountain" to 20, "Swamp" to 20), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.grant(controller: EntityId, replacement: ReplacementEffect) {
        replaceState(
            state.copy(
                grantedReplacementEffects = state.grantedReplacementEffects + GrantedReplacementEffect(
                    entityId = controller,
                    controllerId = controller,
                    replacement = replacement,
                    duration = Duration.EndOfTurn,
                )
            )
        )
    }

    fun GameTestDriver.bolt(caster: EntityId, target: EntityId) {
        giveMana(caster, Color.RED, 1)
        val bolt = putCardInHand(caster, "Lightning Bolt")
        castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Player(target))).error shouldBe null
        bothPass()
    }

    val floor = LifeLossFloor(floor = 1, appliesTo = EventPattern.LifeLossEvent(player = Player.You))

    test("a granted floor holds lethal damage at 1 with no source on the battlefield") {
        val driver = driver()
        val you = driver.activePlayer!!
        driver.grant(you, floor)
        driver.setLifeTotal(you, 3)

        driver.bolt(you, you)

        driver.assertLifeTotal(you, 1)
    }

    test("a granted floor guards only the player it was granted to") {
        val driver = driver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.grant(you, floor)
        driver.setLifeTotal(opponent, 3)

        driver.bolt(you, opponent)

        driver.assertLifeTotal(opponent, 0)
    }

    test("a granted life-loss modifier changes damage-caused loss") {
        val driver = driver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.grant(you, ModifyLifeLoss(multiplier = 2, appliesTo = EventPattern.LifeLossEvent(player = Player.EachOpponent)))

        driver.bolt(you, opponent)

        driver.assertLifeTotal(opponent, 14)
    }
})
