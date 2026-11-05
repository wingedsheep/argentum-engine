package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * The keywords a [com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect] hands out that the
 * readers had no entry for: protection from a colour (`PROTECTION_FROM_<COLOR>`), the -1/-1
 * keywords wither and infect, the come-back keywords persist and undying, myriad, reach and
 * changeling. Each is a gift to the creature that gains it, so its target slot is
 * [TargetPolarity.BENEFICIAL], and each says what it is for: a fight won (combat enhancement) or a
 * creature kept (protection).
 *
 * Unread, Crimson Acolyte's "{W}: target creature gains protection from red until end of turn" slot
 * fell back to the removal ranking and handed the protection to the opponent's creature.
 */
class KeywordGrantReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun TestGame.permanent(playerId: EntityId, name: String): EntityId =
        state.getBattlefield(playerId).single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun triggerTags(card: String) =
        cardRegistry.requireCard(card).script.triggeredAbilities
            .first { it.targetRequirement != null }
            .let { intents.forEffect(it.effect).tags }

    init {
        test("a granted keyword's target reads as a gift") {
            val acolyte = cardRegistry.requireCard("Crimson Acolyte")
            withClue("Crimson Acolyte: protection from red is a gift to the creature that gains it") {
                intents.abilityTargetPolarities(acolyte.name, acolyte.script.activatedAbilities.single().id) shouldBe
                    listOf(TargetPolarity.BENEFICIAL)
            }
            for (card in listOf("Rhys, the Evermore", "Barbed Bloodletter")) {
                val trigger = cardRegistry.requireCard(card).script.triggeredAbilities.first { it.targetRequirement != null }
                withClue("$card: persist / wither on our creature is ours to aim") {
                    intents.effectTargetPolarities(trigger.effect, listOf(trigger.targetRequirement!!), card) shouldBe
                        listOf(TargetPolarity.BENEFICIAL)
                }
            }
            val oko = cardRegistry.requireCard("Oko, Lorwyn Liege")
            val plusTwo = oko.script.activatedAbilities.first { it.targetRequirement != null }
            withClue("Oko's +2: gaining all creature types never hurts the creature") {
                intents.abilityTargetPolarities(oko.name, plusTwo.id) shouldBe listOf(TargetPolarity.BENEFICIAL)
            }
        }

        test("a granted keyword says what it is for") {
            withClue("persist brings the creature back: protection") {
                triggerTags("Rhys, the Evermore") shouldContain IntentTag.PROTECTION
            }
            withClue("wither wins the fight it is in: combat enhancement") {
                triggerTags("Barbed Bloodletter") shouldContain IntentTag.COMBAT_ENHANCEMENT
            }
        }

        test("a 'when that creature dies this turn' rider does not hide a keyword trick's timing") {
            withClue("Scarblade's Malice: deathtouch and lifelink until end of turn, plus a token if it dies this turn") {
                ExpiringGrantWindow.readsSpellKeywordGrant(cardRegistry.requireCard("Scarblade's Malice").script.spellEffect!!) shouldBe true
            }
        }

        test("gives protection from red to our Serra Angel, not the opponent's Hill Giant") {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(1)
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardOnBattlefield(1, "Crimson Acolyte")
                .withCardOnBattlefield(1, "Serra Angel")
                .withCardOnBattlefield(2, "Hill Giant")
                .build()
                .advanceToPriority(1, Step.PRECOMBAT_MAIN)
            val activate = GameSimulator(cardRegistry).getLegalActions(game.state, game.player1Id)
                .first { (it.action as? ActivateAbility)?.sourceId == game.permanent(game.player1Id, "Crimson Acolyte") }

            val filled = TargetSelection.fillHeuristically(
                game.state, activate, game.player1Id, fillPartialRequirements = true,
                intents = intents, polarityFromEffect = true,
            ) as ActivateAbility

            withClue("protection from red reads as a gift, so it goes on our best creature") {
                (filled.targets.single() as ChosenTarget.Permanent).entityId shouldBe
                    game.permanent(game.player1Id, "Serra Angel")
            }
        }
    }
}
