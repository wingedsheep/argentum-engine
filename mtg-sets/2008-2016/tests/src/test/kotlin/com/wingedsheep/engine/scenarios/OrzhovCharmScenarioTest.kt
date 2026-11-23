package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Orzhov Charm (GTC #185) — {W}{B} choose one: bounce your creature and the Auras you control on it;
 * destroy a creature and lose life equal to its toughness; or reanimate a mana value 1 or less creature.
 */
class OrzhovCharmScenarioTest : ScenarioTestBase() {
    init {
        test("first mode returns the creature and your Auras on it; an opponent's Aura goes to the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Orzhov Charm")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Claim of Erebos", "Grizzly Bears")
                .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val spell = game.findCardsInHand(1, "Orzhov Charm").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    chosenModes = listOf(0),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bears))),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Claim of Erebos") shouldBe true
            withClue("the opponent's Aura isn't returned; it falls off as a state-based action") {
                game.isInHand(2, "Pacifism") shouldBe false
                game.isInGraveyard(2, "Pacifism") shouldBe true
            }
        }

        test("second mode costs life equal to toughness even when the creature survives") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Orzhov Charm")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Darksteel Myr")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val myr = game.findPermanent("Darksteel Myr")!!
            val spell = game.findCardsInHand(1, "Orzhov Charm").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(myr))),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Darksteel Myr") shouldBe true
            game.getLifeTotal(1) shouldBe 19
        }

        test("second mode destroys the creature and you lose its toughness") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Orzhov Charm")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            val spell = game.findCardsInHand(1, "Orzhov Charm").single()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    spell,
                    chosenModes = listOf(1),
                    modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(giant))),
                )
            )
            withClue("cast should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
