package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.puzzles.AiPuzzle
import com.wingedsheep.ai.puzzles.PuzzleCategory
import com.wingedsheep.ai.puzzles.PuzzleRunner
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Untap target attacking creature an opponent controls and remove it from combat" (Spires of
 * Orazca, Labyrinth of Skophos): the attacker deals no damage — one creature shut off for the
 * combat, read as [IntentTag.NEUTRALIZE]. Pulling our own attacker out (Reconnaissance) saves it
 * and is no answer.
 */
class RemoveFromCombatReadingAiTest : ScenarioTestBase() {

    private val runner by lazy { PuzzleRunner(cardRegistry) { scenario() } }

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("'$name' is not in the corpus")

    init {
        test("removing an opponent's attacker from combat neutralizes it; our own does not") {
            val spires = card("Thaumatic Compass").backFace!!.script.activatedAbilities.last()
            withClue("Spires of Orazca's combat ability reads as neutralizing the attacker") {
                CardIntentAnalyzer.analyzeEffect(spires.effect, spires.targetRequirements).tags shouldContain IntentTag.NEUTRALIZE
                AiReadingAudit.audit(card("Thaumatic Compass")).shouldBeEmpty()
            }
            val recon = card("Reconnaissance").script.activatedAbilities.single()
            withClue("Reconnaissance pulls our own attacker out: no answer") {
                CardIntentAnalyzer.analyzeEffect(recon.effect, recon.targetRequirements).tags shouldNotContain IntentTag.NEUTRALIZE
            }
        }

        test("an attacker that would be lethal is removed from combat") {
            val result = runner.run(
                AiPuzzle(
                    id = "remove-lethal-attacker",
                    category = PuzzleCategory.HOLDING_INSTANTS,
                    expectation = "Hill Giant attacks for lethal: Labyrinth of Skophos takes it out of combat",
                    aiSeat = 1,
                    position = { s ->
                        s.withPlayers()
                            .withActivePlayer(2)
                            .withLifeTotal(1, 3)
                            .withLandsOnBattlefield(1, "Plains", 4)
                            .withCardOnBattlefield(1, "Labyrinth of Skophos")
                            .withCardOnBattlefield(2, "Hill Giant")
                            .apply { repeat(4) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Plains") } }
                            .build()
                            .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                            .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                            .advanceToDeclaration(1, Step.DECLARE_BLOCKERS)
                            .also { it.declareBlockers(emptyMap()) }
                            .advanceToPriority(1, Step.DECLARE_BLOCKERS)
                    },
                    check = { shouldActivate("Labyrinth of Skophos"); shouldTarget("Hill Giant") },
                ),
                AiProfile.LIVE,
            )
            withClue("removing the lethal attacker from combat is read as neutralizing it: chose ${result.move} — ${result.failure}") {
                result.passed shouldBe true
            }
        }
    }
}
