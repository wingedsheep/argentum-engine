package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AttackTax
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Summon: Yojimbo (FIC #28) — chapter II: "Until your next turn, creatures can't attack you unless
 * their controller pays {2} for each of those creatures"; chapter IV: "Create X Treasure tokens,
 * where X is the number of opponents who control a creature with power 4 or greater."
 *
 * The tax is held by the player, not the Saga, so it still applies after the Saga is gone.
 */
class SummonYojimboScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.setLore(lore: Int) {
            val saga = findPermanent("Summon: Yojimbo")!!
            state = state.updateEntity(saga) { container ->
                container.with(CountersComponent().withAdded(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
            }
        }

        test("chapter II taxes the opponent's attack on you, even after the Saga is gone") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Summon: Yojimbo")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Forest") } }
                .build()
            game.setLore(1)

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()
            withClue("the tax is granted to the Saga's controller") {
                game.state.grantedStaticAbilities.single().let {
                    it.entityId shouldBe game.player1Id
                    (it.ability is AttackTax) shouldBe true
                }
            }

            // The Saga leaving doesn't end the tax — it's a rule about the player now.
            val saga = game.findPermanent("Summon: Yojimbo")!!
            game.state = game.state.moveToZone(
                saga, ZoneKey(game.player1Id, Zone.BATTLEFIELD), ZoneKey(game.player1Id, Zone.GRAVEYARD)
            )

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.state.activePlayerId shouldBe game.player2Id
            val bears = game.findPermanents("Grizzly Bears")
            val result = game.execute(DeclareAttackers(game.player2Id, bears.associateWith { game.player1Id }))
            result.error shouldBe null
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                .requiredCost shouldBe "{1}{1}{1}{1}"
        }

        test("chapter IV makes a Treasure per opponent controlling a creature with power 4 or greater") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Summon: Yojimbo")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Forest") } }
                .build()
            game.setLore(3)

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            game.findPermanents("Treasure").size shouldBe 1
            withClue("the Saga is sacrificed after chapter IV") {
                game.findPermanent("Summon: Yojimbo") shouldBe null
            }
        }

        test("chapter IV counts only opponents — your own 5/5 Saga doesn't make a Treasure") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Summon: Yojimbo")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .apply { repeat(6) { withCardInLibrary(1, "Plains"); withCardInLibrary(2, "Forest") } }
                .build()
            game.setLore(3)

            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.resolveStack()

            game.findPermanents("Treasure").size shouldBe 0
        }
    }
}
