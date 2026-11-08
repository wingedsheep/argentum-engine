package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.CreatureTypeSynergy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "Equipped creature … must be blocked by an Eldrazi if able" (Slayer's Cleaver): the AI reads the
 * lure as combat enhancement, not as an ask for Eldrazi, and as the defender it obeys the
 * requirement with its Eldrazi even when it would rather not block at all.
 */
class BlockerFilteredMustBeBlockedAiTest : ScenarioTestBase() {

    private val drone = card("Test Eldrazi Chump") {
        typeLine = "Creature — Eldrazi Drone"
        power = 1
        toughness = 1
    }

    private val wallOfEldrazi = card("Test Eldrazi Bulwark") {
        typeLine = "Creature — Eldrazi Drone"
        power = 2
        toughness = 6
    }

    init {
        cardRegistry.register(listOf(drone, wallOfEldrazi))

        test("Slayer's Cleaver reads as combat enhancement and asks for no tribe") {
            val cleaver = cardRegistry.requireCard("Slayer's Cleaver")
            CardIntentAnalyzer.analyze(cleaver).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
            CreatureTypeSynergy.rewarded(cleaver) shouldBe emptySet()
        }

        test("the defending AI blocks the equipped creature with its Eldrazi") {
            val game = scenario()
                .withPlayers("Attacker", "AI")
                .withCardOnBattlefield(1, "Slayer's Cleaver")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, drone.name)
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cleaver = game.findPermanent("Slayer's Cleaver")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val equipId = cardRegistry.requireCard("Slayer's Cleaver").activatedAbilities.first { it.isEquipAbility }.id
            game.execute(ActivateAbility(game.player1Id, cleaver, equipId, targets = listOf(ChosenTarget.Permanent(bears))))
                .error shouldBe null
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            val chump = game.findPermanent(drone.name)!!
            val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
            val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
            withClue("blocks: $blocks") { blocks[chump] shouldBe listOf(bears) }
            game.execute(action).error shouldBe null
        }

        test("with two Eldrazi able to obey it, the AI blocks with the one that survives") {
            for (order in listOf(listOf(drone.name, wallOfEldrazi.name), listOf(wallOfEldrazi.name, drone.name))) {
                val game = scenario()
                    .withPlayers("Attacker", "AI")
                    .withCardOnBattlefield(1, "Slayer's Cleaver")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(2, order[0])
                    .withCardOnBattlefield(2, order[1])
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val cleaver = game.findPermanent("Slayer's Cleaver")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val equipId = cardRegistry.requireCard("Slayer's Cleaver").activatedAbilities.first { it.isEquipAbility }.id
                game.execute(ActivateAbility(game.player1Id, cleaver, equipId, targets = listOf(ChosenTarget.Permanent(bears))))
                    .error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val bulwark = game.findPermanent(wallOfEldrazi.name)!!
                val action = AIPlayer.create(cardRegistry, game.player2Id, AiProfile.LIVE).chooseAction(game.state)
                val blocks = action.shouldBeInstanceOf<DeclareBlockers>().blockers
                withClue("order $order, blocks: $blocks") { blocks[bulwark] shouldBe listOf(bears) }
                game.execute(action).error shouldBe null
            }
        }
    }
}
