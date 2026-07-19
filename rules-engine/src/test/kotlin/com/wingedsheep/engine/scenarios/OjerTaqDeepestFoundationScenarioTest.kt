package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.OjerTaqDeepestFoundation
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ojer Taq, Deepest Foundation // Temple of Civilization (LCI #26).
 *
 *  1. **Token tripling** — [com.wingedsheep.sdk.scripting.MultiplyTokenCreation]`(factor = 3)`:
 *     Dragon Fodder's two Goblin tokens become six under Ojer Taq's control.
 *  2. **Dies → return tapped + transformed** — the shared dies-trigger returns the God as its
 *     back face, Temple of Civilization, tapped.
 *  3. **Transform-back gate** — the land's `{2}{W}, {T}` transform is only activatable once you
 *     have attacked with three or more creatures this turn.
 */
class OjerTaqDeepestFoundationScenarioTest : ScenarioTestBase() {

    init {
        context("Ojer Taq, Deepest Foundation") {

            test("triples creature tokens created under your control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Taq, Deepest Foundation", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2) // {1}{R} for Dragon Fodder
                    .withCardInHand(1, "Dragon Fodder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Dragon Fodder").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("two Goblin tokens tripled to six") {
                    game.findPermanents("Goblin Token").size shouldBe 6
                }
            }

            test("when it dies it returns tapped as Temple of Civilization") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Taq, Deepest Foundation", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taq = game.findPermanent("Ojer Taq, Deepest Foundation")!!

                repeat(2) { // 6 damage kills the 6/6
                    game.castSpell(1, "Lightning Bolt", targetId = taq).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Temple of Civilization") == null && guard++ < 10) game.resolveStack()

                withClue("same entity returned as the back face, tapped") {
                    game.findPermanent("Temple of Civilization") shouldBe taq
                    game.state.getEntity(taq)!!.get<TappedComponent>() shouldNotBe null
                }
            }
        }

        context("Temple of Civilization — transform-back gate") {

            val transformAbilityId = OjerTaqDeepestFoundation.backFace!!
                .activatedAbilities.first { !it.isManaAbility }.id

            test("transforms back after attacking with three or more creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Civilization")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardOnBattlefield(1, "Craw Wurm", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3) // {2}{W} for the transform (Temple taps for the cost)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Civilization")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Grizzly Bears" to 2, "Hill Giant" to 2, "Craw Wurm" to 2)
                ).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the land flipped to its front face") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Ojer Taq, Deepest Foundation"
                }
            }

            test("cannot transform back without having attacked with three creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Civilization")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Civilization")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId)
                )

                withClue("activation is illegal without the attack condition") {
                    result.error shouldNotBe null
                }
                withClue("it stays a land") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Temple of Civilization"
                }
            }
        }
    }
}
