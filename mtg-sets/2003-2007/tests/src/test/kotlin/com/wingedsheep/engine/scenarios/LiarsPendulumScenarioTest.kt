package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Liar's Pendulum (MRD #196).
 *
 * {1} Artifact
 * "{2}, {T}: Choose a card name. Target opponent guesses whether a card with that name is in your
 *  hand. You may reveal your hand. If you do and your opponent guessed wrong, draw a card."
 *
 * Four decisions in a fixed order — you name a card, the opponent guesses, you choose whether to
 * reveal — and the draw hangs off *both* the reveal happening and the guess having been wrong. The
 * card's rulings are explicit about the two things that are easy to get backwards, so both get a
 * test: the reveal is offered no matter how the guess went, and you can't draw without revealing.
 *
 * The truth table is covered from both sides, because a guess is scored against reality rather than
 * against a fixed answer: naming a card you hold and naming one you don't must both be able to
 * produce a right guess and a wrong one.
 */
class LiarsPendulumScenarioTest : ScenarioTestBase() {

    private val pendulumAbilityId by lazy {
        cardRegistry.requireCard("Liar's Pendulum").activatedAbilities.single().id
    }

    /** Whether the opponent guessed right, and whether the reveal happened, drive the whole card. */
    data class Run(val nameInHand: Boolean, val guess: Boolean, val reveal: Boolean, val draws: Boolean)

    init {
        context("Liar's Pendulum") {

            // The hand always holds Grizzly Bears; `nameInHand` decides whether we point the guess at
            // that card or at one we demonstrably don't hold.
            val runs = listOf(
                Run(nameInHand = true, guess = true, reveal = true, draws = false),
                Run(nameInHand = true, guess = false, reveal = true, draws = true),
                Run(nameInHand = false, guess = true, reveal = true, draws = true),
                Run(nameInHand = false, guess = false, reveal = true, draws = false),
                Run(nameInHand = true, guess = false, reveal = false, draws = false),
            )

            runs.forEach { run ->
                val named = if (run.nameInHand) "Grizzly Bears" else "Shivan Dragon"
                val guessWord = if (run.guess) "yes" else "no"
                val verdict = if (run.guess == run.nameInHand) "right" else "wrong"
                val revealWord = if (run.reveal) "revealing" else "not revealing"
                val outcome = if (run.draws) "draws a card" else "draws nothing"

                test("naming $named, opponent guesses $guessWord ($verdict), $revealWord — $outcome") {
                    val game = scenario()
                        .withPlayers("Player", "Opponent")
                        .withCardOnBattlefield(1, "Liar's Pendulum")
                        .withLandsOnBattlefield(1, "Mountain", 2)
                        .withCardInHand(1, "Grizzly Bears")
                        .withCardInLibrary(1, "Plains")
                        .withCardInLibrary(2, "Island")
                        .withActivePlayer(1)
                        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                        .build()

                    withClue("the run only means anything if the hand really is what we think") {
                        game.isInHand(1, "Grizzly Bears") shouldBe true
                        game.isInHand(1, "Shivan Dragon") shouldBe false
                    }

                    val pendulum = game.findPermanent("Liar's Pendulum")!!
                    val handBefore = game.handSize(1)

                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = pendulum,
                            abilityId = pendulumAbilityId,
                            targets = listOf(ChosenTarget.Player(game.player2Id))
                        )
                    ).error shouldBe null
                    game.resolveStack()

                    // 1. You name a card.
                    val nameDecision = game.getPendingDecision() as? ChooseOptionDecision
                    withClue("the controller names the card first: ${game.getPendingDecision()}") {
                        (nameDecision != null) shouldBe true
                        nameDecision!!.playerId shouldBe game.player1Id
                    }
                    val index = nameDecision!!.options.indexOf(named)
                    withClue("'$named' must be offerable as a card name") { (index >= 0) shouldBe true }
                    game.submitDecision(OptionChosenResponse(nameDecision.id, index)).error shouldBe null

                    // 2. The targeted opponent guesses — never the controller.
                    val guessDecision = game.getPendingDecision() as? YesNoDecision
                    withClue("the guess goes to the targeted opponent: ${game.getPendingDecision()}") {
                        (guessDecision != null) shouldBe true
                        guessDecision!!.playerId shouldBe game.player2Id
                    }
                    withClue("the question names the card, so the guesser knows what they're guessing about") {
                        guessDecision!!.prompt.contains(named) shouldBe true
                    }
                    game.answerYesNo(run.guess).error shouldBe null

                    // 3. You may reveal — offered whichever way the guess went (the card's own ruling).
                    val revealDecision = game.getPendingDecision() as? YesNoDecision
                    withClue("the reveal is offered regardless of the guess: ${game.getPendingDecision()}") {
                        (revealDecision != null) shouldBe true
                        revealDecision!!.playerId shouldBe game.player1Id
                    }
                    game.answerYesNo(run.reveal).error shouldBe null

                    withClue("no decisions left over") {
                        game.hasPendingDecision() shouldBe false
                    }
                    withClue("draw expectation for this run") {
                        game.handSize(1) shouldBe handBefore + if (run.draws) 1 else 0
                    }
                }
            }
        }
    }
}
