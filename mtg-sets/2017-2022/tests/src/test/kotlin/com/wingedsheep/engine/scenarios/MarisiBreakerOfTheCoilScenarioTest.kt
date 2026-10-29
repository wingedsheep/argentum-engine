package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c19.cards.MarisiBreakerOfTheCoil
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Marisi, Breaker of the Coil (C19 #46):
 *   "Your opponents can't cast spells during combat.
 *    Whenever a creature you control deals combat damage to a player, goad each creature that
 *    player controls."
 *
 * The cast lock is [com.wingedsheep.sdk.scripting.PlayersCantCastSpells] gated on
 * `IsInPhase(COMBAT, yoursOnly = false)` — any player's combat phase, opponents only. The goad is
 * a group iteration over the *damaged* player's creatures, so a three-player table proves the
 * other opponent's creatures stay ungoaded.
 */
class MarisiBreakerOfTheCoilScenarioTest : ScenarioTestBase() {

    private fun boltInHand(game: TestGame, playerId: EntityId): EntityId =
        game.state.getZone(ZoneKey(playerId, Zone.HAND)).first()

    private fun castLockFor(activePlayer: Int, phase: Phase, step: Step): Pair<String?, String?> {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Marisi, Breaker of the Coil")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInHand(2, "Lightning Bolt")
            .withActivePlayer(activePlayer)
            .inPhase(phase, step)
            .build()
        val utils = CastPermissionUtils(cardRegistry, services.predicateEvaluator, services.conditionEvaluator)
        return utils.reasonCannotCast(game.state, game.player1Id, boltInHand(game, game.player1Id)) to
            utils.reasonCannotCast(game.state, game.player2Id, boltInHand(game, game.player2Id))
    }

    init {
        context("Your opponents can't cast spells during combat") {
            test("an opponent can't cast during the controller's combat; the controller can") {
                val (controller, opponent) = castLockFor(1, Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("the opponent is locked out during combat") { opponent shouldNotBe null }
                withClue("Marisi's controller isn't restricted") { controller shouldBe null }
            }

            test("the lock also applies during the opponent's own combat") {
                val (_, opponent) = castLockFor(2, Phase.COMBAT, Step.BEGIN_COMBAT)
                opponent shouldNotBe null
            }

            test("outside combat the opponent casts freely") {
                val (_, opponent) = castLockFor(1, Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                opponent shouldBe null
            }
        }

        context("Goad each creature that player controls") {
            test("goads every creature the damaged player controls, and none of another opponent's") {
                val d = GameTestDriver()
                d.registerCards(TestCards.all + MarisiBreakerOfTheCoil)
                val (p1, p2, p3) = d.initMultiplayer(List(3) { Deck.of("Mountain" to 40) })
                d.passPriorityUntil(Step.PRECOMBAT_MAIN)

                d.putCreatureOnBattlefield(p1, "Marisi, Breaker of the Coil")
                val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears").also { d.removeSummoningSickness(it) }
                val damagedA = d.putCreatureOnBattlefield(p2, "Centaur Courser")
                val damagedB = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
                val bystander = d.putCreatureOnBattlefield(p3, "Centaur Courser")

                d.passPriorityUntil(Step.DECLARE_ATTACKERS)
                d.declareAttackers(p1, listOf(bears), p2).error shouldBe null
                d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

                withClue("the Bears connected with p2") { d.getLifeTotal(p2) shouldBe 18 }
                withClue("each creature p2 controls is goaded by Marisi's controller") {
                    d.state.getEntity(damagedA)!!.get<GoadedComponent>()!!.goaderIds shouldBe setOf(p1)
                    d.state.getEntity(damagedB)!!.get<GoadedComponent>()!!.goaderIds shouldBe setOf(p1)
                }
                withClue("p3 wasn't dealt damage, so its creature isn't goaded") {
                    d.state.getEntity(bystander)!!.get<GoadedComponent>() shouldBe null
                }
            }
        }
    }
}
