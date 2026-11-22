package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.VarolzTheScarStriped
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Varolz, the Scar-Striped (DGM #112) — {1}{B}{G} Legendary Creature — Troll Warrior 2/2.
 *
 *   Each creature card in your graveyard has scavenge. The scavenge cost is equal to its mana cost.
 *   Sacrifice another creature: Regenerate Varolz.
 */
class VarolzTheScarStripedScenarioTest : ScenarioTestBase() {
    private val scavenge = AbilityId("scavenge_own")
    private val regenerate = VarolzTheScarStriped.activatedAbilities.single().id

    private fun TestGame.scavengeActions(player: Int, card: String) = getLegalActions(player)
        .mapNotNull { it.action as? ActivateAbility }
        .filter { it.abilityId == scavenge && it.sourceId in findCardsInGraveyard(player, card) }

    private fun TestGame.plusOnes(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("a creature card in your graveyard scavenges for its own mana cost, exiling itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Varolz, the Scar-Striped")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            val action = game.scavengeActions(1, "Hill Giant").single()
            game.execute(action.copy(targets = listOf(ChosenTarget.Permanent(bears)))).error shouldBe null
            game.isInExile(1, "Hill Giant") shouldBe true
            game.resolveStack()

            game.plusOnes(bears) shouldBe 3
        }

        test("the scavenge cost keeps the card's coloured mana requirements") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Varolz, the Scar-Striped")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.scavengeActions(1, "Hill Giant").shouldBeEmpty()
        }

        test("only creature cards in your graveyard, and only as a sorcery") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Varolz, the Scar-Striped")
                .withCardInGraveyard(2, "Hill Giant")
                .withLandsOnBattlefield(2, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            // The opponent, in their own main phase, gets no scavenge from your Varolz.
            game.scavengeActions(2, "Hill Giant").shouldBeEmpty()

            val upkeep = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Varolz, the Scar-Striped")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
                .build()
            upkeep.scavengeActions(1, "Hill Giant").shouldBeEmpty()
        }

        test("sacrificing another creature regenerates Varolz through lethal damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Varolz, the Scar-Striped")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val varolz = game.findPermanent("Varolz, the Scar-Striped")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.state.activePlayerId!!,
                    sourceId = varolz,
                    abilityId = regenerate,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                )
            ).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true

            game.castSpell(1, "Lightning Bolt", varolz).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Varolz, the Scar-Striped") shouldBe true
        }
    }
}
