package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A one-shot "+X/-X" whose toughness loss is a count off the board — Tezzeret the Schemer's −2,
 * "target creature gets +X/-X until end of turn, where X is the number of artifacts you control",
 * Chatterfang's "+X/-X" for X Squirrels sacrificed — is removal. Read unknown, the slot took the
 * fallback ranking; read harmful, the AI aims it at the opponent's creature, not its own.
 */
class DynamicMinusToughnessPolarityAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a toughness loss counted off the board reads harmful") {
            val tezzeret = cardRegistry.requireCard("Tezzeret the Schemer")
            val minusTwo = tezzeret.script.activatedAbilities[1]
            TargetPolarityAnalyzer.forAbility(tezzeret, minusTwo) shouldBe listOf(TargetPolarity.HARMFUL)
            val chatterfang = cardRegistry.requireCard("Chatterfang, Squirrel General")
            TargetPolarityAnalyzer.forAbility(chatterfang, chatterfang.script.activatedAbilities.single()) shouldBe
                listOf(TargetPolarity.HARMFUL)
            withClue("Tezzeret the Schemer reads fully") {
                AiReadingAudit.read(tezzeret).readFully shouldBe true
            }
        }

        test("Tezzeret the Schemer's −2 aims at the opponent's creature") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(1)
                .withCardOnBattlefield(1, "Tezzeret the Schemer")
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Bonesplitter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val minusTwoId = cardRegistry.requireCard("Tezzeret the Schemer").script.activatedAbilities[1].id
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first {
                (it.action as? ActivateAbility)?.abilityId == minusTwoId
            }
            val rank = TargetSelection.ranker(game.state, action, game.player1Id, intents, polarityFromEffect = true)
            val best = TargetSelection.targetInfosFor(action)!!.single().validTargets.maxBy(rank)

            withClue("+2/-2 kills a 2/2: the opponent's Grizzly Bears, not ours") {
                game.state.getEntity(best)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
                TargetSelection.isOurs(game.state, best, game.player1Id) shouldBe false
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
            }
        }
    }
}
