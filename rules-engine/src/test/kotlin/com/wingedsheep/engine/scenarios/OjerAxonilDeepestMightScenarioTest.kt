package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.lci.cards.OjerAxonilDeepestMight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ojer Axonil, Deepest Might // Temple of Power (LCI #158).
 *
 *  1. **Damage floor** — [com.wingedsheep.sdk.scripting.SetMinimumDamage] raises noncombat damage
 *     from a red source you control to an *opponent* up to Ojer Axonil's power; it leaves damage to
 *     an opponent's creature (recipient is a player) untouched.
 *  2. **Dies → return tapped + transformed** — the shared dies-trigger returns Temple of Power.
 *  3. **Transform-back gate** — Temple of Power's transform needs red sources you controlled to
 *     have dealt 4+ noncombat damage this turn (the RED_NONCOMBAT_DAMAGE_DEALT tracker).
 */
class OjerAxonilDeepestMightScenarioTest : ScenarioTestBase() {

    init {
        fun castBolt(game: TestGame, targetPlayer: Int) {
            game.castSpellTargetingPlayer(1, "Lightning Bolt", targetPlayer).error shouldBe null
            if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
            game.resolveStack()
        }

        context("Ojer Axonil, Deepest Might") {

            test("floors a red source's noncombat damage to an opponent up to Axonil's power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Axonil, Deepest Might", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                castBolt(game, 2)

                withClue("Bolt's 3 damage floored to Axonil's power (4): 20 - 4 = 16") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }

            test("does not floor damage to an opponent's creature (recipient is a player only)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Axonil, Deepest Might", summoningSickness = false)
                    .withCardOnBattlefield(2, "Craw Wurm") // 6/4
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wurm = game.findPermanent("Craw Wurm")!!
                game.castSpell(1, "Lightning Bolt", targetId = wurm).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("Bolt dealt only 3 (not floored to 4), so the 4-toughness Wurm survives") {
                    game.findPermanent("Craw Wurm").shouldNotBeNull()
                }
            }

            test("when it dies it returns tapped as Temple of Power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ojer Axonil, Deepest Might", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val axonil = game.findPermanent("Ojer Axonil, Deepest Might")!!

                repeat(2) { // 6 damage (bolts to my own creature aren't floored) kills the 4/4
                    game.castSpell(1, "Lightning Bolt", targetId = axonil).error shouldBe null
                    if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                    game.resolveStack()
                }
                var guard = 0
                while (game.findPermanent("Temple of Power") == null && guard++ < 10) game.resolveStack()

                withClue("same entity returned as the back face, tapped") {
                    game.findPermanent("Temple of Power") shouldBe axonil
                    game.state.getEntity(axonil)!!.get<TappedComponent>() shouldNotBe null
                }
            }
        }

        context("Temple of Power — transform-back gate") {

            val transformAbilityId = OjerAxonilDeepestMight.backFace!!
                .activatedAbilities.first { !it.isManaAbility }.id

            test("transforms back after red sources dealt 4+ noncombat damage this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Power")
                    .withLandsOnBattlefield(1, "Mountain", 5) // 2 Bolts ({R}{R}) + transform ({2}{R})
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Power")!!

                castBolt(game, 2) // 3 red noncombat damage
                castBolt(game, 2) // 6 total, over the threshold

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId))
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the land flipped to its front face") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Ojer Axonil, Deepest Might"
                }
            }

            test("cannot transform back without 4 noncombat damage from red sources") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Temple of Power")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val temple = game.findPermanent("Temple of Power")!!
                val result = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = temple, abilityId = transformAbilityId)
                )

                withClue("activation is illegal without the red-damage condition") {
                    result.error shouldNotBe null
                }
                withClue("it stays a land") {
                    game.state.getEntity(temple)!!.get<CardComponent>()!!.name shouldBe "Temple of Power"
                }
            }
        }
    }
}
