package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mom.cards.IchorDrinker
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ichor Drinker (MOM #111) — "{B}, Exile this card from your graveyard: Incubate 2. Activate only as
 * a sorcery."
 */
class IchorDrinkerScenarioTest : ScenarioTestBase() {

    private val ability = IchorDrinker.activatedAbilities[0].id

    init {
        context("Ichor Drinker") {
            test("exiling it from the graveyard incubates 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Ichor Drinker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drinker = game.findCardsInGraveyard(1, "Ichor Drinker").single()
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = drinker, abilityId = ability)
                ).error shouldBe null
                game.isInExile(1, "Ichor Drinker") shouldBe true
                game.resolveStack()

                val incubator = game.findPermanent("Incubator")
                incubator shouldNotBe null
                game.state.getEntity(incubator!!)?.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            }

            test("cannot be activated at instant speed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Ichor Drinker")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()

                val drinker = game.findCardsInGraveyard(1, "Ichor Drinker").single()
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = drinker, abilityId = ability)
                ).error shouldNotBe null
                game.isInGraveyard(1, "Ichor Drinker") shouldBe true
            }
        }
    }
}
