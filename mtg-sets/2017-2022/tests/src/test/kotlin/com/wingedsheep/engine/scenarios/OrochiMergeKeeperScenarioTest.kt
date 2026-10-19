package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.neo.cards.OrochiMergeKeeper
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Orochi Merge-Keeper (NEO #203) — {T}: Add {G}. As long as it's modified, it has
 * "{T}: Add {G}{G}." The {G}{G} grant is self-scoped through a battlefield-scope filter so the
 * mana solver sees it as well as the legal-action enumerator.
 */
class OrochiMergeKeeperScenarioTest : ScenarioTestBase() {

    private fun addCounter(game: TestGame, id: EntityId) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
        }
    }

    private fun manaActions(game: TestGame, id: EntityId) =
        game.getLegalActions(1).map { it.action }.filterIsInstance<ActivateAbility>().filter { it.sourceId == id }

    private fun build() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Orochi Merge-Keeper")
        .withCardInHand(1, "Grizzly Bears")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("unmodified it has only {T}: Add {G} and can't pay for a two-mana spell") {
            val game = build()
            val keeper = game.findPermanent("Orochi Merge-Keeper")!!

            manaActions(game, keeper).size shouldBe 1
            game.castSpell(1, "Grizzly Bears").error shouldNotBe null
        }

        test("modified it gains {T}: Add {G}{G}") {
            val game = build()
            val keeper = game.findPermanent("Orochi Merge-Keeper")!!
            addCounter(game, keeper)

            val actions = manaActions(game, keeper)
            actions.size shouldBe 2

            val ggAbility = actions.first { it.abilityId != OrochiMergeKeeper.activatedAbilities[0].id }
            game.execute(ggAbility).error shouldBe null
            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 2
        }

        test("modified it auto-pays a two-mana spell by itself") {
            val game = build()
            val keeper = game.findPermanent("Orochi Merge-Keeper")!!
            addCounter(game, keeper)

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
