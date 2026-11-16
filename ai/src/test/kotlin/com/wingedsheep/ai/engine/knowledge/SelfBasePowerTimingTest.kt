package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * "This creature has base power 8 until end of turn" (Crater Elemental's formidable ability): a
 * [com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect] on its own source, gone at cleanup —
 * the holder's body reset for this turn's fight, read as the self-pump it is. Unread, the
 * end-of-turn timing reader had no payoff for it, so the AI would buy it in a postcombat main
 * phase with no fight left to spend it in.
 */
class SelfBasePowerTimingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val noop by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }
    private val both by lazy { HoldPolicy(intents, holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true) }

    private val formidable by lazy { cardRegistry.getCard("Crater Elemental")!!.script.activatedAbilities[1] }

    private fun TestGame.verdict(policy: HoldPolicy) = policy.verdictFor(
        state, player1Id, "Crater Elemental",
        activation = ActivateAbility(player1Id, findPermanent("Crater Elemental")!!, formidable.id),
    )

    private fun board() = scenario().withPlayers()
        .withCardOnBattlefield(1, "Crater Elemental")
        .withCardOnBattlefield(1, "Craw Wurm")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardOnBattlefield(2, "Hill Giant")

    init {
        test("the base-power reset reads as a self-pump with an until-end-of-turn window") {
            ExpiringGrantWindow.hasExpiringReading(
                formidable.effect, creatureGrants = true, source = cardRegistry.getCard("Crater Elemental"),
            ) shouldBe true
        }

        test("our postcombat main: no fight is left to spend base power 8 in") {
            val game = board().build().advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.verdict(noop) shouldBe TimingVerdict.NoWindow
        }

        test("our precombat main: held for combat, though the Elemental can still attack") {
            val game = board().build().advanceToPriority(1, Step.PRECOMBAT_MAIN)
            game.verdict(noop) shouldBe TimingVerdict.Neutral
            game.verdict(both) shouldBe TimingVerdict.NoWindow
        }

        test("their attack is in and the Elemental can block: base power 8 is worth buying") {
            val game = board().withActivePlayer(2).build()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            game.verdict(noop) shouldBe TimingVerdict.Neutral
            game.verdict(both) shouldBe TimingVerdict.Neutral
        }
    }
}
