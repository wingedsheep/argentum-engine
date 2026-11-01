package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.ChandraFireArtisan
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bioessence Hydra (WAR #186).
 *
 *   This creature enters with a +1/+1 counter on it for each loyalty counter on planeswalkers you
 *   control.
 *   Whenever one or more loyalty counters are put on planeswalkers you control, put that many
 *   +1/+1 counters on this creature.
 *
 * Proves the entry count sums loyalty across planeswalkers, that a +N loyalty cost feeds the
 * trigger, and that a planeswalker entering with loyalty fires it for its whole starting loyalty
 * (rulings).
 */
class BioessenceHydraScenarioTest : ScenarioTestBase() {

    init {
        test("enters with a +1/+1 counter per loyalty counter across your planeswalkers") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Chandra, Fire Artisan")
                .withCardOnBattlefield(1, "Jiang Yanggu, Wildcrafter")
                .withCardOnBattlefield(2, "Chandra, Fire Artisan")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInHand(1, "Bioessence Hydra")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Bioessence Hydra").error shouldBe null
            game.resolveStack()

            val hydra = game.findPermanent("Bioessence Hydra")!!
            withClue("4 (Chandra) + 3 (Jiang Yanggu); the opponent's Chandra doesn't count") {
                counters(game, hydra, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 7
            }
        }

        test("a +1 loyalty ability puts a +1/+1 counter on the Hydra") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Bioessence Hydra")
                .withCardOnBattlefield(1, "Chandra, Fire Artisan")
                .withCardInLibrary(1, "Shock")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val hydra = game.findPermanent("Bioessence Hydra")!!
            val chandra = game.findPermanent("Chandra, Fire Artisan")!!
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = chandra,
                    abilityId = ChandraFireArtisan.activatedAbilities[0].id,
                )
            ).error shouldBe null
            game.resolveStack()

            counters(game, chandra, CounterType.LOYALTY) shouldBe 5
            counters(game, hydra, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        }

        test("a planeswalker entering with loyalty gives the Hydra that many counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Bioessence Hydra")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInHand(1, "Chandra, Fire Artisan")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Chandra, Fire Artisan").error shouldBe null
            game.resolveStack()

            val hydra = game.findPermanent("Bioessence Hydra")!!
            counters(game, hydra, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 4
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0
}
