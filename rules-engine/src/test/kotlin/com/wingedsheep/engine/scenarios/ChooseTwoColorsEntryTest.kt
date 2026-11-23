package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * "As this enters, choose two colors" — `EntersWithChoice(COLOR, colorCount = 2)` asks for exactly two
 * distinct colors, records them as a [ChoiceValue.ColorsChoice], and the readers see both:
 * `IterationSpace.ChosenColorsOf` walks them, and `SharesChosenColorWithSource` matches an object
 * that is at least one of them. Modelled on Tablet of the Guilds.
 */
class ChooseTwoColorsEntryTest : FunSpec({
    val tablet = card("Test Two-Color Tablet") {
        manaCost = "{2}"; typeLine = "Artifact"
        replacementEffect(EntersWithChoice(ChoiceType.COLOR, colorCount = 2))
        triggeredAbility {
            trigger = Triggers.you.casts()
            interveningIf = Conditions.TriggeringSpellMatches(GameObjectFilter.Any.sharingChosenColorWithSource())
            effect = Effects.ForEachChosenColor(
                effect = Effects.If(
                    Conditions.TriggeringSpellMatches(GameObjectFilter.Any.withChosenColor()),
                    Effects.GainLife(1)
                )
            )
        }
    }
    val white = card("Test White Bear") { manaCost = "{W}"; typeLine = "Creature — Bear"; power = 1; toughness = 1 }
    val azorius = card("Test Azorius Bear") { manaCost = "{W}{U}"; typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val red = card("Test Red Bear") { manaCost = "{R}"; typeLine = "Creature — Bear"; power = 1; toughness = 1 }
    val reanimate = card("Test Tablet Reanimate") {
        manaCost = "{B}"; typeLine = "Sorcery"
        spell { effect = Effects.PutOntoBattlefield(target(TargetFilter.CardInGraveyard)) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(tablet, white, azorius, red, reanimate))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.castTablet(): EntityId {
        val id = putCardInHand(player1, tablet.name)
        giveMana(player1, Color.WHITE, 2)
        castSpell(player1, id).error shouldBe null
        bothPass()
        return id
    }
    fun GameTestDriver.chooseColors(vararg colors: Color): ExecutionResult {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        return submitDecision(player1, ColorChosenResponse(decision.id, colors.first(), colors.toList()))
    }
    fun GameTestDriver.castBear(name: String, vararg pips: Color) {
        val id = putCardInHand(player1, name)
        pips.forEach { giveMana(player1, it, 1) }
        castSpell(player1, id).error shouldBe null
    }

    test("the entry asks for exactly two colors and records both, in WUBRG order") {
        val d = driver()
        val id = d.castTablet()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        decision.minColors shouldBe 2
        decision.maxColors shouldBe 2
        decision.prompt shouldBe "Choose two colors"

        d.chooseColors(Color.BLUE, Color.WHITE).error shouldBe null

        val entity = d.state.getEntity(id)!!
        entity.get<CastChoicesComponent>()!!.chosen[ChoiceSlot.COLOR] shouldBe
            ChoiceValue.ColorsChoice(listOf(Color.WHITE, Color.BLUE))
        entity.chosenColors() shouldBe listOf(Color.WHITE, Color.BLUE)
        // Two colors are not "the chosen color" of a one-color reader.
        entity.chosenColor() shouldBe null
    }

    test("one color, or three, is not an answer to choose two") {
        val d = driver()
        d.castTablet()
        val decision = d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        d.submitDecision(d.player1, ColorChosenResponse(decision.id, Color.WHITE)).error shouldNotBe null
        d.chooseColors(Color.WHITE, Color.BLUE, Color.BLACK).error shouldNotBe null
        d.chooseColors(Color.WHITE, Color.WHITE).error shouldNotBe null
        d.chooseColors(Color.WHITE, Color.BLUE).error shouldBe null
    }

    test("a spell of one chosen color gains 1, of both gains 2, of neither doesn't trigger") {
        val d = driver()
        d.castTablet()
        d.chooseColors(Color.WHITE, Color.BLUE).error shouldBe null
        val start = d.getLifeTotal(d.player1)

        d.castBear(white.name, Color.WHITE)
        d.state.stack.size shouldBe 2
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe start + 1
        d.bothPass()

        d.castBear(azorius.name, Color.WHITE, Color.BLUE)
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe start + 3
        d.bothPass()

        // CR 603.4 intervening "if": a red spell is none of the chosen colors, so nothing triggers.
        d.castBear(red.name, Color.RED)
        d.state.stack.size shouldBe 1
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe start + 3
    }

    test("put onto the battlefield without being cast, it still chooses two colors") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, tablet.name)
        val spell = d.putCardInHand(d.player1, reanimate.name)
        d.giveMana(d.player1, Color.BLACK, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()

        d.state.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>().minColors shouldBe 2
        d.chooseColors(Color.RED, Color.GREEN).error shouldBe null
        d.state.getEntity(id)!!.chosenColors() shouldBe listOf(Color.RED, Color.GREEN)
    }

    test("the two-color choice survives a state round-trip, and the SDK shapes round-trip") {
        val d = driver()
        val id = d.castTablet()
        d.chooseColors(Color.BLACK, Color.GREEN).error shouldBe null
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        val restored = json.decodeFromString<GameState>(json.encodeToString(d.state))
        restored.getEntity(id)!!.chosenColors() shouldBe listOf(Color.BLACK, Color.GREEN)

        val sdkJson = Json { encodeDefaults = true }
        val entry = EntersWithChoice(ChoiceType.COLOR, colorCount = 2)
        sdkJson.decodeFromString<ReplacementEffect>(sdkJson.encodeToString<ReplacementEffect>(entry)) shouldBe entry
        entry.description shouldBe "As this permanent enters, choose two colors"
        val space: IterationSpace = IterationSpace.ChosenColorsOf()
        sdkJson.decodeFromString<IterationSpace>(sdkJson.encodeToString(space)) shouldBe space
    }

    test("colorCount is only for color choices, and no more than the choosable colors") {
        shouldThrow<IllegalArgumentException> { EntersWithChoice(ChoiceType.CREATURE_TYPE, colorCount = 2) }
        shouldThrow<IllegalArgumentException> {
            EntersWithChoice(ChoiceType.COLOR, colorCount = 5, excludedColors = setOf(Color.RED))
        }
    }

    test("answerWith tops a ranked pick up to the exact count; minimalAnswers enumerates the pairs") {
        val decision = ChooseColorDecision(
            id = "d", playerId = EntityId.of("p"), prompt = "Choose two colors",
            context = DecisionContext(phase = DecisionPhase.RESOLUTION), maxColors = 2, minColors = 2
        )
        decision.answerWith(listOf(Color.GREEN)).colors shouldBe listOf(Color.GREEN, Color.WHITE)
        decision.minimalAnswers().size shouldBe 10
        decision.copy(minColors = 1, maxColors = 1).answerWith(listOf(Color.RED)) shouldBe
            ColorChosenResponse("d", Color.RED)
    }
})
