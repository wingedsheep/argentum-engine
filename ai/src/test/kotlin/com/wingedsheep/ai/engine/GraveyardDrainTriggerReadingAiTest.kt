package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "{1}{U}{B}: Whenever a card is put into an opponent's graveyard from anywhere this turn, that
 * player loses 1 life" (Duskmantle Guildmage) — a global trigger created until end of turn whose
 * "that player" is the owner of an opponent's card — reads as a drain on the opponent
 * ([IntentTag.DAMAGE_TO_PLAYERS]), and is spent only while the turn's fights and main phases are
 * still ahead.
 *
 * The decision it changes: the activation is left open in a main phase and refused in the end
 * step, where it would wear off before anything else reaches their graveyard.
 */
class GraveyardDrainTriggerReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val guildmage by lazy { cardRegistry.requireCard("Duskmantle Guildmage") }

    private val policy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }

    private fun verdictOn(activePlayer: Int, step: Step): TimingVerdict {
        val game = scenario()
            .withPlayers()
            .withTurnNumber(6)
            .withActivePlayer(activePlayer)
            .withCardOnBattlefield(1, "Duskmantle Guildmage")
            .withLandsOnBattlefield(1, "Island", 2)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withCardOnBattlefield(2, "Hill Giant")
            .build()
            .advanceToPriority(1, step)
        return policy.verdictFor(
            game.state, game.player1Id, "Duskmantle Guildmage",
            activation = ActivateAbility(
                game.player1Id, game.findPermanent("Duskmantle Guildmage")!!, guildmage.script.activatedAbilities[0].id,
            ),
        )
    }

    init {
        test("the graveyard drain trigger reads as damage to the opponent, with a timing") {
            val ability = guildmage.script.activatedAbilities[0]
            CardIntentAnalyzer.analyzeEffect(ability.effect, ability.targetRequirements, source = guildmage).tags shouldContain
                IntentTag.DAMAGE_TO_PLAYERS
            val reading = AiReadingAudit.read(guildmage)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the graveyard drain trigger is open in a main phase and refused in the end step") {
            withClue("in our precombat main, combat and spells are still ahead") {
                verdictOn(activePlayer = 1, step = Step.PRECOMBAT_MAIN) shouldNotBe TimingVerdict.NoWindow
            }
            withClue("in our end step it wears off before anything else reaches their graveyard") {
                verdictOn(activePlayer = 1, step = Step.END) shouldBe TimingVerdict.NoWindow
            }
        }
    }
}
