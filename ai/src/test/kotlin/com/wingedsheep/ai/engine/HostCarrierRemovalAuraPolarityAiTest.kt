package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * An Aura that destroys the creature its host is attached to — "Enchant Equipment. At the
 * beginning of your upkeep, if enchanted Equipment is attached to a creature, destroy that
 * creature" (Artificer's Hex), a destroy-all over `Creature.hasAttached(Any.attachedToBySource())`
 * — curses whoever wears the Equipment, so the enchant slot reads [TargetPolarity.HARMFUL]. The
 * filter reaches one creature at most: spot removal, never a sweeper.
 *
 * The decision it changes: with only our own Equipment to enchant, the Hex would kill our own
 * equipped creature every upkeep; the AI no longer aims it there.
 */
class HostCarrierRemovalAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("destroying the creature wearing the host curses the host, and is spot removal") {
            TargetPolarityAnalyzer.forSpell(card("Artificer's Hex")) shouldBe listOf(TargetPolarity.HARMFUL)
            val tags = CardIntentAnalyzer.analyze(card("Artificer's Hex")).tags
            tags shouldContain IntentTag.REMOVAL
            withClue("one creature at most is no wrath") { tags shouldNotContain IntentTag.SWEEPER }
        }

        test("Artificer's Hex is not cast on our own Equipment") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Bonesplitter", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Artificer's Hex")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Artificer's Hex"
            }

            withClue("our Bonesplitter is the only Equipment to enchant") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
