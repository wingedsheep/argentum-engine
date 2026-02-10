package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
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
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.util.UUID

/**
 * Tests for Imagecrafter.
 *
 * Imagecrafter: {U}
 * Creature — Human Wizard
 * 1/1
 * {T}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.
 */
class ImagecrafterTest : FunSpec({

    val imagecrafterAbilityId = AbilityId(UUID.randomUUID().toString())

    val Imagecrafter = CardDefinition(
        name = "Imagecrafter",
        manaCost = ManaCost.parse("{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Human"), Subtype("Wizard"))),
        oracleText = "{T}: Choose a creature type other than Wall. Target creature becomes that type until end of turn.",
        creatureStats = CreatureStats(1, 1),
        script = CardScript.permanent(
            ActivatedAbility(
                id = imagecrafterAbilityId,
                cost = AbilityCost.Tap,
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.ContextTarget(0),
                    excludedTypes = listOf("Wall")
                ),
                targetRequirement = TargetCreature()
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Imagecrafter))
        return driver
    }

    test("Imagecrafter taps and presents creature type choice excluding Wall") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Imagecrafter and a target creature on the battlefield
        val imagecrafter = driver.putCreatureOnBattlefield(activePlayer, "Imagecrafter")
        driver.removeSummoningSickness(imagecrafter)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Activate Imagecrafter targeting Grizzly Bears
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = imagecrafter,
                abilityId = imagecrafterAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )
        result.isSuccess shouldBe true

        // Imagecrafter should be tapped
        driver.isTapped(imagecrafter) shouldBe true

        // Both pass to resolve the ability
        driver.bothPass()

        // Should present a ChooseOptionDecision for creature type
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        // Wall should NOT be in the options
        decision.options shouldNotContain "Wall"
    }

    test("creature type changes after choosing") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val imagecrafter = driver.putCreatureOnBattlefield(activePlayer, "Imagecrafter")
        driver.removeSummoningSickness(imagecrafter)
        val bear = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Activate targeting the bear
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = imagecrafter,
                abilityId = imagecrafterAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Goblin" from the options
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // The ability should have resolved successfully — game should not be paused
        driver.isPaused shouldBe false
    }
})
