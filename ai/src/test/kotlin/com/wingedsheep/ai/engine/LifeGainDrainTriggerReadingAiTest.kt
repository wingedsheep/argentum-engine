package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "{1}{W}{B}: Whenever you gain life this turn, each opponent loses that much life" (Vizkopa
 * Guildmage) — a global trigger created until end of turn that drains each opponent for the life we
 * gain — is spent only while the turn's fights and main phases, where that life comes from, are
 * still ahead.
 *
 * The decision it changes: the activation is left open in a main phase and refused in the end
 * step, where it would wear off before any more life is gained.
 */
class LifeGainDrainTriggerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val guildmage by lazy { cardRegistry.requireCard("Vizkopa Guildmage") }

    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }

    private fun verdictOn(step: Step): TimingVerdict {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(6)
            .withActivePlayer(1)
            .withCardOnBattlefield(1, "Vizkopa Guildmage")
            .withLandsOnBattlefield(1, "Plains", 2)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
            .advanceToPriority(1, step)
        return policy.verdictFor(
            game.state, game.player1Id, "Vizkopa Guildmage",
            activation = ActivateAbility(
                game.player1Id, game.findPermanent("Vizkopa Guildmage")!!, guildmage.script.activatedAbilities[1].id,
            ),
        )
    }

    init {
        test("the life-gain drain trigger is read fully, with a timing") {
            val reading = AiReadingAudit.read(guildmage)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the life-gain drain trigger is open in a main phase and refused in the end step") {
            withClue("in our precombat main, combat and lifegain spells are still ahead") {
                verdictOn(Step.PRECOMBAT_MAIN) shouldNotBe TimingVerdict.NoWindow
            }
            withClue("in our end step it wears off before any more life is gained") {
                verdictOn(Step.END) shouldBe TimingVerdict.NoWindow
            }
        }
    }
}
