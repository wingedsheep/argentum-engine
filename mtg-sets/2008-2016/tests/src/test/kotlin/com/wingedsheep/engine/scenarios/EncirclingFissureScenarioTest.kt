package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Encircling Fissure — "Prevent all combat damage that would be dealt this turn by creatures
 * target opponent controls." Awaken 2—{4}{W}.
 *
 * Pins that the targeted opponent's creatures deal no combat damage (including one that entered
 * after the spell resolved, per the card's ruling), that the caster's creatures still do, and the
 * awakened cast — built by hand because the awaken land target follows a player target.
 */
class EncirclingFissureScenarioTest : ScenarioTestBase() {

    init {
        test("the targeted opponent's attackers deal no combat damage, even one cast after the spell resolved") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Encircling Fissure")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardInHand(2, "Raging Goblin")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // The opponent passes in their main phase; the player responds with the instant.
            game.execute(PassPriority(game.player2Id))
            game.castSpellTargetingPlayer(1, "Encircling Fissure", 2).error shouldBe null
            game.resolveStack()

            // A haste creature the opponent controls only after the spell resolved is covered too.
            game.castSpell(2, "Raging Goblin").error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 1, "Raging Goblin" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("neither the 3/3 nor the later-cast 1/1 dealt combat damage") {
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("the caster's creatures still deal combat damage; the opponent's blocker deals none") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Encircling Fissure")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Encircling Fissure", 2).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2, "Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Centaur Courser" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the unblocked Hill Giant's 3 combat damage is dealt") {
                game.getLifeTotal(2) shouldBe 17
            }
            withClue("the blocking Centaur Courser's damage to the Grizzly Bears is prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        test("cast for its awaken cost, a land becomes a 2/2 hasty Elemental that survives an opposing blocker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Encircling Fissure")
                .withLandsOnBattlefield(1, "Plains", 5)
                // Maze of Ith has no mana ability, so paying {4}{W} with the Plains leaves it untapped.
                .withCardOnBattlefield(1, "Maze of Ith")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val land = game.findPermanent("Maze of Ith")!!
            val fissure = game.state.getHand(game.player1Id).single()
            game.execute(
                CastSpell(
                    game.player1Id, fissure,
                    targets = listOf(ChosenTarget.Player(game.player2Id), ChosenTarget.Permanent(land)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.AWAKEN
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(land) shouldBe true
            projected.hasSubtype(land, "Elemental") shouldBe true
            projected.hasType(land, "LAND") shouldBe true
            projected.hasKeyword(land, Keyword.HASTE) shouldBe true
            projected.getPower(land) shouldBe 2
            projected.getToughness(land) shouldBe 2
            game.state.getEntity(land)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2

            val courser = game.findPermanent("Centaur Courser")!!
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(land to game.player2Id))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.execute(DeclareBlockers(game.player2Id, mapOf(courser to listOf(land)))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the 3/3 blocker's combat damage to the awakened land is prevented") {
                game.state.getBattlefield().contains(land) shouldBe true
            }
            withClue("the awakened land's own 2 combat damage to the blocker is dealt") {
                game.isOnBattlefield("Centaur Courser") shouldBe true
                game.state.getEntity(courser)?.get<DamageComponent>()?.amount shouldBe 2
            }
        }
    }
}
