package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Void Winnower — "Your opponents can't cast spells with even mana values. (Zero is even.) Your
 * opponents can't block with creatures with even mana values."
 *
 * Rulings pinned here: an X spell counts the X chosen for it; an {X} permanent counts X as 0; a
 * face-down spell or creature has mana value 0, so it can't be cast face down or block.
 */
class VoidWinnowerScenarioTest : ScenarioTestBase() {
    init {
        fun castGame(vararg hand: String) = scenario()
            .withPlayers()
            .withCardOnBattlefield(1, "Void Winnower")
            .withLandsOnBattlefield(2, "Plains", 7)
            .apply { hand.forEach { withCardInHand(2, it) } }
            .withCardInHand(1, "Grizzly Bears")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.handCard(player: EntityId, name: String): EntityId = state.getHand(player).first {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

        test("an opponent can't cast an even-mana-value spell but can cast an odd one") {
            val game = castGame("Glory Seeker", "Dawning Purist")
            // Glory Seeker {1}{W}: mana value 2.
            game.castSpell(2, "Glory Seeker").error.shouldNotBeNull()
            // Dawning Purist {2}{W}: mana value 3.
            game.castSpell(2, "Dawning Purist").error.shouldBeNull()
        }

        test("an X spell counts the X chosen for it") {
            val game = castGame("Endless One")
            game.castXSpell(2, "Endless One", xValue = 6).error.shouldNotBeNull()
            game.castXSpell(2, "Endless One", xValue = 5).error.shouldBeNull()
        }

        test("a face-down spell has mana value 0, so it can't be cast face down") {
            val game = castGame("Dawning Purist")
            val purist = game.handCard(game.player2Id, "Dawning Purist")
            game.execute(CastSpell(game.player2Id, purist, castFaceDown = true)).error.shouldNotBeNull()
        }

        test("Void Winnower's controller isn't restricted") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Void Winnower")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardInHand(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Grizzly Bears").error.shouldBeNull()
        }

        fun blockGame(vararg blockers: String): TestGame {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Void Winnower")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .apply { blockers.forEach { withCardOnBattlefield(2, it) } }
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            return game
        }

        fun TestGame.attack() {
            declareAttackers(mapOf("Hill Giant" to 2)).error.shouldBeNull()
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }

        fun TestGame.block(blocker: String) = declareBlockers(mapOf(blocker to listOf("Hill Giant"))).error

        test("an opponent's even-mana-value creature can't block; an odd one can") {
            blockGame("Glory Seeker").apply { attack() }.block("Glory Seeker").shouldNotBeNull()
            blockGame("Centaur Courser").apply { attack() }.block("Centaur Courser").shouldBeNull()
        }

        test("a face-down creature has mana value 0 and can't block") {
            val game = blockGame("Centaur Courser")
            val courser = game.findPermanent("Centaur Courser")!!
            game.state = game.state.updateEntity(courser) { it.with(FaceDownComponent) }
            game.attack()
            game.block("Centaur Courser").shouldNotBeNull()
        }

        test("an {X} creature on the battlefield counts X as 0") {
            val game = blockGame("Endless One", "Mistcutter Hydra")
            listOf("Endless One", "Mistcutter Hydra").forEach { name ->
                val id = game.findPermanent(name)!!
                game.state = game.state.updateEntity(id) {
                    it.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 3))
                }
            }
            game.attack()
            // Endless One {X}: mana value 0 — even. Mistcutter Hydra {X}{G}: mana value 1 — odd.
            game.block("Endless One").shouldNotBeNull()
            game.block("Mistcutter Hydra").shouldBeNull()
        }

        test("Void Winnower's controller can block with even-mana-value creatures") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Void Winnower")
                .withCardOnBattlefield(1, "Glory Seeker")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            game.declareAttackers(mapOf("Hill Giant" to 1)).error.shouldBeNull()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Glory Seeker" to listOf("Hill Giant"))).error.shouldBeNull()
        }
    }
}
