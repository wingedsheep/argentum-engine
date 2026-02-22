package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.onslaught.cards.ElvishPathcutter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Elvish Pathcutter.
 *
 * Elvish Pathcutter: {3}{G}
 * Creature — Elf Scout
 * 1/2
 * {2}{G}: Target Elf creature gains forestwalk until end of turn.
 */
class ElvishPathcutterTest : FunSpec({

    val pathcutterAbilityId = ElvishPathcutter.activatedAbilities.first().id

    val ElvishWarrior = CardDefinition.creature(
        name = "Elvish Warrior",
        manaCost = ManaCost.parse("{G}{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Warrior")),
        power = 2,
        toughness = 3,
        oracleText = ""
    )

    val GlorySeeker = CardDefinition.creature(
        name = "Glory Seeker",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ElvishWarrior, GlorySeeker))
        return driver
    }

    test("Activating ability grants forestwalk to target Elf") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pathcutter = driver.putCreatureOnBattlefield(activePlayer, "Elvish Pathcutter")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(pathcutter)
        driver.removeSummoningSickness(elf)

        driver.giveMana(activePlayer, Color.GREEN, 3)

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pathcutter,
                abilityId = pathcutterAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        activateResult.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasKeyword(elf, Keyword.FORESTWALK) shouldBe true
    }

    test("Pathcutter can target itself since it is an Elf") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pathcutter = driver.putCreatureOnBattlefield(activePlayer, "Elvish Pathcutter")
        driver.removeSummoningSickness(pathcutter)

        driver.giveMana(activePlayer, Color.GREEN, 3)

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pathcutter,
                abilityId = pathcutterAbilityId,
                targets = listOf(ChosenTarget.Permanent(pathcutter))
            )
        )
        activateResult.isSuccess shouldBe true

        driver.bothPass()

        val projected = projector.project(driver.state)
        projected.hasKeyword(pathcutter, Keyword.FORESTWALK) shouldBe true
    }

    test("Cannot target non-Elf creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pathcutter = driver.putCreatureOnBattlefield(activePlayer, "Elvish Pathcutter")
        val human = driver.putCreatureOnBattlefield(activePlayer, "Glory Seeker")
        driver.removeSummoningSickness(pathcutter)
        driver.removeSummoningSickness(human)

        driver.giveMana(activePlayer, Color.GREEN, 3)

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pathcutter,
                abilityId = pathcutterAbilityId,
                targets = listOf(ChosenTarget.Permanent(human))
            )
        )
        activateResult.isSuccess shouldBe false
    }

    test("Forestwalk wears off at end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pathcutter = driver.putCreatureOnBattlefield(activePlayer, "Elvish Pathcutter")
        val elf = driver.putCreatureOnBattlefield(activePlayer, "Elvish Warrior")
        driver.removeSummoningSickness(pathcutter)
        driver.removeSummoningSickness(elf)

        driver.giveMana(activePlayer, Color.GREEN, 3)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = pathcutter,
                abilityId = pathcutterAbilityId,
                targets = listOf(ChosenTarget.Permanent(elf))
            )
        )
        driver.bothPass()

        // Forestwalk should be active now
        val projected = projector.project(driver.state)
        projected.hasKeyword(elf, Keyword.FORESTWALK) shouldBe true

        // Advance past end of turn to opponent's upkeep - forestwalk should wear off
        driver.passPriorityUntil(Step.UPKEEP)

        val projectedNextTurn = projector.project(driver.state)
        projectedNextTurn.hasKeyword(elf, Keyword.FORESTWALK) shouldBe false
    }
})
