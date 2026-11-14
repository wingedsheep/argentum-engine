package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.plc.cards.ProdigalPyromancer
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Touch of Moonglove (ORI #123).
 *
 * "Target creature you control gets +1/+0 and gains deathtouch until end of turn. Whenever a
 * creature dealt damage by that creature dies this turn, its controller loses 2 life."
 *
 * The rider fires once per creature the pumped creature damaged, even when the pumped creature dies
 * in the same combat; a creature something else killed doesn't count; the dying creature's
 * controller loses the life (2015-06-22 ruling), even when that is the caster; and once the pumped
 * creature leaves and comes back it is a new object, so its later damage doesn't count.
 */
class TouchOfMoongloveScenarioTest : ScenarioTestBase() {

    init {
        test("a blocker killed by the pumped creature costs its controller 2 life, even as they trade") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Touch of Moonglove")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Touch of Moonglove", game.findPermanent("Grizzly Bears")).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Centaur Courser" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Hill Giant" to listOf("Grizzly Bears"), "Raging Goblin" to listOf("Centaur Courser"))
            ).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            // Bears (3/2 deathtouch) and Hill Giant trade; the Goblin died to the Courser.
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isInGraveyard(2, "Raging Goblin") shouldBe true
            // One trigger, for the Giant only.
            game.getLifeTotal(2) shouldBe 18
            game.getLifeTotal(1) shouldBe 20
        }

        test("the dying creature's controller loses the life, even when that is the caster") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Touch of Moonglove")
                .withCardOnBattlefield(1, "Prodigal Pyromancer")
                .withCardOnBattlefield(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val pyromancer = game.findPermanent("Prodigal Pyromancer")!!
            game.castSpell(1, "Touch of Moonglove", pyromancer).error shouldBe null
            game.resolveStack()

            game.execute(
                ActivateAbility(
                    game.player1Id, pyromancer, ProdigalPyromancer.activatedAbilities.single().id,
                    listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!))
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 20
        }

        test("once the pumped creature has left and returned, its damage no longer counts") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Touch of Moonglove")
                .withCardInHand(1, "Unsummon")
                .withCardOnBattlefield(1, "Flametongue Kavu")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val kavu = game.findPermanent("Flametongue Kavu")!!
            game.castSpell(1, "Touch of Moonglove", kavu).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Unsummon", kavu).error shouldBe null
            game.resolveStack()
            game.isInHand(1, "Flametongue Kavu") shouldBe true

            // Recast: a new object whose enters trigger deals 4 damage to the Giant.
            game.castSpell(1, "Flametongue Kavu").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
