package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.NearheathChaplain
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Nearheath Chaplain (SOI #29) — {3}{W} 3/1 Human Cleric with lifelink.
 *
 *   {2}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens with
 *   flying. Activate only as a sorcery.
 */
class NearheathChaplainScenarioTest : ScenarioTestBase() {

    private val abilityId = NearheathChaplain.activatedAbilities.first().id

    init {
        test("from the graveyard, it exiles itself to make two 1/1 flying Spirits") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Nearheath Chaplain")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val chaplain = game.findCardsInGraveyard(1, "Nearheath Chaplain").single()
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = chaplain, abilityId = abilityId)
            ).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Nearheath Chaplain") shouldBe true
            val spirits = game.findAllPermanents("Spirit Token")
            spirits shouldHaveSize 2
            spirits.forEach { spirit ->
                game.state.projectedState.getPower(spirit) shouldBe 1
                game.state.projectedState.getToughness(spirit) shouldBe 1
                game.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
            }
        }
    }
}
