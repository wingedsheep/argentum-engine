package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.afr.cards.WerewolfPackLeader
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Werewolf Pack Leader (AFR #211) — {G}{G} Creature — Human Werewolf 3/3.
 *
 *   Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power
 *   6 or greater this combat, draw a card.
 *   {3}{G}: Until end of turn, this creature has base power and toughness 5/3, gains trample, and
 *   isn't a Human.
 */
class WerewolfPackLeaderScenarioTest : ScenarioTestBase() {

    private val transform = WerewolfPackLeader.activatedAbilities.single()

    init {
        fun board() = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Werewolf Pack Leader", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withLandsOnBattlefield(1, "Forest", 4)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun ScenarioTestBase.TestGame.activate() {
            val leader = findPermanent("Werewolf Pack Leader")!!
            execute(ActivateAbility(player1Id, leader, transform.id)).error shouldBe null
            resolveStack()
        }

        test("the ability makes it a 5/3 trampling Werewolf that isn't a Human") {
            val game = board()
            game.activate()
            val leader = game.findPermanent("Werewolf Pack Leader")!!

            val projected = game.state.projectedState
            projected.getPower(leader) shouldBe 5
            projected.getToughness(leader) shouldBe 3
            projected.hasKeyword(leader, Keyword.TRAMPLE) shouldBe true
            projected.isCreature(leader) shouldBe true
            withClue("it isn't a Human") { projected.hasSubtype(leader, "Human") shouldBe false }
            withClue("it is still a Werewolf") { projected.hasSubtype(leader, "Werewolf") shouldBe true }
        }

        test("the effect ends at end of turn") {
            val game = board()
            game.activate()
            val leader = game.findPermanent("Werewolf Pack Leader")!!

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

            val projected = game.state.projectedState
            projected.getPower(leader) shouldBe 3
            projected.getToughness(leader) shouldBe 3
            projected.hasKeyword(leader, Keyword.TRAMPLE) shouldBe false
            projected.hasSubtype(leader, "Human") shouldBe true
        }

        test("attacking with total power 5 draws nothing") {
            val game = board()
            val handBefore = game.handSize(1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Werewolf Pack Leader" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
        }

        test("activating before combat brings the pack to 7 power and draws a card") {
            val game = board()
            game.activate()
            val handBefore = game.handSize(1)

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Werewolf Pack Leader" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }
    }
}
