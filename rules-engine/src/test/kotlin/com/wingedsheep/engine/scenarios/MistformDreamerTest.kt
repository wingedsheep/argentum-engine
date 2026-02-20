package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Mistform Dreamer.
 *
 * Mistform Dreamer: {2}{U}
 * Creature — Illusion
 * 2/1
 * Flying
 * {1}: Mistform Dreamer becomes the creature type of your choice until end of turn.
 */
class MistformDreamerTest : FunSpec({

    val mistformDreamerAbilityId = AbilityId(UUID.randomUUID().toString())

    val MistformDreamer = CardDefinition(
        name = "Mistform Dreamer",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Illusion"))),
        oracleText = "Flying\n{1}: Mistform Dreamer becomes the creature type of your choice until end of turn.",
        creatureStats = CreatureStats(2, 1),
        keywords = setOf(Keyword.FLYING),
        script = CardScript.permanent(
            ActivatedAbility(
                id = mistformDreamerAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{1}")),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.Self
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistformDreamer))
        return driver
    }

    test("Mistform Dreamer has flying") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dreamer = driver.putCreatureOnBattlefield(activePlayer, "Mistform Dreamer")
        val projected = projector.project(driver.state)

        projected.hasKeyword(dreamer, Keyword.FLYING) shouldBe true
    }

    test("activated ability presents creature type choice including Wall") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dreamer = driver.putCreatureOnBattlefield(activePlayer, "Mistform Dreamer")
        driver.removeSummoningSickness(dreamer)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability (costs {1} mana, no target)
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = dreamer,
                abilityId = mistformDreamerAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Both pass to resolve the ability
        driver.bothPass()

        // Should present a ChooseOptionDecision for creature type
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        // Unlike Imagecrafter, Wall should be in the options (no excluded types)
        decision.options shouldContain "Wall"
    }

    test("creature type changes to chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dreamer = driver.putCreatureOnBattlefield(activePlayer, "Mistform Dreamer")
        driver.removeSummoningSickness(dreamer)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = dreamer,
                abilityId = mistformDreamerAbilityId
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" from the options
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // The ability should have resolved successfully
        driver.isPaused shouldBe false
    }
})
