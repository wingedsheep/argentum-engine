package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.HostSplit
import com.wingedsheep.ai.engine.knowledge.HostTrait
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * An Aura whose conditional statics turn on the enchanted permanent's card type through
 * `Conditions.EnchantedPermanentMatches` (an `EntityMatches` on the enchanted permanent) — Nyx
 * Infusion's "enchanted creature gets +2/+2 as long as it's an enchantment. Otherwise, it gets
 * -2/-2". Read once per kind of host, it is FLEXIBLE with a [HostSplit] on the enchantment type:
 * a pump for our enchantment creatures, a -2/-2 for their other creatures.
 */
class HostEnchantmentConditionAuraPolarityAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun infusionCast(game: TestGame) =
        GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
            val spell = action.action as? CastSpell ?: return@first false
            game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == INFUSION
        }

    private fun board(ours: List<String>, theirs: List<String>) = scenario()
        .withPlayers()
        .withTurnNumber(6)
        .withActivePlayer(1)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInHand(1, INFUSION)
        .apply { ours.forEach { withCardOnBattlefield(1, it) } }
        .apply { theirs.forEach { withCardOnBattlefield(2, it) } }
        .build()
        .advanceToPriority(1, Step.PRECOMBAT_MAIN)

    init {
        test("Nyx Infusion reads flexible, split by whether the host is an enchantment") {
            val card = cardRegistry.getCard(INFUSION)!!
            TargetPolarityAnalyzer.forSpell(card) shouldBe listOf(TargetPolarity.FLEXIBLE)
            TargetPolarityAnalyzer.hostSplit(card) shouldBe
                HostSplit(HostTrait.CardType("ENCHANTMENT"), TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            val reading = AiReadingAudit.read(card)
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("Nyx Infusion ranks our enchantment creature and their plain creature ahead of the wrong-side ones") {
            val game = board(ours = listOf(AEGIS, "Grizzly Bears"), theirs = listOf(AEGIS, "Grizzly Bears"))
            val action = infusionCast(game)
            val rank = TargetSelection.ranker(game.state, action, game.player1Id, intents, polarityFromEffect = true)
            val candidates = TargetSelection.targetInfosFor(action)!!.single().validTargets
            val names = candidates.sortedByDescending(rank).map { id ->
                val name = game.state.getEntity(id)!!.get<CardComponent>()!!.name
                val side = if (TargetSelection.isOurs(game.state, id, game.player1Id)) "ours" else "theirs"
                "$name ($side)"
            }

            names.take(2) shouldContainExactlyInAnyOrder listOf("$AEGIS (ours)", "Grizzly Bears (theirs)")
            TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, intents) shouldBe false
        }

        test("Nyx Infusion with only our plain creatures and their enchantment creatures to enchant is held") {
            val game = board(ours = listOf("Grizzly Bears"), theirs = listOf(AEGIS))
            withClue("-2/-2 on our Bears and +2/+2 on their Aegis both help the opponent") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, infusionCast(game), game.player1Id, intents) shouldBe true
            }
        }
    }

    private companion object {
        const val INFUSION = "Nyx Infusion"
        const val AEGIS = "Aegis of the Gods"
    }
}
