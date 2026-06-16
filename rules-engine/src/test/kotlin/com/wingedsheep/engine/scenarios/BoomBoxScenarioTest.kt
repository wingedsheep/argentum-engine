package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BoomBox
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Boom Box — {2} Artifact
 *
 * "{6}, {T}, Sacrifice this artifact: Destroy up to one target artifact, up to one
 * target creature, and up to one target land."
 *
 * Verifies: activating with all three targets destroys an artifact, a creature, and a
 * land (and sacrifices Boom Box itself); and that with no targets chosen the ability
 * still resolves, sacrificing Boom Box and destroying nothing.
 */
class BoomBoxScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    val abilityId = BoomBox.activatedAbilities[0].id

    test("destroys up to one artifact, creature, and land, and sacrifices itself") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val boomBox = driver.putPermanentOnBattlefield(me, "Boom Box")
        val artifact = driver.putPermanentOnBattlefield(opp, "Bandit's Haul")
        val creature = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val land = driver.putLandOnBattlefield(opp, "Mountain")

        driver.giveColorlessMana(me, 6)
        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = boomBox,
                abilityId = abilityId,
                targets = listOf(
                    ChosenTarget.Permanent(artifact),
                    ChosenTarget.Permanent(creature),
                    ChosenTarget.Permanent(land),
                ),
            )
        )
        result.isSuccess shouldBe true

        // Boom Box is sacrificed as a cost immediately.
        driver.assertInGraveyard(me, "Boom Box")

        driver.bothPass() // resolve the ability

        driver.assertInGraveyard(opp, "Bandit's Haul")
        driver.assertInGraveyard(opp, "Centaur Courser")
        driver.assertInGraveyard(opp, "Mountain")
    }

    test("can be activated with no targets — still sacrifices Boom Box, destroys nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val boomBox = driver.putPermanentOnBattlefield(me, "Boom Box")
        val creature = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.giveColorlessMana(me, 6)
        val result = driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = boomBox,
                abilityId = abilityId,
                targets = emptyList(),
            )
        )
        result.isSuccess shouldBe true
        driver.assertInGraveyard(me, "Boom Box")

        driver.bothPass() // resolve the ability

        // The untargeted creature survives.
        driver.getGraveyardCardNames(opp).contains("Centaur Courser") shouldBe false
        driver.getPermanents(opp).contains(creature) shouldBe true
    }
})
