package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.NephaliaMoondrakes
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Nephalia Moondrakes (SOI #75) — {5}{U}{U} 5/5 Drake with flying.
 *
 *   When this creature enters, target creature gains flying until end of turn.
 *   {4}{U}{U}, Exile this card from your graveyard: Creatures you control gain flying until end of
 *   turn.
 */
class NephaliaMoondrakesScenarioTest : ScenarioTestBase() {

    private val abilityId = NephaliaMoondrakes.activatedAbilities.first().id

    init {
        test("entering gives target creature flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Nephalia Moondrakes")
                .withLandsOnBattlefield(1, "Island", 7)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
            game.castSpell(1, "Nephalia Moondrakes").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("exiled from the graveyard, it gives your creatures flying but not the opponent's") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Nephalia Moondrakes")
                .withLandsOnBattlefield(1, "Island", 6)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val moondrakes = game.findCardsInGraveyard(1, "Nephalia Moondrakes").single()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = moondrakes, abilityId = abilityId)
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Nephalia Moondrakes") shouldBe true
            val projected = game.state.projectedState
            projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.FLYING) shouldBe true
            projected.hasKeyword(game.findPermanent("Hill Giant")!!, Keyword.FLYING) shouldBe false
        }
    }
}
