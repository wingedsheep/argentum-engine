package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardFlippedOntoBattlefieldEvent
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.FlipThrowResponse
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Falling Star (LEG #145) — {2}{R} Sorcery.
 *
 * "Flip Falling Star onto the playing area from a height of at least one foot. Falling Star deals
 *  3 damage to each creature it lands on. Tap all creatures dealt damage by Falling Star. If Falling
 *  Star doesn't turn completely over at least once during the flip, it has no effect."
 *
 * The flip is Chaos Orb's [FlipOntoBattlefieldDecision], thrown by a spell from the stack. The tap
 * reads the creatures the spell itself dealt damage to.
 */
class FallingStarScenarioTest : ScenarioTestBase() {

    /** Middle of the engine's clean release zone. */
    private val clean = 0.74

    private fun TestGame.castStar(): FlipOntoBattlefieldDecision {
        val cast = castSpell(1, "Falling Star")
        withClue("Casting Falling Star should succeed: ${cast.error}") { cast.error shouldBe null }
        resolveStack()
        return getPendingDecision().shouldBeInstanceOf<FlipOntoBattlefieldDecision>()
    }

    private fun TestGame.throwAtCard(
        decision: FlipOntoBattlefieldDecision,
        id: EntityId,
        strength: Double = clean,
    ): CardFlippedOntoBattlefieldEvent {
        val card = decision.table.cards.single { it.entityId == id }
        val result = submitDecision(FlipThrowResponse(decision.id, card.x, card.y, strength, 0.0))
        withClue("Throwing should succeed: ${result.error}") { result.error shouldBe null }
        return result.events.filterIsInstance<CardFlippedOntoBattlefieldEvent>().single()
    }

    private fun TestGame.damageOn(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0
    private fun TestGame.tapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    private fun baseScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withRngSeed(1L)
        .withCardInHand(1, "Falling Star")
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("a clean flip deals 3 damage to the creature it lands on and taps it") {
            val game = baseScenario()
                .withCardOnBattlefield(2, "Giant Spider")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .build()
            val spider = game.findPermanent("Giant Spider")!!

            val event = game.throwAtCard(game.castStar(), spider)

            event.landing.turnedOver.shouldBeTrue()
            event.affected shouldContain spider
            game.damageOn(spider) shouldBe 3
            game.tapped(spider).shouldBeTrue()
            withClue("a creature it didn't land on is neither damaged nor tapped") {
                val bears = game.findPermanent("Grizzly Bears")
                if (bears != null && bears !in event.affected) {
                    game.damageOn(bears) shouldBe 0
                    game.tapped(bears).shouldBeFalse()
                }
            }
            game.isInGraveyard(1, "Falling Star").shouldBeTrue()
        }

        test("a creature it lands on whose damage is prevented isn't dealt damage, so isn't tapped") {
            val game = baseScenario()
                .withCardOnBattlefield(2, "Repentant Blacksmith")
                .build()
            val blacksmith = game.findPermanent("Repentant Blacksmith")!!

            val event = game.throwAtCard(game.castStar(), blacksmith)

            event.affected shouldContain blacksmith
            game.damageOn(blacksmith) shouldBe 0
            game.tapped(blacksmith).shouldBeFalse()
        }

        test("if it doesn't turn over completely, it has no effect") {
            val game = baseScenario()
                .withCardOnBattlefield(2, "Giant Spider")
                .build()
            val spider = game.findPermanent("Giant Spider")!!

            val event = game.throwAtCard(game.castStar(), spider, strength = 0.0)

            event.landing.turnedOver.shouldBeFalse()
            event.affected.shouldBeEmpty()
            game.damageOn(spider) shouldBe 0
            game.tapped(spider).shouldBeFalse()
        }

        test("damage it dealt on an earlier cast this turn doesn't tap creatures on a later cast") {
            // The spell's damage record belongs to one stack object (CR 400.7). Hit the spider,
            // untap it, return the Star to hand and cast it again with a limp flip: the new spell
            // dealt no damage, so the spider stays untapped.
            val game = baseScenario()
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Giant Spider")
                .build()
            val spider = game.findPermanent("Giant Spider")!!

            game.throwAtCard(game.castStar(), spider)
            game.tapped(spider).shouldBeTrue()

            // Untap the spider and put the Star back in hand.
            game.state = game.state.updateEntity(spider) { it.without<TappedComponent>() }
            val star = game.findCardsInGraveyard(1, "Falling Star").single()
            game.state = game.state.moveToZone(
                star,
                com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.GRAVEYARD),
                com.wingedsheep.engine.state.ZoneKey(game.player1Id, com.wingedsheep.sdk.core.Zone.HAND),
            )

            val event = game.throwAtCard(game.castStar(), spider, strength = 0.0)
            event.affected.shouldBeEmpty()
            game.tapped(spider).shouldBeFalse()
        }
    }
}
