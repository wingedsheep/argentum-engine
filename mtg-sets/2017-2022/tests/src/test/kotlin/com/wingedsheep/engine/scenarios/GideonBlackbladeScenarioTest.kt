package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.war.cards.GideonBlackblade
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * Gideon Blackblade (WAR #13). On his controller's turn he's a 4/4 indestructible Human Soldier
 * creature that's *still* a planeswalker, and damage to him is prevented; on any other turn he's
 * just a planeswalker. The +1 asks for the keyword on resolution.
 */
class GideonBlackbladeScenarioTest : ScenarioTestBase() {

    private val plusOne = GideonBlackblade.activatedAbilities[0].id

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        test("on his controller's turn he's a 4/4 indestructible Human Soldier creature and still a planeswalker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon Blackblade")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gideon = game.findPermanent("Gideon Blackblade")!!
            seedLoyalty(game, gideon, 4)

            val projected = game.state.projectedState
            projected.isCreature(gideon) shouldBe true
            projected.isPlaneswalker(gideon) shouldBe true
            projected.getSubtypes(gideon) shouldContainAll setOf("Human", "Soldier", "Gideon")
            projected.getPower(gideon) shouldBe 4
            projected.getToughness(gideon) shouldBe 4
            projected.hasKeyword(gideon, Keyword.INDESTRUCTIBLE) shouldBe true
        }

        test("on another player's turn he's only a planeswalker") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Gideon Blackblade")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gideon = game.findPermanent("Gideon Blackblade")!!
            seedLoyalty(game, gideon, 4)

            val projected = game.state.projectedState
            projected.isCreature(gideon) shouldBe false
            projected.isPlaneswalker(gideon) shouldBe true
            projected.hasKeyword(gideon, Keyword.INDESTRUCTIBLE) shouldBe false
        }

        test("damage to him is prevented during his controller's turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon Blackblade")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gideon = game.findPermanent("Gideon Blackblade")!!
            seedLoyalty(game, gideon, 4)

            game.castSpell(1, "Lightning Bolt", gideon).error shouldBe null
            game.resolveStack()

            withClue("no loyalty lost") { loyalty(game, gideon) shouldBe 4 }
            withClue("no damage marked") {
                (game.state.getEntity(gideon)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
            }
        }

        test("damage to him is not prevented during another player's turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(2, "Gideon Blackblade")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gideon = game.findPermanent("Gideon Blackblade")!!
            seedLoyalty(game, gideon, 4)

            game.castSpell(1, "Lightning Bolt", gideon).error shouldBe null
            game.resolveStack()

            loyalty(game, gideon) shouldBe 1
        }

        test("+1 gives another creature you control the keyword chosen on resolution") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Gideon Blackblade")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gideon = game.findPermanent("Gideon Blackblade")!!
            seedLoyalty(game, gideon, 4)
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = gideon,
                    abilityId = plusOne,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            val choice = game.getPendingDecision() as ChooseOptionDecision
            game.submitDecision(
                OptionChosenResponse(choice.id, choice.options.indexOf("Lifelink until end of turn"))
            ).error shouldBe null

            val projected = game.state.projectedState
            projected.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
            projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
            projected.hasKeyword(bears, Keyword.INDESTRUCTIBLE) shouldBe false
            loyalty(game, gideon) shouldBe 5
        }
    }
}
