package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Creatures with power less than this creature's power can't block creatures you control"
 * (Champion of Lambholt) and its self form (Howlgeist): the AI reads both as evasion, and its
 * combat math shuts out every blocker smaller than the *source* — for the group form, even when
 * the attacker itself is smaller than the blocker.
 */
class LesserPowerEvasionAiTest : ScenarioTestBase() {

    init {
        test("Champion of Lambholt and Howlgeist read as evasion") {
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Champion of Lambholt")).tags shouldContain
                IntentTag.EVASION_GRANT
            CardIntentAnalyzer.analyze(cardRegistry.requireCard("Howlgeist")).tags shouldContain
                IntentTag.EVASION_GRANT
        }

        test("combat math: blockers smaller than Champion can't block its team; others can") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Champion of Lambholt")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Runeclaw Bear")
                .withCardOnBattlefield(2, "Gray Ogre")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // Grow Champion to 3/3 so the 2-power Ogre is under the bar.
            val champion = game.findPermanent("Champion of Lambholt")!!
            game.state = game.state.updateEntity(champion) {
                it.with(com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                    .withAdded(com.wingedsheep.sdk.core.CounterType.PLUS_ONE_PLUS_ONE, 2))
            }
            val projected = game.state.projectedState
            projected.getPower(champion) shouldBe 3
            val elves = game.findPermanent("Llanowar Elves")!!
            val ogre = game.findPermanent("Gray Ogre")!!
            withClue("the 1/1 Elves can't be blocked by the 2/2 Ogre: Champion's power 3 is the bar") {
                CombatMath.canBeBlockedBy(game.state, projected, elves, ogre, cardRegistry) shouldBe false
            }
            withClue("nor can Champion itself") {
                CombatMath.canBeBlockedBy(game.state, projected, champion, ogre, cardRegistry) shouldBe false
            }
            withClue("the opponent's creatures aren't covered: Elves can block the Goblin") {
                CombatMath.canBeBlockedBy(
                    game.state, projected, game.findPermanent("Raging Goblin")!!, elves, cardRegistry
                ) shouldBe true
            }
        }

        test("combat math: Howlgeist can't be blocked by a smaller creature") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Howlgeist")
                .withCardOnBattlefield(2, "Gray Ogre")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            CombatMath.canBeBlockedBy(
                game.state, game.state.projectedState,
                game.findPermanent("Howlgeist")!!, game.findPermanent("Gray Ogre")!!, cardRegistry
            ) shouldBe false
        }
    }
}
