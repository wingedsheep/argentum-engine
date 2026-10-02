package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.SolarTransformer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Solar Transformer (MH3) — enters tapped, gets you {E}{E}{E}; {T}: Add {C}; {T}, Pay {E}: Add one
 * mana of any color.
 */
class SolarTransformerScenarioTest : ScenarioTestBase() {

    private val colorlessAbility = SolarTransformer.activatedAbilities[0].id
    private val anyColorAbility = SolarTransformer.activatedAbilities[1].id

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.pool(): ManaPoolComponent =
        state.getEntity(player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    /** Cast the Transformer from hand and resolve it plus its enters trigger, then untap it. */
    private fun castAndUntap(): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Solar Transformer")
            .withLandsOnBattlefield(1, "Wastes", 2)
            .withCardInLibrary(1, "Wastes")
            .withCardInLibrary(2, "Wastes")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Solar Transformer").error shouldBe null
        game.resolveStack()
        val transformer = game.findPermanent("Solar Transformer")!!
        game.state = game.state.updateEntity(transformer) { it.without<TappedComponent>() }
        return game to transformer
    }

    init {
        context("Solar Transformer") {

            test("enters tapped and gets you three energy") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Solar Transformer")
                    .withLandsOnBattlefield(1, "Wastes", 2)
                    .withCardInLibrary(1, "Wastes")
                    .withCardInLibrary(2, "Wastes")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.castSpell(1, "Solar Transformer").error shouldBe null
                game.resolveStack()

                val transformer = game.findPermanent("Solar Transformer")
                transformer shouldNotBe null
                withClue("this artifact enters tapped") {
                    game.state.getEntity(transformer!!)?.get<TappedComponent>() shouldNotBe null
                }
                game.energy() shouldBe 3
            }

            test("{T}: Add {C} costs no energy") {
                val (game, transformer) = castAndUntap()
                game.execute(ActivateAbility(game.player1Id, transformer, colorlessAbility)).error shouldBe null
                game.pool().colorless shouldBe 1
                game.energy() shouldBe 3
            }

            test("{T}, Pay {E}: adds one mana of the chosen color and spends one energy") {
                val (game, transformer) = castAndUntap()
                game.execute(
                    ActivateAbility(game.player1Id, transformer, anyColorAbility, manaColorChoice = Color.RED)
                ).error shouldBe null
                game.pool().red shouldBe 1
                game.energy() shouldBe 2
                game.state.getEntity(transformer)?.get<TappedComponent>() shouldNotBe null
            }

            test("the colored ability can't be activated without energy") {
                val (game, transformer) = castAndUntap()
                game.state = game.state.updateEntity(game.player1Id) {
                    it.without<CountersComponent>()
                }
                game.energy() shouldBe 0
                game.execute(
                    ActivateAbility(game.player1Id, transformer, anyColorAbility, manaColorChoice = Color.GREEN)
                ).error shouldNotBe null
                game.pool().green shouldBe 0
            }
        }
    }
}
