package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Narfi, Betrayer King (KHM #224).
 *
 *   Other snow and Zombie creatures you control get +1/+1.
 *   {S}{S}{S}: Return this card from your graveyard to the battlefield tapped.
 *
 * Pins the lord's union filter (snow *or* Zombie, other, yours) and the graveyard-zone activation
 * paid only with snow mana.
 */
class NarfiBetrayerKingScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Narfi, Betrayer King")!!.activatedAbilities.single().id

    init {
        test("pumps other snow creatures and Zombies you control, not Narfi, non-snow non-Zombies, or opponents'") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Narfi, Betrayer King")
                .withCardOnBattlefield(1, "Pilfering Hawk")
                .withCardOnBattlefield(1, "Gravedigger")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Avalanche Caller")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            fun pt(name: String) = game.findPermanent(name)!!.let { projected.getPower(it) to projected.getToughness(it) }

            withClue("snow creature") { pt("Pilfering Hawk") shouldBe (2 to 3) }
            withClue("Zombie") { pt("Gravedigger") shouldBe (3 to 3) }
            withClue("neither snow nor Zombie") { pt("Grizzly Bears") shouldBe (2 to 2) }
            withClue("\"other\": Narfi itself") { pt("Narfi, Betrayer King") shouldBe (4 to 3) }
            withClue("opponent's snow creature") { pt("Avalanche Caller") shouldBe (1 to 3) }
        }

        test("{S}{S}{S} returns it from the graveyard to the battlefield tapped") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Narfi, Betrayer King")
                .withLandsOnBattlefield(1, "Snow-Covered Swamp", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val narfi = game.findCardsInGraveyard(1, "Narfi, Betrayer King").single()

            game.execute(ActivateAbility(game.player1Id, narfi, abilityId)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Narfi, Betrayer King") shouldBe true
            val onBattlefield = game.findPermanent("Narfi, Betrayer King")!!
            game.state.getEntity(onBattlefield)!!.has<TappedComponent>() shouldBe true
        }

        test("non-snow mana cannot pay {S}{S}{S}") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInGraveyard(1, "Narfi, Betrayer King")
                .withLandsOnBattlefield(1, "Snow-Covered Swamp", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                .none { it.isAffordable } shouldBe true
        }
    }
}
