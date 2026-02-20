package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TurnManager
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
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.SourceHasSubtype
import com.wingedsheep.sdk.scripting.StaticTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.UUID

/**
 * Tests for Mistform Wall.
 *
 * Mistform Wall: {2}{U}
 * Creature — Illusion Wall
 * 1/4
 * This creature has defender as long as it's a Wall.
 * {1}: Mistform Wall becomes the creature type of your choice until end of turn.
 */
class MistformWallTest : FunSpec({

    val mistformWallAbilityId = AbilityId(UUID.randomUUID().toString())

    val MistformWall = CardDefinition(
        name = "Mistform Wall",
        manaCost = ManaCost.parse("{2}{U}"),
        typeLine = TypeLine.creature(setOf(Subtype("Illusion"), Subtype("Wall"))),
        oracleText = "This creature has defender as long as it's a Wall.\n{1}: Mistform Wall becomes the creature type of your choice until end of turn.",
        creatureStats = CreatureStats(1, 4),
        script = CardScript.permanent(
            ActivatedAbility(
                id = mistformWallAbilityId,
                cost = AbilityCost.Mana(ManaCost.parse("{1}")),
                effect = BecomeCreatureTypeEffect(
                    target = EffectTarget.Self
                )
            ),
            staticAbilities = listOf(
                ConditionalStaticAbility(
                    ability = GrantKeyword(Keyword.DEFENDER, StaticTarget.SourceCreature),
                    condition = SourceHasSubtype(Subtype("Wall"))
                )
            )
        )
    )

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MistformWall))
        return driver
    }

    test("Mistform Wall has defender by default (is a Wall)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        val projected = projector.project(driver.state)

        projected.hasKeyword(wall, Keyword.DEFENDER) shouldBe true
    }

    test("Mistform Wall is 1/4") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        val projected = projector.project(driver.state)

        projected.getPower(wall) shouldBe 1
        projected.getToughness(wall) shouldBe 4
    }

    test("loses defender after changing to non-Wall creature type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        driver.removeSummoningSickness(wall)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wall,
                abilityId = mistformWallAbilityId
            )
        )
        result.isSuccess shouldBe true

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" (a non-Wall type)
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // After becoming an Elf, it should no longer have defender
        val projected = projector.project(driver.state)
        projected.hasKeyword(wall, Keyword.DEFENDER) shouldBe false
    }

    test("keeps defender after changing to Wall creature type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        driver.removeSummoningSickness(wall)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wall,
                abilityId = mistformWallAbilityId
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Wall" explicitly
        val decision = driver.pendingDecision as ChooseOptionDecision
        val wallIndex = decision.options.indexOf("Wall")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, wallIndex))

        // After choosing Wall, it should still have defender
        val projected = projector.project(driver.state)
        projected.hasKeyword(wall, Keyword.DEFENDER) shouldBe true
    }

    test("subtypes change to chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        driver.removeSummoningSickness(wall)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wall,
                abilityId = mistformWallAbilityId
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Goblin"
        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Subtypes should now be just "Goblin" (replaces Illusion and Wall)
        val projected = projector.project(driver.state)
        projected.getSubtypes(wall) shouldBe setOf("Goblin")
    }

    val turnManager = TurnManager()

    test("not a valid attacker while still a Wall (has defender)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        driver.removeSummoningSickness(wall)

        // Mistform Wall should NOT be a valid attacker (it has defender as a Wall)
        val validAttackers = turnManager.getValidAttackers(driver.state, activePlayer)
        validAttackers shouldBe emptyList()
    }

    test("becomes a valid attacker after changing to non-Wall type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val wall = driver.putCreatureOnBattlefield(activePlayer, "Mistform Wall")
        driver.removeSummoningSickness(wall)

        // Give mana to pay the {1} cost
        driver.giveMana(activePlayer, Color.BLUE, 1)

        // Activate the ability
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = wall,
                abilityId = mistformWallAbilityId
            )
        )

        // Resolve the ability
        driver.bothPass()

        // Choose "Elf" (a non-Wall type)
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Mistform Wall should now be a valid attacker (no longer a Wall, no defender)
        val validAttackers = turnManager.getValidAttackers(driver.state, activePlayer)
        validAttackers shouldBe listOf(wall)
    }
})
