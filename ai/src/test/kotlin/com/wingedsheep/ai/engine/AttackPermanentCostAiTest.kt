package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * `CantAttackUnlessPermanentCost` — a creature that can't attack unless its controller sacrifices
 * (Leviathan) or returns to hand (Floodtide Serpent) permanents they control. The AI reads it as
 * the body's own combat restriction, and once the attack is declared pays as cheaply as it can. A
 * bounce keeps the card, so it costs only the mana to recast it: the cheapest goes back. The bounce
 * prompt used to fall through to "pick the best" and returned the 7-mana Warstorm Surge.
 */
class AttackPermanentCostAiTest : ScenarioTestBase() {

    init {
        test("an attack paid with permanents reads as the creature's own combat restriction") {
            listOf("Floodtide Serpent", "Leviathan").forEach { name ->
                withClue(name) {
                    CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!).tags shouldContain IntentTag.COMBAT_RESTRICTION
                }
            }
        }

        test("the AI returns its cheapest enchantment to recast to pay Floodtide Serpent's attack") {
            val game = scenario()
                .withPlayers("Tide", "Shore")
                .withCardOnBattlefield(1, "Floodtide Serpent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Glorious Anthem")
                .withCardOnBattlefield(1, "Warstorm Surge")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Floodtide Serpent" to 2)).error shouldBe null
            val decision = game.state.pendingDecision
            decision.shouldNotBeNull()
            decision.shouldBeInstanceOf<SelectCardsDecision>()

            val ai = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE)
            val response = ai.respondToDecision(game.state, decision).shouldBeInstanceOf<CardsSelectedResponse>()
            val names = response.selectedCards.map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            withClue("a bounce costs only the recast: return the 3-mana Anthem, not the 7-mana Surge") {
                names shouldBe listOf("Glorious Anthem")
            }
        }
    }
}
