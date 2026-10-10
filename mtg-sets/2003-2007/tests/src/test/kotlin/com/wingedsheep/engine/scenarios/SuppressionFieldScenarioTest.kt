package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Suppression Field (RAV #31) — {1}{W} Enchantment.
 *
 *   Activated abilities cost {2} more to activate unless they're mana abilities.
 *
 * Two halves, and the second is what the card lives or dies on. The tax is the board-wide form of
 * Skyseer's Chariot's `IncreaseActivatedAbilityCost`, and — as there — it applies even to an
 * ability with no mana in its cost, so a bare `{T}:` becomes `{2}, {T}:`. The exemption is the new
 * `excludeManaAbilities` flag: without it a global tax would price every land's and every Signet's
 * mana ability at {2} more, which is the opposite of what the card says and would lock a player out
 * of their own mana.
 */
class SuppressionFieldScenarioTest : ScenarioTestBase() {

    private fun TestGame.actionFor(sourceId: EntityId) =
        getLegalActions(1).first { (it.action as? ActivateAbility)?.sourceId == sourceId }

    private fun TestGame.canActivate(sourceId: EntityId): Boolean =
        getLegalActions(1).any { (it.action as? ActivateAbility)?.sourceId == sourceId && it.isAffordable }

    private fun TestGame.manaPool(playerNumber: Int): ManaPoolComponent? =
        state.getEntity(if (playerNumber == 1) player1Id else player2Id)?.get<ManaPoolComponent>()

    init {
        context("Suppression Field — the {2} tax") {

            test("a bare {T} ability is taxed to {2}, {T} and priced out of reach") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                withClue("with no Suppression Field out, the free ping is activatable") {
                    game.canActivate(tim) shouldBe true
                }

                val taxed = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withCardOnBattlefield(1, "Suppression Field")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val taxedTim = taxed.findPermanent("Prodigal Sorcerer")!!
                withClue("a tax applies even where a reduction could not: {T} gains a mana part") {
                    taxed.actionFor(taxedTim).description shouldBe "{2}, {T}: Deal 1 damage to any target"
                }
                withClue("with no mana available the taxed ability is unaffordable") {
                    taxed.canActivate(taxedTim) shouldBe false
                }
            }

            test("the tax is exactly {2} — two untapped lands buy the ability back") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Prodigal Sorcerer")
                    .withCardOnBattlefield(1, "Suppression Field")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                game.canActivate(tim) shouldBe true
            }

            test("the tax reaches an opponent's abilities too — it names no controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Suppression Field")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tim = game.findPermanent("Prodigal Sorcerer")!!
                val action = game.getLegalActions(2)
                    .first { (it.action as? ActivateAbility)?.sourceId == tim }
                withClue("Suppression Field is symmetric — the opponent's ping is taxed identically") {
                    action.description shouldBe "{2}, {T}: Deal 1 damage to any target"
                    action.isAffordable shouldBe false
                }
            }
        }

        context("Suppression Field — mana abilities are exempt") {

            test("a Signet's {1}, {T} mana ability keeps its printed cost") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Suppression Field")
                    .withCardOnBattlefield(1, "Boros Signet")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val signet = game.findPermanent("Boros Signet")!!
                withClue("two Plains cover the printed {1}; a {3} tax would put it out of reach") {
                    game.canActivate(signet) shouldBe true
                }

                game.execute(game.actionFor(signet).action).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val pool = game.manaPool(1)
                withClue("the ability resolved and added {R}{W}") {
                    pool?.red shouldBe 1
                    pool?.white shouldBe 1
                }
            }

            test("a land's mana ability is untaxed, so a lone land still produces mana") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Suppression Field")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!
                withClue("a taxed {T}: Add {G} would need {2} the player does not have") {
                    game.canActivate(forest) shouldBe true
                }

                game.execute(game.actionFor(forest).action).error shouldBe null
                game.resolveStack()

                withClue("the land still adds its {G} at the printed cost") {
                    game.manaPool(1)?.green shouldBe 1
                }
            }
        }
    }
}
