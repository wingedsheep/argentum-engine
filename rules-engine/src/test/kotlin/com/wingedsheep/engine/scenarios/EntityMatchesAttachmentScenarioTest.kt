package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pins the attachment arm of `EntityMatches` (`EffectTarget.EnchantedPermanent` /
 * `EffectTarget.EquippedCreature` → the source's `AttachedToComponent`), which no registered-set
 * scenario covers (Essence Leak, the only `EnchantedPermanentMatches` card, has no test).
 *
 * Three paths through `ConditionEvaluator.evaluateAttachmentFilterMatch`:
 * - the static-ability **projection gate** (`ConditionalStaticAbility` keyword grant on an Aura),
 * - the **trigger-resolver** conditional grant (Essence Leak's shape: "as long as enchanted
 *   permanent is red or green, it has '<triggered ability>'"),
 * - the **`EquippedCreature` role** via raw `Conditions.EntityMatches` — the role no facade names.
 */
class EntityMatchesAttachmentScenarioTest : FunSpec({

    val CrimsonBear = CardDefinition.creature(
        name = "Crimson Bear",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    val AlabasterSoldier = CardDefinition.creature(
        name = "Alabaster Soldier",
        manaCost = ManaCost.parse("{1}{W}"),
        subtypes = setOf(Subtype("Soldier")),
        power = 2,
        toughness = 2
    )

    // Essence Leak's shape, with an observable payoff: as long as enchanted permanent is red or
    // green, it has flying and "At the beginning of your upkeep, draw a card."
    val TestLeak = card("Test Leak") {
        manaCost = "{U}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant permanent\nAs long as enchanted permanent is red or green, it has " +
            "flying and \"At the beginning of your upkeep, draw a card.\""

        auraTarget = Targets.Permanent

        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature()),
                condition = Conditions.EnchantedPermanentMatches(
                    GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.GREEN)
                )
            )
        }
        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantTriggeredAbility(
                    ability = TriggeredAbility.create(
                        trigger = Triggers.YourUpkeep.event,
                        binding = Triggers.YourUpkeep.binding,
                        effect = Effects.DrawCards(1)
                    ),
                    filter = GroupFilter.attachedCreature()
                ),
                condition = Conditions.EnchantedPermanentMatches(
                    GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.GREEN)
                )
            )
        }
    }

    // The role no facade names: equipped creature has flying as long as it's a Bear.
    val TestGauntlet = card("Test Gauntlet") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equipped creature has flying as long as it's a Bear."

        staticAbility {
            ability = ConditionalStaticAbility(
                ability = GrantKeyword(Keyword.FLYING, GroupFilter.attachedCreature()),
                condition = Conditions.EntityMatches(
                    EffectTarget.EquippedCreature,
                    GameObjectFilter.Creature.withSubtype(Subtype("Bear"))
                )
            )
        }
    }

    fun GameTestDriver.putAttached(
        playerId: EntityId,
        cardName: String,
        targetId: EntityId
    ): EntityId {
        val attachmentId = putPermanentOnBattlefield(playerId, cardName)
        var newState = state.updateEntity(attachmentId) { c -> c.with(AttachedToComponent(targetId)) }
        val existing = newState.getEntity(targetId)
            ?.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
        newState = newState.updateEntity(targetId) { c ->
            c.with(AttachmentsComponent(existing + attachmentId))
        }
        replaceState(newState)
        return attachmentId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CrimsonBear, AlabasterSoldier, TestLeak, TestGauntlet))
        return driver
    }

    fun advanceToControllerUpkeep(driver: GameTestDriver, controller: EntityId) {
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        if (driver.activePlayer != controller) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
            driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        }
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe controller
    }

    val projector = StateProjector()

    test("projection gate: enchanted red permanent matches, keyword grant is on") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Crimson Bear")
        driver.putAttached(player, "Test Leak", bear)

        projector.project(driver.state).hasKeyword(bear, Keyword.FLYING) shouldBe true
    }

    test("projection gate: enchanted white permanent doesn't match, keyword grant is off") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(player, "Alabaster Soldier")
        driver.putAttached(player, "Test Leak", soldier)

        projector.project(driver.state).hasKeyword(soldier, Keyword.FLYING) shouldBe false
    }

    test("trigger resolver: granted upkeep trigger fires while enchanted permanent matches") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Crimson Bear")
        driver.putAttached(player, "Test Leak", bear)
        val handBefore = driver.getHandSize(player)

        advanceToControllerUpkeep(driver, player)

        driver.stackSize shouldBe 1
        driver.bothPass()
        // +1 from the granted trigger; the turn's draw step hasn't happened yet.
        driver.getHandSize(player) shouldBe handBefore + 1
    }

    test("trigger resolver: no granted trigger while enchanted permanent doesn't match") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(player, "Alabaster Soldier")
        driver.putAttached(player, "Test Leak", soldier)

        advanceToControllerUpkeep(driver, player)

        driver.stackSize shouldBe 0
    }

    test("EquippedCreature role: equipped Bear matches, keyword grant is on") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = driver.putCreatureOnBattlefield(player, "Crimson Bear")
        driver.putAttached(player, "Test Gauntlet", bear)

        projector.project(driver.state).hasKeyword(bear, Keyword.FLYING) shouldBe true
    }

    test("EquippedCreature role: equipped non-Bear doesn't match, keyword grant is off") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(player, "Alabaster Soldier")
        driver.putAttached(player, "Test Gauntlet", soldier)

        projector.project(driver.state).hasKeyword(soldier, Keyword.FLYING) shouldBe false
    }
})
