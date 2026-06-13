package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * The Grey Havens — "{T}: Add one mana of any color among legendary creature cards in your
 * graveyard." Exercises the new ManaColorSet.AmongCardsInGraveyard via
 * Effects.AddManaOfColorAmongGraveyard. A mono-green legendary in the graveyard yields green.
 */
class TheGreyHavensScenarioTest : ScenarioTestBase() {

    private val graveyardManaAbilityId by lazy {
        cardRegistry.requireCard("The Grey Havens").activatedAbilities[1].id
    }

    init {
        test("adds mana of a color found among legendary creature cards in your graveyard") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "The Grey Havens")
                .withCardInGraveyard(1, "Fangorn, Tree Shepherd") // mono-green legendary creature
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val greyHavens = game.findPermanent("The Grey Havens")!!
            game.execute(ActivateAbility(game.player1Id, greyHavens, graveyardManaAbilityId)).error shouldBe null

            // The only color among legendary creature cards in the graveyard is green.
            game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.green shouldBe 1
        }
    }
}
