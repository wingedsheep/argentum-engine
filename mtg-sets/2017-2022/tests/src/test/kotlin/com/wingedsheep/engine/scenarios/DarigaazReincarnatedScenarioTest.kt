package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Darigaaz Reincarnated (DOM #193).
 *
 *   If Darigaaz Reincarnated would die, instead exile it with three egg counters on it.
 *   At the beginning of your upkeep, if Darigaaz is exiled with an egg counter on it, remove an egg
 *   counter from it. Then if Darigaaz has no egg counters on it, return it to the battlefield.
 *
 * The upkeep ability functions from exile, so its intervening "if" counts the eggs on the exiled
 * card — not the battlefield-exit snapshot, taken before the eggs were added.
 */
class DarigaazReincarnatedScenarioTest : ScenarioTestBase() {

    private val slay = card("Darigaaz Test Slay") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.Destroy(EffectTarget.ContextTarget(0)) }
    }

    init {
        cardRegistry.register(slay)

        fun TestGame.exiledDarigaaz(): EntityId? =
            state.getExile(player1Id).firstOrNull { state.getEntity(it)!!.get<CardComponent>()!!.name == "Darigaaz Reincarnated" }

        fun TestGame.eggs(id: EntityId): Int = state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.EGG) ?: 0

        fun TestGame.toOwnUpkeep() {
            do {
                passUntilPhase(Phase.ENDING, Step.END)
                passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            } while (state.activePlayerId != player1Id)
            resolveStack()
        }

        test("dying exiles it with three eggs; one comes off each upkeep and it returns on the third") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Darigaaz Reincarnated")
                .withCardInHand(1, "Darigaaz Test Slay")
                .apply { repeat(10) { withCardInLibrary(1, "Mountain"); withCardInLibrary(2, "Mountain") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Darigaaz Test Slay", game.findPermanent("Darigaaz Reincarnated")!!).error shouldBe null
            game.resolveStack()
            val exiled = game.exiledDarigaaz()
            exiled shouldNotBe null
            game.eggs(exiled!!) shouldBe 3

            game.toOwnUpkeep()
            withClue("first upkeep removes an egg") { game.eggs(exiled) shouldBe 2 }
            game.toOwnUpkeep()
            game.eggs(exiled) shouldBe 1
            game.toOwnUpkeep()
            withClue("the last egg comes off and Darigaaz returns") {
                game.findPermanent("Darigaaz Reincarnated") shouldNotBe null
                game.exiledDarigaaz() shouldBe null
            }
        }
    }
}
