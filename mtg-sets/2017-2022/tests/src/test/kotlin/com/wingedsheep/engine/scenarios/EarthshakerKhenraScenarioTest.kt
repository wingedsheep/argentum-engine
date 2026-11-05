package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.EarthshakerKhenra
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Earthshaker Khenra (HOU #90) — {1}{R} Creature — Jackal Warrior 2/1.
 *
 *   Haste
 *   When this creature enters, target creature with power less than or equal to this creature's
 *   power can't block this turn.
 *   Eternalize {4}{R}{R}
 */
class EarthshakerKhenraScenarioTest : ScenarioTestBase() {

    private val name = "Earthshaker Khenra"
    private val eternalizeId = EarthshakerKhenra.activatedAbilities.single().id

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Centaur Courser")
            .withCardOnBattlefield(2, "Force of Nature")

        test("the 2/1 can target only power 2 or less, and the target can't block its hasty attack") {
            val game = board()
                .withCardInHand(1, name)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val courser = game.findPermanent("Centaur Courser")!!

            game.castSpell(1, name).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].shouldNotBeNull()
            withClue("Grizzly Bears (2) is within the 2-power cap; Centaur Courser (3) is not") {
                legal shouldContain bears
                legal shouldNotContain courser
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            withClue("haste lets the Khenra attack the turn it enters") {
                game.declareAttackers(mapOf(name to 2)).error shouldBe null
            }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Grizzly Bears can't block this turn") {
                game.declareBlockers(mapOf("Grizzly Bears" to listOf(name))).error shouldNotBe null
            }
        }

        test("the eternalized 4/4 token's trigger reaches power 4 but not power 5") {
            val game = board()
                .withCardInGraveyard(1, name)
                .withLandsOnBattlefield(1, "Mountain", 6)
                .build()
            val courser = game.findPermanent("Centaur Courser")!!
            val force = game.findPermanent("Force of Nature")!!
            val card = game.findCardsInGraveyard(1, name).single()

            game.execute(ActivateAbility(game.player1Id, card, eternalizeId)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            val legal = decision.legalTargets[0].shouldNotBeNull()
            withClue("the token's power is 4: Centaur Courser (3) is legal, Force of Nature (5) is not") {
                legal shouldContain courser
                legal shouldNotContain force
            }
            game.selectTargets(listOf(courser)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf(name to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("Centaur Courser can't block this turn") {
                game.declareBlockers(mapOf("Centaur Courser" to listOf(name))).error shouldNotBe null
            }
        }
    }
}
