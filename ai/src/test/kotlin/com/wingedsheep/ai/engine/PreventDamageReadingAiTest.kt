package com.wingedsheep.ai.engine

import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleMove
import com.wingedsheep.ai.puzzles.PuzzleResult
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.ai.puzzles.advanceToStackResponse
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * An instant-speed activation that only prevents damage until end of turn, read as a shield: it
 * buys something only when damage can still come this turn — a fight its recipient is in, or a
 * spell or ability on the stack it answers. In our own precombat main, with nothing on the stack,
 * tapping Samite Healer shields nothing and leaves it tapped through the opponent's attack.
 */
class PreventDamageReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun decide(position: (ScenarioBuilder) -> TestGame, check: PuzzleMove.() -> Unit): PuzzleResult =
        runner.run(
            AiPuzzle(
                id = "prevent-damage-activation",
                category = PuzzleCategory.PRIORITY_TIMING,
                expectation = "",
                aiSeat = 1,
                position = position,
                check = check,
            ),
            AiProfile.LIVE,
        )

    /** Samite Healer ({T}: prevent the next 1 damage to any target this turn) in our precombat main. */
    private val healerInOurMain = { s: ScenarioBuilder ->
        s.withPlayers()
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Samite Healer")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
    }

    /** The same board in our postcombat main: no later window to defer to, and no damage left to come. */
    private val healerInOurPostcombatMain = { s: ScenarioBuilder ->
        healerInOurMain(s).advanceToPriority(1, Step.POSTCOMBAT_MAIN)
    }

    init {
        test("a damage shield is not bought in our own main phase with nothing to prevent") {
            val result = decide(healerInOurMain) { shouldNotActivate("Samite Healer") }
            withClue("an end-of-turn prevention shield reads as a payoff spent only on damage this turn: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a damage shield is refused once no damage can come this turn") {
            val result = decide(healerInOurPostcombatMain) { shouldNotActivate("Samite Healer") }
            withClue("an end-of-turn prevention shield with no damage left this turn buys nothing: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }

        test("a damage shield still answers lethal burn aimed at our face") {
            // At 5 life, the opponent's Lava Axe (5 damage to target player) in their precombat main: one prevented is the game.
            val game = scenario().withPlayers()
                .withActivePlayer(2)
                .withLifeTotal(1, 5)
                .withCardOnBattlefield(1, "Samite Healer")
                .withLandsOnBattlefield(2, "Mountain", 5)
                .withCardInHand(2, "Lava Axe")
                .build()
            game.advanceToPriority(2, Step.PRECOMBAT_MAIN)
            game.castSpellTargetingPlayer(2, "Lava Axe", 1).error shouldBe null
            game.advanceToStackResponse(1)

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val source = (action as? ActivateAbility)?.let { game.state.getEntity(it.sourceId)?.get<CardComponent>()?.name }
            withClue("a shield on us is still spent against burn on the stack: chose $action") {
                source shouldBe "Samite Healer"
            }
        }
    }
}
