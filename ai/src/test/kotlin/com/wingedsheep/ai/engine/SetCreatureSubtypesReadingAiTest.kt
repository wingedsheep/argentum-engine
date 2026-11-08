package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Target creature becomes a Coward" / "becomes a Warrior"
 * ([com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect]) on a card whose own
 * "Cowards can't block Warriors" names the type: the blocker type takes the creature's blocks away
 * ([TargetPolarity.HARMFUL]), the protected type makes it evade them ([TargetPolarity.BENEFICIAL]),
 * both an [IntentTag.EVASION_GRANT] — and until end of turn, a payoff spent only in a fight its
 * creature can still be in.
 *
 * The decisions it changes: Boldwyr Intimidator's "becomes a Coward" is no longer spent on our own
 * creature when theirs can't be targeted, nor on their creature once our combat is over; Kargan
 * Intimidator's modal "becomes a Coward", whose target is asked after the mode is announced, goes
 * on the opponent's creature.
 */
class SetCreatureSubtypesReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val boldwyr by lazy { card("Boldwyr Intimidator") }

    private fun TestGame.cowardVerdict(target: String): TimingVerdict =
        HoldPolicy(intents, refuseWastedPayoffs = true).verdictFor(
            state, player1Id, "Boldwyr Intimidator",
            activation = ActivateAbility(
                player1Id, findPermanent("Boldwyr Intimidator")!!, boldwyr.script.activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(findPermanent(target)!!)),
            ),
        )

    private fun position(step: Step) = scenario()
        .withPlayers()
        .withTurnNumber(8)
        .withActivePlayer(1)
        .withCardOnBattlefield(1, "Boldwyr Intimidator")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(2, "Hill Giant")
        .build()
        .advanceToPriority(1, step)

    init {
        test("the type a card's restriction names reads as its side of that restriction") {
            val (coward, warrior) = boldwyr.script.activatedAbilities
            withClue("a Coward can't block Warriors: a curse on its holder") {
                TargetPolarityAnalyzer.forAbility(boldwyr, coward) shouldBe listOf(TargetPolarity.HARMFUL)
            }
            withClue("a Warrior can't be blocked by Cowards: a gift") {
                TargetPolarityAnalyzer.forAbility(boldwyr, warrior) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
            CardIntentAnalyzer.analyzeEffect(coward.effect, coward.targetRequirements, source = boldwyr).tags shouldContain
                IntentTag.EVASION_GRANT
            val gornog = card("Gornog, the Red Reaper")
            val trigger = gornog.script.triggeredAbilities.single()
            TargetPolarityAnalyzer.forRequirements(
                trigger.effect, listOfNotNull(trigger.targetRequirement), source = gornog,
            ) shouldBe listOf(TargetPolarity.HARMFUL)
            for (name in listOf("Boldwyr Intimidator", "Kargan Intimidator", "Gornog, the Red Reaper")) {
                val reading = AiReadingAudit.read(card(name))
                withClue("$name: $reading") { reading.readFully shouldBe true }
            }
        }

        test("Boldwyr's Coward is not aimed at our own creature when theirs can't be targeted") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Boldwyr Intimidator")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Gladecover Scout")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val source = game.findPermanent("Boldwyr Intimidator")!!
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first {
                val activation = it.action as? ActivateAbility
                activation?.sourceId == source && activation.abilityId == boldwyr.script.activatedAbilities[0].id
            }

            withClue("the hexproof Scout leaves only our own creatures to turn into a Coward") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe true
            }
        }

        test("Kargan's Coward mode, targeted after the mode is announced, goes on their creature") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Kargan Intimidator")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val kargan = card("Kargan Intimidator")
            game.execute(
                ActivateAbility(game.player1Id, game.findPermanent("Kargan Intimidator")!!, kargan.script.activatedAbilities[0].id),
            ).error shouldBe null
            val modes = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            val coward = modes.options.indexOfFirst { it.startsWith("Target creature becomes a Coward") }
            game.submitDecision(OptionChosenResponse(modes.id, coward)).error shouldBe null

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            TargetSelection.decisionPolarities(game.state, decision, intents) shouldBe mapOf(0 to TargetPolarity.HARMFUL)
            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            withClue("a Coward can't block Kargan: the curse lands on their Hill Giant, not our Bears") {
                response.shouldBeInstanceOf<TargetsResponse>().selectedTargets.values.flatten() shouldBe
                    listOf(game.findPermanent("Hill Giant")!!)
            }
        }

        test("a Coward until end of turn buys nothing once our combat is over") {
            withClue("before our attack, their Hill Giant made a Coward can't block Boldwyr") {
                position(Step.PRECOMBAT_MAIN).cowardVerdict("Hill Giant") shouldNotBe TimingVerdict.NoWindow
            }
            withClue("after combat, the type wears off at cleanup having changed no block") {
                position(Step.POSTCOMBAT_MAIN).cowardVerdict("Hill Giant") shouldBe TimingVerdict.NoWindow
            }
        }
    }
}
