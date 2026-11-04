package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToDeclaration
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Prevent all damage that would be dealt this turn by a source of your choice" (Mourner's Shield,
 * Burrenton Forge-Tender) — a [com.wingedsheep.sdk.scripting.effects.PreventionDirection.FromTarget]
 * shield read from the **source's** side rather than a recipient's.
 *
 * Timing: the silence is spent only on damage still to come this turn — an opposing creature with
 * a fight left, or something on the stack — and, since the source is named as it resolves, its
 * window is after blocks. Choice: the source named is the threat it is raised against (an opposing
 * spell on the stack, then an opposing creature in combat), not the card worth keeping most, which
 * the generic selection ranking used to answer with one of our own creatures.
 */
class DamageSourcePreventionAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private val tenderAbility by lazy {
        intents.definition("Burrenton Forge-Tender")!!.script.activatedAbilities.single().id
    }

    private fun TestGame.tenderActivation() =
        ActivateAbility(player1Id, findPermanent("Burrenton Forge-Tender")!!, tenderAbility)

    private fun TestGame.tenderVerdict(): TimingVerdict =
        HoldPolicy(intents, holdExpiringGrantsForCombat = true, refuseUnspendableGrants = true).verdictFor(
            state, player1Id, "Burrenton Forge-Tender", activation = tenderActivation(),
        )

    private fun TestGame.aiSourceChoice(): List<EntityId> {
        val decision = state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        val response = AIPlayer.create(cardRegistry, player1Id, AiProfile.LIVE).respondToDecision(state, decision)
        return response.shouldBeInstanceOf<CardsSelectedResponse>().selectedCards
    }

    /** Bob's turn: a red Hill Giant and a red Raging Goblin against Alice's Forge-Tender and Shivan Dragon. */
    private fun theirTurn() = scenario()
        .withPlayers("Alice", "Bob")
        .withActivePlayer(2)
        .withCardOnBattlefield(1, "Burrenton Forge-Tender")
        .withCardOnBattlefield(1, "Shivan Dragon", tapped = true)
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Raging Goblin")
        .withLandsOnBattlefield(2, "Mountain", 1)
        .withCardInHand(2, "Lightning Bolt")
        .build()

    init {
        test("the chosen-source silences read fully") {
            for (card in listOf("Mourner's Shield", "Burrenton Forge-Tender")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(card))
                withClue("$card: $reading") { reading.readFully shouldBe true }
            }
        }

        test("held through their declare-attackers step: the source is named once blocks are in") {
            val game = theirTurn()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Hill Giant" to 1)) }
                .advanceToPriority(1, Step.DECLARE_ATTACKERS)
            game.tenderVerdict() shouldBe TimingVerdict.NoWindow

            game.advanceToPriority(1, Step.DECLARE_BLOCKERS)
            game.tenderVerdict() shouldBe TimingVerdict.Neutral
        }

        test("refused once no damage is left to come this turn") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Burrenton Forge-Tender")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.POSTCOMBAT_MAIN)
            game.tenderVerdict() shouldBe TimingVerdict.NoWindow
        }

        test("names the attacking Goblin, not the idle Giant or our own Dragon") {
            val game = theirTurn()
                .advanceToDeclaration(2, Step.DECLARE_ATTACKERS)
                .also { it.declareAttackers(mapOf("Raging Goblin" to 1)) }
                .advanceToPriority(1, Step.DECLARE_BLOCKERS)
            game.execute(game.tenderActivation()).error shouldBe null
            game.resolveStack()

            game.aiSourceChoice() shouldContainExactly listOf(game.findPermanent("Raging Goblin")!!)
        }

        test("names their Lightning Bolt on the stack ahead of any creature") {
            val game = theirTurn()
            game.state.phase shouldBe Phase.PRECOMBAT_MAIN
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Shivan Dragon")!!).error shouldBe null
            val bolt = game.state.stack.last()
            game.passPriority().error shouldBe null
            game.execute(game.tenderActivation()).error shouldBe null
            game.resolveStack()

            game.aiSourceChoice() shouldContainExactly listOf(bolt)
        }
    }
}
