package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
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
import io.kotest.matchers.shouldBe

/**
 * An Aura that makes the land it enchants every basic land type in addition to its other types
 * (Nylea's Presence — five [com.wingedsheep.sdk.scripting.GrantSubtype]s on its host) gives that
 * land's controller every basic land type's mana ability: a gift, so the enchant slot names our
 * own land. Unread, it fell back to the removal ranking and could enchant the opponent's land.
 */
class LandTypeGiftAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an Aura adding basic land types to its host is a gift to the host's controller") {
            TargetPolarityAnalyzer.forSpell(card("Nylea's Presence")) shouldBe listOf(TargetPolarity.BENEFICIAL)
        }

        test("Nylea's Presence enchants our own land, not the opponent's") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(4)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Nylea's Presence")
                .withLandsOnBattlefield(2, "Island", 2)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)

            val presence = game.state.getZone(game.player1Id, Zone.HAND)
                .single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Nylea's Presence" }
            val cast = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? CastSpell)?.cardId == presence }

            val filled = TargetSelection.fillHeuristically(
                game.state, cast, game.player1Id, fillPartialRequirements = true,
                intents = IntentCatalog.of(cardRegistry), polarityFromEffect = true,
            ) as CastSpell

            val host = (filled.targets.single() as ChosenTarget.Permanent).entityId
            withClue("every basic land type is more mana for the land's controller, so the land is ours") {
                game.state.projectedState.getController(host) shouldBe game.player1Id
            }
        }
    }
}
