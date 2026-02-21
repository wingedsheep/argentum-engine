package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifyStatsForCreatureGroup
import com.wingedsheep.sdk.scripting.targets.TargetCreature
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
                    target = EffectTarget.BoundVariable("target"),
                    excludedTypes = listOf("Wall")
                ),
                targetRequirement = TargetCreature(id = "target")
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

    test("Imagecrafter changing Soldier to Beast removes Aven Brigadier lord bonus") {
        // Aven Brigadier: "Other Soldier creatures get +1/+1"
        // When Imagecrafter changes a Soldier to a Beast, the lord bonus should stop applying.
        val brigadierSoldierLordAbilityId = AbilityId(UUID.randomUUID().toString())
        val AvenBrigadier = CardDefinition(
            name = "Aven Brigadier",
            manaCost = ManaCost.parse("{3}{W}{W}{W}"),
            typeLine = TypeLine.creature(setOf(Subtype("Bird"), Subtype("Soldier"))),
            oracleText = "Other Soldier creatures get +1/+1.",
            creatureStats = CreatureStats(3, 5),
            keywords = setOf(Keyword.FLYING),
            script = CardScript.permanent(
                staticAbilities = listOf(
                    ModifyStatsForCreatureGroup(
                        powerBonus = 1,
                        toughnessBonus = 1,
                        filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Soldier"), excludeSelf = true)
                    )
                )
            )
        )

        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Imagecrafter, AvenBrigadier))
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Plains" to 20),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val projector = StateProjector()

        // Put Aven Brigadier and a Soldier creature on the battlefield
        val brigadier = driver.putCreatureOnBattlefield(activePlayer, "Aven Brigadier")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Blade of the Ninth Watch") // 2/1 Human Soldier

        // Soldier should get +1/+1 from Brigadier's lord effect
        projector.getProjectedPower(driver.state, soldier) shouldBe 3  // 2 + 1
        projector.getProjectedToughness(driver.state, soldier) shouldBe 2  // 1 + 1

        // Activate Imagecrafter to change the Soldier to a Beast
        val imagecrafter = driver.putCreatureOnBattlefield(activePlayer, "Imagecrafter")
        driver.removeSummoningSickness(imagecrafter)

        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = imagecrafter,
                abilityId = imagecrafterAbilityId,
                targets = listOf(ChosenTarget.Permanent(soldier))
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Beast" from the options
        val decision = driver.pendingDecision as ChooseOptionDecision
        val beastIndex = decision.options.indexOf("Beast")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, beastIndex))

        // Soldier is now a Beast — Aven Brigadier's lord bonus should NOT apply
        projector.getProjectedPower(driver.state, soldier) shouldBe 2  // base 2, no lord bonus
        projector.getProjectedToughness(driver.state, soldier) shouldBe 1  // base 1, no lord bonus
    }
})
