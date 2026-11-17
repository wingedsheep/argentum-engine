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
 * An until-end-of-turn "when this creature dies, …" grant over a group — Whisperwood Elemental's
 * "Sacrifice this creature: Until end of turn, face-up nontoken creatures you control gain 'When
 * this creature dies, manifest the top card of your library.'"
 *
 * Its payoff is spent only if one of those creatures dies before cleanup, the same window a
 * death-return grant (Feign Death) waits for. With no fight left and nothing on the stack, the
 * sacrifice throws the Elemental away for nothing; with removal aimed at our creature, it is the
 * answer.
 */
class DiesTriggerGrantTimingTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    /** The "nothing can spend it" floor alone. */
    private val floorOnlyPolicy by lazy { HoldPolicy(intents, refuseUnspendableGrants = true) }

    private fun TestGame.sacrifice(): ActivateAbility {
        val ability = cardRegistry.requireCard(WHISPERWOOD).script.activatedAbilities.single()
        return ActivateAbility(player1Id, findPermanent(WHISPERWOOD)!!, ability.id)
    }

    private fun TestGame.verdict(): TimingVerdict =
        floorOnlyPolicy.verdictFor(state, player1Id, WHISPERWOOD, activation = sacrifice())

    private fun board(active: Int) = scenario().withPlayers()
        .withActivePlayer(active)
        .withCardOnBattlefield(1, WHISPERWOOD)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardInHand(2, "Murder")
        .withLandsOnBattlefield(2, "Swamp", 3)
        .apply { repeat(3) { withCardInLibrary(1, "Forest"); withCardInLibrary(2, "Swamp") } }
        .build()

    init {
        test("Whisperwood Elemental's sacrifice reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard(WHISPERWOOD))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("our second main phase, empty stack: nothing can die before cleanup, so no window") {
            val game = board(active = 1).advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.verdict() shouldBe TimingVerdict.NoWindow
        }

        test("removal aimed at our Bears: the grant can still pay off") {
            val game = board(active = 2).advanceToPriority(2, Step.PRECOMBAT_MAIN)
            game.castSpell(2, "Murder", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.passPriority()
            game.verdict() shouldNotBe TimingVerdict.NoWindow
        }
    }

    private companion object {
        const val WHISPERWOOD = "Whisperwood Elemental"
    }
}
