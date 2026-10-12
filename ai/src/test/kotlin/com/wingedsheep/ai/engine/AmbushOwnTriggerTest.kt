package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.AmbushWindow
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [AiProfile.holdFlashPermanentsForAmbush] on [AiProfile.LIVE], from an AI-vs-AI `live` log: the AI
 * flashed in Little Bear during its **own upkeep**, in response to nothing but its own delayed
 * Bird-token trigger from The Eagles Are Coming!. The bear's "untap another target creature you
 * control" fizzled for want of a target, and the card was spent before the draw step.
 *
 * Two faults stacked. `live` never carried the ambush flag at all, and [AmbushWindow] released its
 * floor for *any* stack object — including our own harmless trigger on our own turn, which is no
 * window to respond in.
 */
class AmbushOwnTriggerTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }
    private val liveWithoutFlag =
        AiProfile.LIVE.copy(id = "live-without-ambush", holdFlashPermanentsForAmbush = false)

    /**
     * Seat 1 casts The Eagles Are Coming! on its own Grizzly Bears in seat 2's end step, so the
     * delayed trigger fires at the beginning of seat 1's next upkeep — the position from the log:
     * our turn, our trigger on the stack, Little Bear in hand with the mana to cast it.
     */
    private val ownUpkeep = { s: ScenarioBuilder ->
        val game = s.withPlayers()
            .withActivePlayer(2)
            .withTurnNumber(4)
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Forest", 3)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(1, "The Eagles Are Coming!")
            .withCardInHand(1, "Little Bear")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .build()
            .advanceToPriority(1, Step.END)
        game.castSpell(1, "The Eagles Are Coming!", game.findPermanent("Grizzly Bears")).error shouldBe null
        game.resolveStack()
        game.advanceToPriority(1, Step.UPKEEP)
    }

    private fun decide(profile: AiProfile, check: PuzzleMove.() -> Unit): PuzzleResult = runner.run(
        AiPuzzle(
            id = "ambush-own-trigger",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "",
            aiSeat = 1,
            position = ownUpkeep,
            check = check,
        ),
        profile,
    )

    private fun PuzzleResult.shouldSucceed() =
        withClue("${puzzle.id}: chose $move — $failure") { passed shouldBe true }

    init {
        test("the position is our own upkeep with only our own trigger on the stack") {
            val game = ownUpkeep(scenario())
            game.state.activePlayerId shouldBe game.player1Id
            game.state.step shouldBe Step.UPKEEP
            game.state.stack shouldHaveSize 1
            val trigger = game.state.getEntity(game.state.stack.single())
                ?.get<TriggeredAbilityOnStackComponent>()
            trigger shouldNotBe null
            trigger!!.controllerId shouldBe game.player1Id
        }

        test("our own trigger on the stack does not release the ambush floor") {
            val game = ownUpkeep(scenario())
            val bear = intents.forName("Little Bear") ?: error("Little Bear is not in the registry")
            AmbushWindow.holds(game.state, game.player1Id, bear) shouldBe true
        }

        test("live holds Little Bear in its own upkeep; without the flag it flashes it in") {
            decide(liveWithoutFlag) { shouldCast("Little Bear") }.shouldSucceed()
            decide(AiProfile.LIVE) { shouldNotCast("Little Bear") }.shouldSucceed()
        }
    }
}
