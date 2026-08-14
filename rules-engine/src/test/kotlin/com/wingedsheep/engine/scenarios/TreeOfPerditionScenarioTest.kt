package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.emn.cards.TreeOfPerdition
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tree of Perdition (EMN #109) — {3}{B} Creature — Plant, 0/13.
 *
 * "Defender
 *  {T}: Exchange target opponent's life total with this creature's toughness."
 *
 * These pin the toughness variant of `ExchangeLifeAndStatEffect` (CR 701.12g), which the power
 * variant (Evra, Halcyon Witness) doesn't cover:
 *  1. the plain exchange — opponent drops to 13, the Tree's toughness becomes their former life;
 *  2. the Cultist's Staff ruling — the opponent receives the Tree's *projected* toughness, while
 *     the Tree's **base** toughness is what gets set, so an attached +1/+2 still applies on top;
 *  3. the Tree leaving the battlefield before resolution makes the whole ability do nothing.
 */
class TreeOfPerditionScenarioTest : ScenarioTestBase() {

    init {
        val exchangeAbilityId = TreeOfPerdition.activatedAbilities.first().id

        context("Tree of Perdition") {

            test("exchanges the targeted opponent's life total with the Tree's toughness") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tree of Perdition")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tree = game.findPermanent("Tree of Perdition")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tree,
                        abilityId = exchangeAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("opponent's life becomes the Tree's former toughness") {
                    game.getLifeTotal(2) shouldBe 13
                }
                withClue("the Tree's toughness becomes the opponent's former life total") {
                    game.state.projectedState.getToughness(tree) shouldBe 20
                }
            }

            test("an attached +1/+2 applies on top of the newly set base toughness") {
                // Holy Strength stands in for the ruling's Cultist's Staff: the Tree is a 1/15 while
                // the opponent is on 7. After the exchange the opponent is on 15 (the *projected*
                // toughness) and the Tree is a 1/9 (base toughness 7, plus Holy Strength's +2).
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tree of Perdition")
                    .withCardAttachedTo(1, "Holy Strength", "Tree of Perdition")
                    .withLifeTotal(2, 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tree = game.findPermanent("Tree of Perdition")!!
                withClue("Holy Strength makes the Tree a 1/15 before the exchange") {
                    game.state.projectedState.getToughness(tree) shouldBe 15
                }

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tree,
                        abilityId = exchangeAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("opponent receives the projected toughness, not the base 13") {
                    game.getLifeTotal(2) shouldBe 15
                }
                withClue("base toughness set to 7, then Holy Strength's +2 applies on top") {
                    game.state.projectedState.getToughness(tree) shouldBe 9
                }
            }

            test("no exchange happens if the Tree has left the battlefield before resolution") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tree of Perdition")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tree = game.findPermanent("Tree of Perdition")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = tree,
                        abilityId = exchangeAbilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null

                // The Tree dies while its ability is still on the stack. Murder resolves first, so by
                // the time the exchange resolves its source is gone.
                game.castSpell(1, "Murder", tree).error shouldBe null
                game.resolveStack()

                withClue("the Tree is gone, so the exchange doesn't happen at all") {
                    game.isOnBattlefield("Tree of Perdition") shouldBe false
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
