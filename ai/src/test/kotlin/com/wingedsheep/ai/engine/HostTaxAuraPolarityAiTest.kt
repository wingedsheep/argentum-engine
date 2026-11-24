package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
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
import io.kotest.matchers.shouldBe

/**
 * An Aura that taxes its host every upkeep — "at the beginning of the upkeep of enchanted
 * permanent's controller, that player sacrifices it unless they pay {X}" (Soul Tithe, a decided
 * `MayPay` gate with an empty branch), "destroy that land unless that player pays {1} or 1 life"
 * (Erosion, a `PayOrSuffer`) — is a curse on whoever's permanent it enchants: they pay forever or
 * lose it. The enchant slot reads [TargetPolarity.HARMFUL] and the trigger reads as removal.
 *
 * The decision it changes: with only our own creature to enchant, Soul Tithe would tax our own
 * board every upkeep; the AI no longer aims it there.
 */
class HostTaxAuraPolarityAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.requireCard(name)

    init {
        test("a recurring sacrifice-unless-pay on the host curses it, and is removal") {
            for (name in listOf("Soul Tithe", "Erosion")) {
                withClue(name) {
                    TargetPolarityAnalyzer.forSpell(card(name)) shouldBe listOf(TargetPolarity.HARMFUL)
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.REMOVAL
                    AiReadingAudit.read(card(name)).readFully shouldBe true
                }
            }
        }

        test("Soul Tithe is not cast on our own creature") {
            val game = scenario()
                .withPlayers()
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Soul Tithe")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val action = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id).first { action ->
                val spell = action.action as? CastSpell ?: return@first false
                game.state.getEntity(spell.cardId)?.get<CardComponent>()?.name == "Soul Tithe"
            }

            withClue("our Hill Giant is the only nonland permanent to enchant") {
                TargetSelection.aimsOnlyAtTheWrongSide(game.state, action, game.player1Id, IntentCatalog.of(cardRegistry)) shouldBe true
            }
        }
    }
}
