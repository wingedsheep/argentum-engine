package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m21.cards.GarrukSavageHerald
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Garruk, Savage Herald (M21 #336).
 *
 * Pins the −7: the granted "you may have this creature assign its combat damage as though it
 * weren't blocked" is a runtime grant of [com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked]
 * to each creature you control at resolution — the combat-damage read site must see it, and a
 * creature that arrives later in the turn must not have it (ruling). Also pins the −2 bite.
 */
class GarrukSavageHeraldScenarioTest : ScenarioTestBase() {

    private val minusTwo = GarrukSavageHerald.activatedAbilities[1].id
    private val minusSeven = GarrukSavageHerald.activatedAbilities[2].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    init {
        test("−7: a blocked creature you control may assign its combat damage as though unblocked") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Garruk, Savage Herald")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val garruk = game.findPermanent("Garruk, Savage Herald")!!
            seedLoyalty(game, garruk, 7)

            game.execute(ActivateAbility(game.player1Id, garruk, minusSeven)).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Wall of Granite" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the Bears' 2 damage goes to the defending player despite the block") {
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("−7: a creature that comes under your control afterwards doesn't gain the ability") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Garruk, Savage Herald")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withCardOnBattlefield(2, "Wall of Granite")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val garruk = game.findPermanent("Garruk, Savage Herald")!!
            seedLoyalty(game, garruk, 7)

            game.execute(ActivateAbility(game.player1Id, garruk, minusSeven)).error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()
            val bears = game.findPermanent("Grizzly Bears")!!

            withClue("only creatures controlled at resolution received the grant") {
                game.state.grantedStaticAbilities.any { it.entityId == bears } shouldBe false
            }
        }

        test("−2: target creature you control deals damage equal to its power to another target creature") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Garruk, Savage Herald")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val garruk = game.findPermanent("Garruk, Savage Herald")!!
            seedLoyalty(game, garruk, 5)
            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = garruk,
                    abilityId = minusTwo,
                    targets = listOf(ChosenTarget.Permanent(giant), ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
