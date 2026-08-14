package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LifesparkSpellbomb
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Lifespark Spellbomb (MRD #197).
 *
 * "{G}, Sacrifice this artifact: Until end of turn, target land becomes a 3/3 creature that's
 *  still a land.
 *  {1}, Sacrifice this artifact: Draw a card."
 *
 * The load-bearing claim is the "*that's still a land*" half: the animate must add the Creature
 * type and set base P/T without replacing the Land type. It also proves the effect survives its
 * source being sacrificed as part of the activation cost, and that it wears off at end of turn.
 */
class LifesparkSpellbombScenarioTest : FunSpec({

    val animateAbilityId = LifesparkSpellbomb.activatedAbilities[0].id // {G}, Sacrifice: animate a land
    val drawAbilityId = LifesparkSpellbomb.activatedAbilities[1].id // {1}, Sacrifice: draw a card

    val stateProjector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver
    }

    test("animates a land into a 3/3 that is still a land, and sacrifices the Spellbomb") {
        val driver = createDriver()
        val player = driver.player1
        val spellbomb = driver.putPermanentOnBattlefield(player, "Lifespark Spellbomb")
        val forest = driver.putLandOnBattlefield(player, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = spellbomb,
                abilityId = animateAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, forest))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        val projected = stateProjector.project(driver.state)
        withClue("The land is now a 3/3 creature") {
            projected.isCreature(forest) shouldBe true
            projected.getPower(forest) shouldBe 3
            projected.getToughness(forest) shouldBe 3
        }
        withClue("\"It's still a land\" — the Land type is added to, not replaced") {
            projected.hasType(forest, "LAND") shouldBe true
        }
        withClue("The Spellbomb was sacrificed as part of the activation cost") {
            driver.findPermanent(player, "Lifespark Spellbomb") shouldBe null
        }
    }

    test("the animation wears off at end of turn") {
        val driver = createDriver()
        val player = driver.player1
        val spellbomb = driver.putPermanentOnBattlefield(player, "Lifespark Spellbomb")
        val forest = driver.putLandOnBattlefield(player, "Forest")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = spellbomb,
                abilityId = animateAbilityId,
                targets = listOf(entityIdToChosenTarget(driver.state, forest))
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        stateProjector.project(driver.state).isCreature(forest) shouldBe true

        // Run out the turn — Duration.EndOfTurn should drop the animate at cleanup.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()

        withClue("Until end of turn — the Forest is a plain land again next turn") {
            stateProjector.project(driver.state).isCreature(forest) shouldBe false
        }
    }

    test("the second ability draws a card and sacrifices the Spellbomb") {
        val driver = createDriver()
        val player = driver.player1
        val spellbomb = driver.putPermanentOnBattlefield(player, "Lifespark Spellbomb")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(playerId = player, sourceId = spellbomb, abilityId = drawAbilityId)
        ).isSuccess shouldBe true
        driver.bothPass()

        withClue("One card drawn") {
            driver.state.getZone(ZoneKey(player, Zone.HAND)).size shouldBe handBefore + 1
        }
        withClue("The Spellbomb was sacrificed as part of the activation cost") {
            driver.findPermanent(player, "Lifespark Spellbomb") shouldBe null
        }
    }
})
