package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.eld.cards.ResoluteRider
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Resolute Rider (ELD #214) — {W/B}{W/B}{W/B}{W/B} Creature — Human Knight 4/2.
 *
 * "{W/B}{W/B}: This creature gains lifelink until end of turn.
 *  {W/B}{W/B}{W/B}: This creature gains indestructible until end of turn."
 */
class ResoluteRiderScenarioTest : ScenarioTestBase() {

    init {
        val lifelinkAbility = ResoluteRider.activatedAbilities[0].id
        val indestructibleAbility = ResoluteRider.activatedAbilities[1].id

        context("Resolute Rider") {

            test("{W/B}{W/B} paid with Plains grants lifelink; combat damage gains life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Resolute Rider")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rider = game.findPermanent("Resolute Rider")!!
                game.state.projectedState.hasKeyword(rider, Keyword.LIFELINK) shouldBe false

                game.execute(ActivateAbility(game.player1Id, rider, lifelinkAbility)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.hasKeyword(rider, Keyword.LIFELINK) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Resolute Rider" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers().error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("4 lifelink damage: opponent to 16, controller to 24") {
                    game.getLifeTotal(2) shouldBe 16
                    game.getLifeTotal(1) shouldBe 24
                }
            }

            test("{W/B}{W/B}{W/B} paid with Swamps grants indestructible; Murder doesn't destroy it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Resolute Rider")
                    .withCardInHand(1, "Murder")
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rider = game.findPermanent("Resolute Rider")!!
                game.execute(ActivateAbility(game.player1Id, rider, indestructibleAbility)).error shouldBe null
                game.resolveStack()
                game.state.projectedState.hasKeyword(rider, Keyword.INDESTRUCTIBLE) shouldBe true
                game.state.projectedState.hasKeyword(rider, Keyword.LIFELINK) shouldBe false

                game.castSpell(1, "Murder", rider).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Resolute Rider") shouldBe true
            }
        }
    }
}
