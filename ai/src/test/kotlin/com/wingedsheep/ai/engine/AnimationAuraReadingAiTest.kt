package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An Aura that makes the noncreature permanent it enchants a creature
 * ([com.wingedsheep.sdk.scripting.GrantCardType] "CREATURE" on its host, with a
 * [com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic] body) hands that permanent's
 * controller a new creature: Puppet Crafting's 5/5 Construct, Zoetic Glyph's 5/4 Golem,
 * Mightstone's Animation's 4/4.
 *
 * Unread, the enchant slot fell back to the removal ranking, which puts the opponent's permanent
 * ahead of ours: the cheap target pick (the Strategist's unsimulated path, every playout) put
 * Puppet Crafting on the opponent's Icy Manipulator and handed them a 5/5.
 *
 * Zoetic Glyph's other half, [com.wingedsheep.sdk.scripting.effects.DiscoverEffect], reads as card
 * draw: discover always ends with one more card cast or in hand.
 */
class AnimationAuraReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an animation Aura's enchant slot is a gift to the host's controller") {
            for (name in listOf("Puppet Crafting", "Zoetic Glyph", "Mightstone's Animation")) {
                withClue("$name: becoming a creature with a large base body is a gift to its controller") {
                    TargetPolarityAnalyzer.forSpell(card(name)) shouldBe listOf(TargetPolarity.BENEFICIAL)
                }
            }
        }

        test("discover is card draw") {
            withClue("Zoetic Glyph's 'discover 3' casts or hands us one more card, so it reads as a draw") {
                CardIntentAnalyzer.analyze(card("Zoetic Glyph")).tags shouldContain IntentTag.DRAW
            }
        }

        test("Puppet Crafting animates our own artifact, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(4)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Puppet Crafting")
                .withCardOnBattlefield(1, "Mind Stone")
                .withCardOnBattlefield(2, "Icy Manipulator")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val crafting = game.state.getZone(game.player1Id, Zone.HAND)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Puppet Crafting" }
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.cardId == crafting }

            // The cheap pick the Strategist and every playout make without simulating.
            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as CastSpell

            val host = (filled.targets.single() as ChosenTarget.Permanent).entityId
            withClue("an animation Aura makes its host a 5/5 for the host's controller, so the host is ours") {
                game.state.projectedState.getController(host) shouldBe game.player1Id
            }
        }
    }
}
