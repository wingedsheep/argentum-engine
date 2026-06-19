package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.sos.cards.PotionersTrove
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Potioner's Trove {3} — Artifact
 *   {T}: Add one mana of any color.
 *   {T}: You gain 2 life. Activate only if you've cast an instant or sorcery spell this turn.
 *
 * Exercises the `ActivationRestriction.OnlyIfCondition(YouCastSpellsThisTurn(InstantOrSorcery))`
 * gate on the second ability:
 *  - before any instant/sorcery is cast, the lifegain ability can't be activated,
 *  - after casting Lightning Bolt (an instant), the lifegain ability resolves and gains 2 life.
 */
class PotionersTroveTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PotionersTrove))
        return driver
    }

    test("lifegain ability is gated until an instant or sorcery is cast this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val lifeBefore = driver.getLifeTotal(you)

        val trove = driver.putPermanentOnBattlefield(you, "Potioner's Trove")
        driver.removeSummoningSickness(trove)

        val lifegainAbilityId =
            driver.cardRegistry.requireCard("Potioner's Trove").activatedAbilities[1].id

        // No instant/sorcery cast yet → the lifegain ability is illegal.
        val blocked = driver.submit(
            ActivateAbility(playerId = you, sourceId = trove, abilityId = lifegainAbilityId),
        )
        (blocked.error != null) shouldBe true
        driver.getLifeTotal(you) shouldBe lifeBefore

        // Cast an instant (Lightning Bolt) at the opponent to satisfy the condition.
        driver.giveMana(you, Color.RED, 1)
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, bolt, listOf(ChosenTarget.Player(opponent))).error shouldBe null

        // Resolve the bolt off the stack.
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass(); safety++
        }

        // Now the lifegain ability is activatable.
        driver.submit(
            ActivateAbility(playerId = you, sourceId = trove, abilityId = lifegainAbilityId),
        ).error shouldBe null

        safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass(); safety++
        }

        driver.getLifeTotal(you) shouldBe (lifeBefore + 2)
    }
})
