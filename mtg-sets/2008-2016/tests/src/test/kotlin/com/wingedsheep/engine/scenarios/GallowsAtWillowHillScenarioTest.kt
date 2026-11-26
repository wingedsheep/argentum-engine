package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gallows at Willow Hill (AVR #215) — {3} Artifact
 *
 *   {3}, {T}, Tap three untapped Humans you control: Destroy target creature. Its controller creates
 *   a 1/1 white Spirit creature token with flying.
 *
 * The Spirit goes to the destroyed creature's controller, not to the Gallows' controller; the cost
 * needs three Humans, and a non-Human can't stand in for one.
 */
class GallowsAtWillowHillScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.requireCard("Gallows at Willow Hill").activatedAbilities[0].id
    }

    init {
        fun isTapped(game: TestGame, id: EntityId): Boolean =
            game.state.getEntity(id)?.has<TappedComponent>() == true

        fun tokens(game: TestGame, playerId: EntityId): List<EntityId> =
            game.state.getBattlefield(playerId).filter { game.state.getEntity(it)?.has<TokenComponent>() == true }

        fun board(): TestGame = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Gallows at Willow Hill")
            .withCardOnBattlefield(1, "Elite Vanguard")
            .withCardOnBattlefield(1, "Elite Vanguard")
            .withCardOnBattlefield(1, "Elite Vanguard")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("tapping three Humans destroys the target and its controller gets a flying Spirit") {
            val game = board()
            val gallows = game.findPermanent("Gallows at Willow Hill")!!
            val humans = game.findAllPermanents("Elite Vanguard")
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = gallows,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                    costPayment = AdditionalCostPayment(tappedPermanents = humans)
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the target is destroyed") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
            withClue("the Gallows and the three Humans paid the cost") {
                isTapped(game, gallows) shouldBe true
                humans.forEach { isTapped(game, it) shouldBe true }
            }
            withClue("the destroyed creature's controller gets the Spirit, not the activator") {
                val spirits = tokens(game, game.player2Id)
                spirits shouldHaveSize 1
                val spirit = spirits.single()
                game.state.getEntity(spirit)!!.get<CardComponent>()!!.typeLine.subtypes.map { it.value } shouldBe listOf("Spirit")
                game.state.projectedState.hasKeyword(spirit, "FLYING") shouldBe true
                game.state.projectedState.getPower(spirit) shouldBe 1
                game.state.projectedState.getToughness(spirit) shouldBe 1
                tokens(game, game.player1Id).shouldBeEmpty()
            }
        }

        test("a non-Human creature can't pay for one of the three Humans") {
            val game = board()
            val gallows = game.findPermanent("Gallows at Willow Hill")!!
            val humans = game.findAllPermanents("Elite Vanguard").take(2)
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = gallows,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(giant)),
                    costPayment = AdditionalCostPayment(tappedPermanents = humans + bears)
                )
            ).error shouldNotBe null
            game.isOnBattlefield("Hill Giant") shouldBe true
        }
    }
}
