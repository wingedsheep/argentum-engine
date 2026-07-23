package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Kutzil, Malamet Exemplar (LCI #232) — {1}{G}{W} Legendary Creature — Cat Warrior (3/3).
 *
 * 1. "Your opponents can't cast spells during your turn." — the reusable
 *    [com.wingedsheep.sdk.scripting.PlayersCantCastSpells] static (EachOpponent, IsYourTurn), proven via
 *    the `reasonCannotCast` chokepoint (independent of mana availability).
 * 2. "Whenever one or more creatures you control each with power greater than its base power deals combat
 *    damage to a player, draw a card." — a `OneOrMoreDealCombatDamageToPlayerEvent` batch trigger whose
 *    sourceFilter is `Creature.powerGreaterThanBase()` (current projected power strictly above the
 *    creature's own printed base). Batch (CR 603.2c): one draw per combat-damage step regardless of how
 *    many qualifying creatures connected.
 */
class KutzilMalametExemplarScenarioTest : ScenarioTestBase() {

    private fun addPlusOneCounters(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to amount)))
        }
    }

    private fun resolveCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        game.resolveStack()
        if (game.state.pendingDecision != null) {
            game.submitDefaultCombatDamage()
            game.resolveStack()
        }
        // Resolve any combat-damage-triggered abilities (Kutzil's draw) that went on the stack.
        game.resolveStack()
    }

    init {
        context("Kutzil's cast restriction — opponents can't cast during your turn") {

            test("an opponent cannot cast during the controller's turn, but the controller can") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBolt = game.state.getZone(ZoneKey(game.player2Id, Zone.HAND)).first()
                val controllerBolt = game.state.getZone(ZoneKey(game.player1Id, Zone.HAND)).first()

                val utils = CastPermissionUtils(cardRegistry, PredicateEvaluator(), ConditionEvaluator())
                withClue("Opponent is locked out of casting during Kutzil's controller's turn") {
                    utils.reasonCannotCast(game.state, game.player2Id, opponentBolt) shouldNotBe null
                }
                withClue("The controller casts freely on their own turn — the lock only hits opponents") {
                    utils.reasonCannotCast(game.state, game.player1Id, controllerBolt) shouldBe null
                }
            }

            test("the opponent may cast on their own turn (your-turn-only)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardInHand(2, "Lightning Bolt")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val opponentBolt = game.state.getZone(ZoneKey(game.player2Id, Zone.HAND)).first()

                val utils = CastPermissionUtils(cardRegistry, PredicateEvaluator(), ConditionEvaluator())
                withClue("The cast lock is your-turn-only, so the opponent casts freely on their own turn") {
                    utils.reasonCannotCast(game.state, game.player2Id, opponentBolt) shouldBe null
                }
            }
        }

        context("Kutzil's combat-damage draw — power greater than base power") {

            test("a creature pumped above base by a +1/+1 counter deals combat damage → draw a card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // 2/2 Bear + a +1/+1 counter -> projected power 3 > base power 2, so it qualifies.
                addPlusOneCounters(game, game.findPermanent("Grizzly Bears")!!, 1)
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                resolveCombatDamage(game)

                withClue("Kutzil drew exactly one card for the pumped attacker") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("a creature pumped above base by an anthem deals combat damage → draw a card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Glorious Anthem")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                // Glorious Anthem: creatures you control get +1/+1 -> Bear is 3/3, power 3 > base 2.
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                resolveCombatDamage(game)

                withClue("Kutzil drew a card — the anthem raised the attacker above its base power") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("an unmodified attacker (power == base) does not trigger the draw") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val handBefore = game.handSize(1)

                // Only the unmodified 2/2 Bear attacks; Kutzil (also unmodified) stays back.
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                resolveCombatDamage(game)

                withClue("No creature had power above its base, so Kutzil did not draw") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("two qualifying creatures hitting the same player still draw only one card (batch)") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Kutzil, Malamet Exemplar", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                addPlusOneCounters(game, game.findPermanent("Grizzly Bears")!!, 1)
                addPlusOneCounters(game, game.findPermanent("Hill Giant")!!, 1)
                val handBefore = game.handSize(1)

                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
                resolveCombatDamage(game)

                withClue("Batch trigger (CR 603.2c): one draw for the whole combat-damage step, not one per creature") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }
        }
    }
}
