package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dmu.cards.AjaniSleeperAgent
import com.wingedsheep.mtg.sets.definitions.snc.cards.LuxiorGiadasGift
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Luxior, Giada's Gift (SNC #240) — {1} Legendary Artifact — Equipment.
 *
 * "Equipped creature gets +1/+1 for each counter on it.
 *  Equipped permanent isn't a planeswalker and is a creature in addition to its other types.
 *  (Loyalty abilities can still be activated.)
 *  Equip planeswalker {1}
 *  Equip {3}"
 *
 * The novel part is an Equipment that attaches to a non-creature and keeps it legal by making it
 * a creature: the equipped planeswalker stops being a planeswalker, becomes a 0/0 creature that
 * grows with its counters, and can still activate its loyalty abilities.
 */
class LuxiorGiadasGiftScenarioTest : ScenarioTestBase() {

    private val equipPlaneswalker = LuxiorGiadasGift.activatedAbilities
        .first { it.isEquipAbility && it.equipQuality == "planeswalker" }
    private val equipCreature = LuxiorGiadasGift.activatedAbilities
        .first { it.isEquipAbility && it.equipQuality == null }
    private val ajaniPlusOne = AjaniSleeperAgent.activatedAbilities[0].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun equip(game: TestGame, luxior: EntityId, abilityId: com.wingedsheep.sdk.scripting.AbilityId, target: EntityId) =
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = luxior,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )

    init {
        test("equip planeswalker turns the planeswalker into a creature sized by its counters, loyalty abilities still work") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Luxior, Giada's Gift")
                .withCardOnBattlefield(1, "Ajani, Sleeper Agent")
                .withCardInLibrary(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val luxior = game.findPermanent("Luxior, Giada's Gift")!!
            val ajani = game.findPermanent("Ajani, Sleeper Agent")!!
            seedLoyalty(game, ajani, 4)

            val result = equip(game, luxior, equipPlaneswalker.id, ajani)
            withClue("equip planeswalker {1} should accept a planeswalker: ${result.error}") { result.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Luxior stays attached — the host is a creature now, so CR 704.5n leaves it on") {
                game.state.getEntity(luxior)?.get<AttachedToComponent>()?.targetId shouldBe ajani
            }
            projected.isCreature(ajani) shouldBe true
            projected.isPlaneswalker(ajani) shouldBe false
            withClue("base 0/0 plus +1/+1 for each of its four loyalty counters") {
                projected.getPower(ajani) shouldBe 4
                projected.getToughness(ajani) shouldBe 4
            }

            withClue("its loyalty abilities can still be activated") {
                game.execute(ActivateAbility(game.player1Id, ajani, ajaniPlusOne)).error shouldBe null
            }
            game.resolveStack()
            loyalty(game, ajani) shouldBe 5
            game.state.projectedState.getPower(ajani) shouldBe 5
            game.state.projectedState.getToughness(ajani) shouldBe 5
        }

        test("equip planeswalker can't target a creature; plain equip on a creature counts its counters") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Luxior, Giada's Gift")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val luxior = game.findPermanent("Luxior, Giada's Gift")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("Equip planeswalker only targets a planeswalker you control") {
                equip(game, luxior, equipPlaneswalker.id, bears).error shouldNotBe null
            }

            game.state = game.state.updateEntity(bears) { c ->
                c.with(CountersComponent().withAdded(CounterType.PLUS_ONE_PLUS_ONE, 2))
            }
            val result = equip(game, luxior, equipCreature.id, bears)
            withClue("equip {3} should accept a creature: ${result.error}") { result.error shouldBe null }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.state.getEntity(luxior)?.get<AttachedToComponent>()?.targetId shouldBe bears
            withClue("2/2 + two +1/+1 counters = 4/4, then +2/+2 for the two counters on it") {
                game.state.projectedState.getPower(bears) shouldBe 6
                game.state.projectedState.getToughness(bears) shouldBe 6
            }
        }
    }
}
