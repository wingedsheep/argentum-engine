package com.wingedsheep.ai.insight

import com.wingedsheep.ai.engine.AIPlayer
import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

/**
 * The local testing mode's contract: what the panel shows is the AI's *actual* workings.
 *
 * The value of this feature depends entirely on the captured ratings being the ones that drove the
 * move — a display recomputed on the side would be a second opinion, and would quietly disagree with
 * the AI exactly when someone is trying to work out why it blundered. So the assertions here are all
 * agreement assertions: the option marked chosen is the action the AI returned, and its score is the
 * highest one recorded.
 */
class AiInsightCaptureTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2,
    )
    val hillGiant = CardDefinition.creature(
        name = "Hill Giant",
        manaCost = ManaCost.parse("{3}{R}"),
        subtypes = setOf(Subtype("Giant")),
        power = 3, toughness = 3,
    )
    val cadet = CardDefinition.creature(
        name = "Eager Cadet",
        manaCost = ManaCost.parse("{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Soldier")),
        power = 1, toughness = 1,
    )
    val drake = CardDefinition.creature(
        name = "Wind Drake",
        manaCost = ManaCost.parse("{2}{U}"),
        subtypes = setOf(Subtype("Drake")),
        power = 2, toughness = 2,
        keywords = setOf(Keyword.FLYING),
    )
    val cards = listOf(bear, hillGiant, cadet, drake)

    fun setup(): Triple<CardRegistry, GameTestDriver, MutableList<Pair<GameState, AiDecisionInsight>>> {
        val registry = CardRegistry().apply {
            register(cards)
            register(TestCards.all)
        }
        val driver = GameTestDriver().apply {
            registerCards(cards)
            registerCards(TestCards.all)
            initMirrorMatch(deck = Deck.of("Plains" to 20, "Forest" to 20), startingLife = 20)
        }
        return Triple(registry, driver, mutableListOf())
    }

    fun aiWithSink(
        registry: CardRegistry,
        playerId: EntityId,
        captured: MutableList<Pair<GameState, AiDecisionInsight>>,
    ): AIPlayer = AIPlayer.create(
        registry, playerId, AiProfile.PRODUCTION,
        insightSink = { state, insight -> captured += state to insight },
    )

    test("a priority decision records every candidate, ranked, with passing as the baseline") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Two castable creatures — a real choice, so the Strategist scores rather than short-circuits.
        driver.putCardInHand(p1, "Grizzly Bears")
        driver.putCardInHand(p1, "Hill Giant")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 3)

        val action = aiWithSink(registry, p1, captured).chooseAction(driver.state)

        captured.shouldNotBeEmpty()
        val insight = captured.last().second
        insight.kind shouldBe AiDecisionKind.PRIORITY
        insight.playerId shouldBe p1
        insight.baselineLabel shouldBe "Pass priority"

        // Both spells were weighed, not just the one that won.
        val labels = insight.options.map { it.label }
        labels.any { it.contains("Grizzly Bears") }.shouldBeTrue()
        labels.any { it.contains("Hill Giant") }.shouldBeTrue()
        insight.options.count { it.baseline } shouldBe 1

        // The captured ranking is the ranking that produced the move.
        val chosen = insight.options.single { it.chosen }
        chosen.label shouldBe insight.chosenLabel
        val scored = insight.options.mapNotNull { it.score }
        chosen.score.shouldNotBeNull() shouldBe scored.max()
        if (action is CastSpell) {
            chosen.baseline shouldBe false
            chosen.score.shouldNotBeNull().shouldBeGreaterThan(insight.baselineScore)
            chosen.cardName shouldNotBe null
        } else {
            chosen.baseline shouldBe true
        }
    }

    test("plan labels name the creatures involved rather than their entity ids") {
        val (_, driver, _) = setup()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        val cadetId = driver.putCreatureOnBattlefield(p2, "Eager Cadet")
        val state = driver.state

        AiInsightLabels.nameOf(state, giant) shouldBe "Hill Giant"
        AiInsightLabels.describeAttackPlan(state, mapOf(giant to p2)) shouldContain "Hill Giant"
        AiInsightLabels.describeAttackPlan(state, emptyMap()) shouldBe "No attacks"
        AiInsightLabels.describeBlockPlan(state, mapOf(cadetId to listOf(giant))) shouldBe
            "Eager Cadet blocks Hill Giant"
        // An entry with no assignment is not a block — it must not read as one.
        AiInsightLabels.describeBlockPlan(state, mapOf(cadetId to emptyList())) shouldBe "No blocks"
    }

    test("declaring attackers records the plans local search compared against not attacking") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)
        // An opponent creature is what makes the advisor run its simulation-backed search at all.
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val action = aiWithSink(registry, p1, captured).chooseAction(driver.state)

        captured.shouldNotBeEmpty()
        val insight = captured.last().second
        insight.kind shouldBe AiDecisionKind.DECLARE_ATTACKERS
        insight.baselineLabel shouldBe "No attacks"

        val chosen = insight.options.single { it.chosen }
        chosen.label shouldBe insight.chosenLabel
        val declared = (action as DeclareAttackers).attackers
        chosen.label shouldBe AiInsightLabels.describeAttackPlan(driver.state, declared)

        // One plan, one row: the search re-visits the same assignment across its passes, and the
        // panel must not show it twice.
        val labels = insight.options.map { it.label }
        labels.distinct().size shouldBe labels.size
    }

    test("a plan recorded from a map that is mutated afterwards stays one row") {
        val trace = CombatPlanTrace()
        // What local search actually does: record the seed plan off its live map, drop the plan,
        // then re-visit the same assignment as a mutation of the empty plan.
        val livePlan = mutableMapOf(EntityId("attacker") to EntityId("defender"))
        trace.recordAttack(livePlan, -6.0)
        livePlan.clear()
        trace.recordAttack(livePlan, 187.0)
        trace.recordAttack(mapOf(EntityId("attacker") to EntityId("defender")), -6.0)

        trace.plans.size shouldBe 2
    }

    test("the sink sees the position the decision was made from, not a later one") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInHand(p1, "Grizzly Bears")
        driver.putCardInHand(p1, "Hill Giant")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 3)

        val before = driver.state
        aiWithSink(registry, p1, captured).chooseAction(before)

        val (recordedState, insight) = captured.last()
        // Same turn/step as the live position — an export has to reproduce the decision, so a state
        // captured after the action would make the whole bundle useless as training input.
        recordedState.turnNumber shouldBe before.turnNumber
        recordedState.step shouldBe before.step
        insight.turnNumber shouldBe before.turnNumber
        insight.step shouldBe before.step.name
    }

    test("every scored option carries an action the engine actually accepts") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInHand(p1, "Grizzly Bears")
        driver.putCardInHand(p1, "Hill Giant")
        driver.putCardInHand(p1, "Wind Drake")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveMana(p1, Color.BLUE, 1)
        driver.giveColorlessMana(p1, 4)

        aiWithSink(registry, p1, captured).chooseAction(driver.state)
        val (state, insight) = captured.last()

        val scored = insight.options.filter { it.score != null }
        scored.size shouldBeGreaterThan 2

        // This is the whole contract behind "play this option instead": the local testing mode
        // submits the recorded action verbatim, so an option the panel offers must be one the
        // processor takes. Anything less and overriding would wedge the game on a rejected move.
        val processor = ActionProcessor(registry)
        scored.forEach { option ->
            val action = option.action.shouldNotBeNull()
            withClue("option '${'$'}{option.label}' should be submittable") {
                processor.process(state, action).result.error shouldBe null
            }
        }
    }

    test("an option the processor already rejected is listed but never offered as playable") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInHand(p1, "Grizzly Bears")
        driver.putCardInHand(p1, "Hill Giant")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 3)

        aiWithSink(registry, p1, captured).chooseAction(driver.state)
        val insight = captured.last().second

        // A note explaining an engine refusal must never come with an action attached — that pairing
        // is exactly what would let the panel offer a move the processor will reject.
        insight.options.forEach { option ->
            if (option.note?.contains("illegal") == true) option.action shouldBe null
        }
    }

    test("combat plans carry the declaration that would play them") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        val p2 = driver.player2
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val attacker = driver.putCreatureOnBattlefield(p1, "Hill Giant")
        driver.removeSummoningSickness(attacker)
        driver.putCreatureOnBattlefield(p2, "Grizzly Bears")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        aiWithSink(registry, p1, captured).chooseAction(driver.state)

        val (state, insight) = captured.last()
        val processor = ActionProcessor(registry)
        insight.options.forEach { option ->
            val action = option.action.shouldNotBeNull()
            (action is DeclareAttackers).shouldBeTrue()
            withClue("plan '${'$'}{option.label}' should be submittable") {
                processor.process(state, action).result.error shouldBe null
            }
        }
    }

    test("no sink means no recording — the production path is untouched") {
        val (registry, driver, captured) = setup()
        val p1 = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardInHand(p1, "Grizzly Bears")
        driver.putCardInHand(p1, "Hill Giant")
        driver.giveMana(p1, Color.GREEN, 1)
        driver.giveMana(p1, Color.RED, 1)
        driver.giveColorlessMana(p1, 3)

        AIPlayer.create(registry, p1, AiProfile.PRODUCTION).chooseAction(driver.state)

        captured.size shouldBe 0
    }
})
