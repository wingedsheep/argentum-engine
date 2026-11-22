package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.FlashGrant
import com.wingedsheep.engine.state.components.player.FlashGrantsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * A flash grant to its own caster — "you may cast spells this turn as though they had flash" (Borne
 * Upon a Wind), "the next sorcery spell you cast this turn can be cast as though it had flash"
 * (Quicken) — is spent in the window it is cast in: the AI, holding a live grant at the opponent's
 * end step, casts the sorcery it makes castable there. That is the timing reading the audit credits.
 */
class FlashGrantSpendAiTest : ScenarioTestBase() {

    init {
        test("with a one-shot sorcery flash grant at the opponent's end step, the AI casts its sorcery") {
            val game = scenario().withPlayers()
                .withTurnNumber(6)
                .withActivePlayer(2)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInHand(1, "Divination")
                .apply { repeat(5) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()
                .advanceToPriority(1, Step.END)
            game.state = game.state.updateEntity(game.player1Id) {
                it.with(FlashGrantsComponent(listOf(FlashGrant(GameObjectFilter.Sorcery, nextSpellOnly = true))))
            }

            val action = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).chooseAction(game.state)
            val cast = (action as? CastSpell)?.let { game.state.getEntity(it.cardId)?.get<CardComponent>()?.name }
            cast shouldBe "Divination"
        }

        test("the audit reads the flash grant's timing on Borne Upon a Wind, Complete the Circuit and Quicken") {
            for (name in listOf("Borne Upon a Wind", "Complete the Circuit", "Quicken")) {
                val reading = AiReadingAudit.read(cardRegistry.requireCard(name))
                withClue("$name: $reading") { reading.flags.none { it.key.startsWith("timing") } shouldBe true }
            }
        }
    }
}
