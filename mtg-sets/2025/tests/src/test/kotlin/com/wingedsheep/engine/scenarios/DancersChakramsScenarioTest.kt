package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dancer's Chakrams (FIC #17) — the equipped creature has "Other commanders you control get +2/+2
 * and have lifelink." Pins the granted lord static: it reads "other" and "you control" relative to
 * the equipped creature, touches only commanders, and only its holder's controller's ones.
 */
class DancersChakramsScenarioTest : ScenarioTestBase() {

    private fun castChakrams(): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Dancer's Chakrams")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Savannah Lions")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        val giant = game.findPermanent("Hill Giant")!!
        game.state = game.state
            .updateEntity(bears) { it.with(CommanderComponent(ownerId = game.player1Id)) }
            .updateEntity(giant) { it.with(CommanderComponent(ownerId = game.player2Id)) }

        val cast = game.castSpell(1, "Dancer's Chakrams")
        withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
        if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
        game.resolveStack()
        return game
    }

    init {
        test("the equipped Hero is a 3/3 lifelink Performer") {
            val game = castChakrams()
            val hero = game.findPermanent("Hero Token")!!
            val projected = game.state.projectedState
            projected.getPower(hero) shouldBe 3
            projected.getToughness(hero) shouldBe 3
            projected.hasKeyword(hero, Keyword.LIFELINK) shouldBe true
            projected.hasSubtype(hero, "Performer") shouldBe true
            projected.hasSubtype(hero, "Hero") shouldBe true
        }

        test("your other commander gets +2/+2 and lifelink; non-commanders and opponents' commanders don't") {
            val game = castChakrams()
            val projected = game.state.projectedState

            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("your commander") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
                projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            }

            val lions = game.findPermanent("Savannah Lions")!!
            withClue("a creature that isn't a commander") {
                // The test registry's Savannah Lions is a 1/1 stub.
                projected.getPower(lions) shouldBe 1
                projected.getToughness(lions) shouldBe 1
                projected.hasKeyword(lions, Keyword.LIFELINK) shouldBe false
            }

            val giant = game.findPermanent("Hill Giant")!!
            withClue("an opponent's commander") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
                projected.hasKeyword(giant, Keyword.LIFELINK) shouldBe false
            }
        }

        test("an equipped commander doesn't pump itself with the granted ability") {
            val game = castChakrams()
            val hero = game.findPermanent("Hero Token")!!
            game.state = game.state.updateEntity(hero) { it.with(CommanderComponent(ownerId = game.player1Id)) }

            val projected = game.state.projectedState
            projected.getPower(hero) shouldBe 3
            projected.getToughness(hero) shouldBe 3
        }
    }
}
