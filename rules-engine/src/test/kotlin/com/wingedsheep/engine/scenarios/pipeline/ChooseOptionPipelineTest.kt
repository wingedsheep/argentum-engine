package com.wingedsheep.engine.scenarios.pipeline

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ChooseOptionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.OptionType
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the generic ChooseOptionEffect pipeline step.
 */
class ChooseOptionPipelineTest : FunSpec({

    val ChooseCreatureTypeThenDraw = CardDefinition.sorcery(
        name = "Choose Creature Type Then Draw",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Choose a creature type. Draw a card.",
        script = CardScript.spell(
            effect = CompositeEffect(listOf(
                ChooseOptionEffect(
                    optionType = OptionType.CREATURE_TYPE,
                    storeAs = "chosenType"
                ),
                DrawCardsEffect(1, EffectTarget.Controller)
            ))
        )
    )

    val ChooseColorSpell = CardDefinition.sorcery(
        name = "Choose Color Spell",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Choose a color.",
        script = CardScript.spell(
            effect = ChooseOptionEffect(
                optionType = OptionType.COLOR,
                storeAs = "chosenColor"
            )
        )
    )

    val ChooseCreatureTypeExcluding = CardDefinition.sorcery(
        name = "Choose Non-Wall Type",
        manaCost = ManaCost.parse("{B}"),
        oracleText = "Choose a creature type other than Wall.",
        script = CardScript.spell(
            effect = ChooseOptionEffect(
                optionType = OptionType.CREATURE_TYPE,
                storeAs = "chosenType",
                excludedOptions = listOf("Wall")
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(
            ChooseCreatureTypeThenDraw, ChooseColorSpell, ChooseCreatureTypeExcluding
        ))
        return driver
    }

    test("ChooseOptionEffect(CREATURE_TYPE) pauses for decision with all creature types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cardId = driver.putCardInHand(activePlayer, "Choose Creature Type Then Draw")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.castSpell(activePlayer, cardId)
        driver.bothPass()

        val decision = driver.pendingDecision
        decision shouldNotBe null
        decision.shouldBeInstanceOf<ChooseOptionDecision>()

        val chooseDecision = decision as ChooseOptionDecision
        chooseDecision.playerId shouldBe activePlayer
        chooseDecision.options shouldBe Subtype.ALL_CREATURE_TYPES
    }

    test("ChooseOptionEffect(CREATURE_TYPE) then draw — pipeline continues after choice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handSizeBefore = driver.getHandSize(activePlayer)
        val cardId = driver.putCardInHand(activePlayer, "Choose Creature Type Then Draw")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.castSpell(activePlayer, cardId)
        driver.bothPass()

        // Choose "Elf"
        val decision = driver.pendingDecision as ChooseOptionDecision
        val elfIndex = decision.options.indexOf("Elf")
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, elfIndex))

        // Pipeline should have continued: put card in hand (+1), cast spell (-1), drew card (+1) = handSizeBefore + 1
        driver.pendingDecision shouldBe null
        driver.getHandSize(activePlayer) shouldBe handSizeBefore + 1
    }

    test("ChooseOptionEffect(COLOR) presents five Magic colors") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cardId = driver.putCardInHand(activePlayer, "Choose Color Spell")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.castSpell(activePlayer, cardId)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        decision.options shouldBe listOf("White", "Blue", "Black", "Red", "Green")

        // Choose "Blue"
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 1))
        driver.pendingDecision shouldBe null
    }

    test("ChooseOptionEffect with excludedOptions filters out excluded types") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20, "Plains" to 20))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val cardId = driver.putCardInHand(activePlayer, "Choose Non-Wall Type")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.castSpell(activePlayer, cardId)
        driver.bothPass()

        val decision = driver.pendingDecision as ChooseOptionDecision
        decision.options.contains("Wall") shouldBe false
        decision.options.size shouldBe Subtype.ALL_CREATURE_TYPES.size - 1

        // Can still choose another type
        driver.submitDecision(activePlayer, OptionChosenResponse(decision.id, 0))
        driver.pendingDecision shouldBe null
    }
})
