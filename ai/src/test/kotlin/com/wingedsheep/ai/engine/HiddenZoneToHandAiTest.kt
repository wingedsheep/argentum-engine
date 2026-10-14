package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A spell whose whole payoff is a card from a hidden zone into hand — a wish — is priced by the leaf
 * as a card spent and a card gained: the same tie with passing as a cantrip, which the AI breaks
 * towards passing. [AiProfile.drawWithIdleMana]'s last-main-phase nudge is the general answer for
 * that shape; a wish never reached it, for two reasons fixed here:
 *
 *  - [com.wingedsheep.ai.engine.knowledge.CardIntent.selectsIntoHand] only recognized a gather from
 *    the top of the library, not one from outside the game (the sideboard);
 *  - and the effect walk stops at an "if you do" gate, which is where Extrapolate the Impossible
 *    keeps its whole payoff, so the card read as doing nothing at all.
 *
 * Production evidence: game `2e28f192-a6fa-464e-bd7e-1338719eb4f4` (Reality Fracture draft). The AI
 * held Extrapolate the Impossible from its opening hand to its death on turn 17; on turn 14 it had
 * seven untapped lands, nothing else to cast, and Vraska ×2, Jace, Reality Sculptor, Saheeli and
 * Edgar outside the game. Casting it scored exactly the pass score.
 */
class HiddenZoneToHandAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }
    private val liveWithoutFlag = AiProfile.LIVE.copy(id = "live-without-idledraw", drawWithIdleMana = false)

    private fun decide(
        profile: AiProfile,
        position: (ScenarioBuilder) -> TestGame,
        check: PuzzleMove.() -> Unit,
    ): PuzzleResult = runner.run(
        AiPuzzle(
            id = "hidden-zone-to-hand",
            category = PuzzleCategory.PRIORITY_TIMING,
            expectation = "",
            aiSeat = 1,
            position = position,
            check = check,
        ),
        profile,
    )

    private fun PuzzleResult.shouldSucceed() =
        withClue("${puzzle.id}: chose $move — $failure") { passed shouldBe true }

    /**
     * The turn-14 shape, after its land drop: seven lands, Extrapolate the Impossible in hand, strong
     * cards outside the game, our postcombat main.
     */
    private fun extrapolate(
        sideboard: List<String> = listOf("Vraska, the Cutting Glare", "Jace, Reality Sculptor", "Edgar, Ancient Bloodlord"),
        extraInHand: String? = null,
        lands: Int = 7,
    ) = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Island", lands - lands / 2)
            .withLandsOnBattlefield(1, "Swamp", lands / 2)
            .withCardInHand(1, "Extrapolate the Impossible")
            .let { if (extraInHand != null) it.withCardInHand(1, extraInHand) else it }
            .let { sideboard.fold(it) { b, card -> b.withCardInSideboard(1, card) } }
            .withCardOnBattlefield(2, "Hill Giant")
            .withTurnNumber(14)
            .build()
            .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
    }

    private fun TestGame.castVerdict(cardName: String): TimingVerdict {
        val card = findCardsInHand(1, cardName).first()
        return HoldPolicy(intents, drawWithIdleMana = true)
            .verdictFor(state, player1Id, cardName, cast = CastSpell(player1Id, card))
    }

    init {
        context("reading a wish") {
            test("Extrapolate the Impossible selects into hand from outside the game, through its if-you-do gate") {
                val intent = intents.forName("Extrapolate the Impossible")!!
                intent.selectsIntoHand shouldBe true
                intent.wishFilters.shouldNotBeEmpty()
            }

            test("Burning Wish is a wish too") {
                val intent = intents.forName("Burning Wish")!!
                intent.selectsIntoHand shouldBe true
                intent.wishFilters.shouldNotBeEmpty()
            }

            test("a plain card draw is not a wish") {
                intents.forName("Divination")!!.wishFilters.shouldBeEmpty()
            }
        }

        context("g 2e28f192 T14: Extrapolate the Impossible with idle mana") {
            test("without the idle-draw flag the agent passes; with it, casts the wish") {
                decide(liveWithoutFlag, extrapolate()) { shouldPass() }.shouldSucceed()
                decide(AiProfile.LIVE, extrapolate()) { shouldCast("Extrapolate the Impossible") }.shouldSucceed()
            }

            test("the wish resolves end to end: the AI reveals two, the opponent picks, a card reaches hand") {
                val game = extrapolate()(scenario())
                val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
                val opponentAi = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE)
                val sideboardBefore = game.state.getZone(game.player1Id, Zone.SIDEBOARD).toSet()

                game.execute(ai.chooseAction(game.state)).error shouldBe null
                var guard = 0
                while (guard++ < 20 && (game.state.stack.isNotEmpty() || game.state.pendingDecision != null)) {
                    val decision = game.state.pendingDecision
                    if (decision == null) {
                        game.passPriority()
                        continue
                    }
                    val answerer = if (decision.playerId == game.player1Id) ai else opponentAi
                    game.execute(SubmitDecision(decision.playerId, answerer.respondToDecision(game.state, decision)))
                        .error shouldBe null
                }

                val fetched = game.state.getZone(game.player1Id, Zone.HAND).filter { it in sideboardBefore }
                withClue("one card from outside the game is now in hand") { fetched.size shouldBe 1 }
                game.isInGraveyard(1, "Extrapolate the Impossible") shouldBe true
            }

            test("a wish into a sideboard with nothing it can take gets no nudge, and is not cast") {
                val game = extrapolate(sideboard = emptyList())(scenario())
                game.castVerdict("Extrapolate the Impossible") shouldBe TimingVerdict.Neutral
                decide(AiProfile.LIVE, extrapolate(sideboard = emptyList())) { shouldPass() }.shouldSucceed()
            }

            test("Burning Wish with no sorcery outside the game gets no nudge; with one, it does") {
                val noSorcery = { s: ScenarioBuilder ->
                    s.withPlayers()
                        .withLandsOnBattlefield(1, "Mountain", 4)
                        .withCardInHand(1, "Burning Wish")
                        .withCardInSideboard(1, "Grizzly Bears")
                        .build()
                        .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
                }
                noSorcery(scenario()).castVerdict("Burning Wish") shouldBe TimingVerdict.Neutral

                val withSorcery = { s: ScenarioBuilder ->
                    s.withPlayers()
                        .withLandsOnBattlefield(1, "Mountain", 4)
                        .withCardInHand(1, "Burning Wish")
                        .withCardInSideboard(1, "Grizzly Bears")
                        .withCardInSideboard(1, "Volcanic Hammer")
                        .build()
                        .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
                }
                withSorcery(scenario()).castVerdict("Burning Wish").shouldBeInstanceOf<TimingVerdict.Adjust>()
            }

            test("control: with mana for only one, a creature is cast before the wish") {
                decide(AiProfile.LIVE, extrapolate(extraInHand = "Walking Corpse", lands = 2)) {
                    shouldCast("Walking Corpse")
                }.shouldSucceed()
            }
        }
    }
}
