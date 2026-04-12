package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.ChronicleOfVictory
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.FeistySpikeling
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private val projector = StateProjector()

/**
 * Tests for GroupFilter.chosenSubtypeKey — dynamic group filtering based on
 * a creature type chosen at resolution time via ChooseOptionEffect pipeline.
 */
class ChosenSubtypeGroupFilterTest : FunSpec({

    // Test card: "Choose a creature type. Creatures of that type get +2/+2 until end of turn."
    // Pipeline: ChooseOption(CREATURE_TYPE) → ForEachInGroup(ChosenSubtypeCreatures, ModifyStats)
    val TribalBoost = CardDefinition.sorcery(
        name = "Tribal Boost",
        manaCost = ManaCost.parse("{2}{G}"),
        oracleText = "Choose a creature type. Creatures of that type get +2/+2 until end of turn.",
        script = CardScript.spell(
            effect = CompositeEffect(listOf(
                ChooseOptionEffect(
                    optionType = OptionType.CREATURE_TYPE,
                    storeAs = "chosenCreatureType"
                ),
                ForEachInGroupEffect(
                    filter = GroupFilter.ChosenSubtypeCreatures("chosenCreatureType"),
                    effect = ModifyStatsEffect(
                        powerModifier = DynamicAmount.Fixed(2),
                        toughnessModifier = DynamicAmount.Fixed(2),
                        target = EffectTarget.Self,
                        duration = Duration.EndOfTurn
                    )
                )
            ))
        )
    )

    // Inline test creatures
    val goblin = CardDefinition.creature(
        name = "Goblin Raider",
        manaCost = ManaCost.parse("{1}{R}"),
        subtypes = setOf(Subtype("Goblin")),
        power = 2,
        toughness = 2
    )

    val elf = CardDefinition.creature(
        name = "Llanowar Elves",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf"), Subtype("Druid")),
        power = 1,
        toughness = 1
    )

    val goblinElf = CardDefinition.creature(
        name = "Goblin Elf",
        manaCost = ManaCost.parse("{R}{G}"),
        subtypes = setOf(Subtype("Goblin"), Subtype("Elf")),
        power = 2,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TribalBoost, goblin, elf, goblinElf))
        return driver
    }

    test("ChosenSubtypeCreatures only affects creatures of the chosen type") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Goblin and an Elf on the battlefield
        val goblinId = driver.putCreatureOnBattlefield(activePlayer, "Goblin Raider")
        val elfId = driver.putCreatureOnBattlefield(activePlayer, "Llanowar Elves")

        // Cast Tribal Boost
        val spell = driver.putCardInHand(activePlayer, "Tribal Boost")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        // Should pause for creature type choice
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        // Choose "Goblin"
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Verify: Goblin got +2/+2, Elf did not
        val projected = projector.project(driver.state)
        projected.getPower(goblinId) shouldBe 4      // 2 + 2
        projected.getToughness(goblinId) shouldBe 4   // 2 + 2
        projected.getPower(elfId) shouldBe 1           // unchanged
        projected.getToughness(elfId) shouldBe 1       // unchanged
    }

    test("ChosenSubtypeCreatures affects creatures with multiple subtypes") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put a Goblin, an Elf, and a Goblin Elf on the battlefield
        val goblinId = driver.putCreatureOnBattlefield(activePlayer, "Goblin Raider")
        val elfId = driver.putCreatureOnBattlefield(activePlayer, "Llanowar Elves")
        val goblinElfId = driver.putCreatureOnBattlefield(activePlayer, "Goblin Elf")

        // Cast Tribal Boost and choose "Goblin"
        val spell = driver.putCardInHand(activePlayer, "Tribal Boost")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Verify: Goblin Raider and Goblin Elf got +2/+2, Elf did not
        val projected = projector.project(driver.state)
        projected.getPower(goblinId) shouldBe 4        // 2 + 2
        projected.getToughness(goblinId) shouldBe 4
        projected.getPower(goblinElfId) shouldBe 4     // 2 + 2 (has Goblin subtype)
        projected.getToughness(goblinElfId) shouldBe 3
        projected.getPower(elfId) shouldBe 1            // unchanged
        projected.getToughness(elfId) shouldBe 1
    }

    test("ChosenSubtypeCreatures affects both players' creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Goblins on both sides
        val myGoblin = driver.putCreatureOnBattlefield(activePlayer, "Goblin Raider")
        val theirGoblin = driver.putCreatureOnBattlefield(opponent, "Goblin Raider")
        val theirElf = driver.putCreatureOnBattlefield(opponent, "Llanowar Elves")

        // Cast and choose Goblin
        val spell = driver.putCardInHand(activePlayer, "Tribal Boost")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Both Goblins buffed, Elf not
        val projected = projector.project(driver.state)
        projected.getPower(myGoblin) shouldBe 4
        projected.getPower(theirGoblin) shouldBe 4
        projected.getPower(theirElf) shouldBe 1
    }

    test("ChosenSubtypeCreatures returns empty when no creatures match") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 20, "Mountain" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Only Elves on battlefield
        val elfId = driver.putCreatureOnBattlefield(activePlayer, "Llanowar Elves")

        // Cast and choose Goblin (no Goblins exist)
        val spell = driver.putCardInHand(activePlayer, "Tribal Boost")
        driver.giveMana(activePlayer, Color.GREEN, 3)
        driver.castSpell(activePlayer, spell)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        val goblinIndex = decision.options.indexOf("Goblin")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, goblinIndex))

        // Elf should be unchanged
        val projected = projector.project(driver.state)
        projected.getPower(elfId) shouldBe 1
        projected.getToughness(elfId) shouldBe 1
    }

    test("changeling creature gets +2/+2 from Chronicle of Victory's chosen type buff") {
        val driver = GameTestDriver()
        val changeling = CardDefinition.creature(
            name = "Test Changeling",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype("Shapeshifter")),
            power = 2,
            toughness = 1,
            keywords = setOf(Keyword.CHANGELING)
        )
        driver.registerCards(TestCards.all + listOf(ChronicleOfVictory, changeling))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Plains" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Chronicle of Victory on battlefield with chosen type "Elf"
        val chronicle = driver.putPermanentOnBattlefield(activePlayer, "Chronicle of Victory")
        driver.replaceState(driver.state.updateEntity(chronicle) { c ->
            c.with(ChosenCreatureTypeComponent("Elf"))
        })

        // Put changeling on battlefield
        val changelingId = driver.putCreatureOnBattlefield(activePlayer, "Test Changeling")

        // Changeling should get +2/+2 (it has all creature types including Elf)
        val projected = projector.project(driver.state)
        projected.getPower(changelingId) shouldBe 4    // 2 + 2
        projected.getToughness(changelingId) shouldBe 3 // 1 + 2
    }

    test("casting changeling spell triggers Chronicle of Victory's draw ability") {
        val driver = GameTestDriver()
        val changeling = CardDefinition.creature(
            name = "Test Changeling",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype("Shapeshifter")),
            power = 2,
            toughness = 1,
            keywords = setOf(Keyword.CHANGELING)
        )
        driver.registerCards(TestCards.all + listOf(ChronicleOfVictory, changeling))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Plains" to 20),
            skipMulligans = true
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Chronicle of Victory on battlefield with chosen type "Elf"
        val chronicle = driver.putPermanentOnBattlefield(activePlayer, "Chronicle of Victory")
        driver.replaceState(driver.state.updateEntity(chronicle) { c ->
            c.with(ChosenCreatureTypeComponent("Elf"))
        })

        val handSizeBefore = driver.getHandSize(activePlayer)

        // Cast changeling creature — should trigger "draw a card"
        val spell = driver.putCardInHand(activePlayer, "Test Changeling")
        driver.giveMana(activePlayer, Color.RED, 2)
        driver.castSpell(activePlayer, spell)
        driver.bothPass() // resolve the changeling spell

        // Should have drawn a card from Chronicle's trigger
        // Hand size: started at handSizeBefore, +1 from putting changeling in hand,
        // -1 from casting it, +1 from draw trigger = handSizeBefore + 1
        driver.getHandSize(activePlayer) shouldBe handSizeBefore + 1
    }
})
