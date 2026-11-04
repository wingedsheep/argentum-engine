package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.PlayerCitysBlessingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rix.cards.OrazcaRelic
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Orazca Relic (RIX #181) — "Ascend. {T}: Add {C}. {T}, Sacrifice this artifact: You gain 3 life
 * and draw a card. Activate only if you have the city's blessing."
 *
 * The first activated ability gated on the city's blessing: the sacrifice ability is refused at
 * nine permanents and becomes legal once the tenth (a land drop) grants the blessing through the
 * Relic's own ascend — on a noncreature artifact.
 */
class OrazcaRelicScenarioTest : ScenarioTestBase() {

    private val sacrificeAbility = OrazcaRelic.activatedAbilities[1].id

    init {
        test("the sacrifice ability needs the city's blessing, granted by the Relic's ascend") {
            // Relic + 8 Plains = 9 permanents; the Plains in hand makes ten.
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Orazca Relic")
                .withLandsOnBattlefield(1, "Plains", 8)
                .withCardInHand(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val relic = game.findPermanent("Orazca Relic").shouldNotBeNull()

            withClue("nine permanents: no blessing, so the ability can't be activated") {
                game.execute(ActivateAbility(game.player1Id, relic, sacrificeAbility)).error.shouldNotBeNull()
            }

            val plains = game.findCardsInHand(1, "Plains").single()
            game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
            withClue("ten permanents with an ascend permanent grant the city's blessing") {
                game.state.getEntity(game.player1Id)?.has<PlayerCitysBlessingComponent>() shouldBe true
            }

            val handBefore = game.handSize(1)
            game.execute(ActivateAbility(game.player1Id, relic, sacrificeAbility)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 23
            game.handSize(1) shouldBe handBefore + 1
            game.isInGraveyard(1, "Orazca Relic") shouldBe true
        }
    }
}
