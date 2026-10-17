package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardFlippedOntoBattlefieldEvent
import com.wingedsheep.engine.core.FlipOntoBattlefieldDecision
import com.wingedsheep.engine.core.FlipThrowResponse
import com.wingedsheep.engine.mechanics.dexterity.FlipTableLayout
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chaos Orb (LEA #235) — {2} Artifact.
 *
 * "{1}, {T}: If this artifact is on the battlefield, flip it onto the battlefield from a height of
 *  at least one foot. If this artifact turns over completely at least once during the flip, destroy
 *  all nontoken permanents it touches. Then destroy this artifact."
 *
 * The flip is a [FlipOntoBattlefieldDecision]: the player aims at a point on the engine's table and
 * picks a release strength, and the engine rolls where it lands. Landings are random, so each test
 * reads the [CardFlippedOntoBattlefieldEvent] for what was touched and checks the board against it.
 */
class ChaosOrbScenarioTest : ScenarioTestBase() {

    private val orbAbilityId = cardRegistry.getCard("Chaos Orb")!!.activatedAbilities.first().id

    /** Middle of the engine's clean release zone. */
    private val clean = 0.74

    private fun TestGame.activateOrb(): FlipOntoBattlefieldDecision {
        val activation = execute(
            ActivateAbility(playerId = player1Id, sourceId = findPermanent("Chaos Orb")!!, abilityId = orbAbilityId)
        )
        withClue("Activating Chaos Orb should succeed: ${activation.error}") { activation.error shouldBe null }
        resolveStack()
        return getPendingDecision().shouldBeInstanceOf<FlipOntoBattlefieldDecision>()
    }

    private fun TestGame.throwAt(
        decision: FlipOntoBattlefieldDecision,
        x: Double,
        y: Double,
        strength: Double = clean,
        sway: Double = 0.0,
    ): CardFlippedOntoBattlefieldEvent {
        val result = submitDecision(FlipThrowResponse(decision.id, x, y, strength, sway))
        withClue("Throwing should succeed: ${result.error}") { result.error shouldBe null }
        return result.events.filterIsInstance<CardFlippedOntoBattlefieldEvent>().single()
    }

    private fun TestGame.throwAtCard(
        decision: FlipOntoBattlefieldDecision,
        id: EntityId,
        strength: Double = clean,
        sway: Double = 0.0,
    ) = decision.table.cards.single { it.entityId == id }.let { throwAt(decision, it.x, it.y, strength, sway) }

    /** Everything the flip counts is destroyed; every other permanent on the table is untouched. */
    private fun TestGame.assertBoardMatches(event: CardFlippedOntoBattlefieldEvent) {
        val battlefield = state.getBattlefield().toSet()
        for (card in event.table.cards) {
            withClue("${card.name}: affected=${card.entityId in event.affected}") {
                (card.entityId in battlefield) shouldBe (card.entityId !in event.affected)
            }
        }
    }

    private fun baseScenario() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Chaos Orb")
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Chaos Orb") {

            test("a clean flip onto a creature destroys it, then the Orb destroys itself") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                val decision = game.activateOrb()
                val event = game.throwAtCard(decision, bears)

                event.landing.turnedOver.shouldBeTrue()
                event.affected shouldContain bears
                game.isInGraveyard(2, "Grizzly Bears").shouldBeTrue()
                game.isInGraveyard(1, "Chaos Orb").shouldBeTrue()
                game.assertBoardMatches(event)
            }

            test("it is not targeted: your own permanents it touches are destroyed too") {
                val game = baseScenario()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()
                val giant = game.findPermanent("Hill Giant")!!

                val decision = game.activateOrb()
                val event = game.throwAtCard(decision, giant)

                event.affected shouldContain giant
                game.isInGraveyard(1, "Hill Giant").shouldBeTrue()
                game.assertBoardMatches(event)
            }

            test("let go too early and it falls short — onto your own creature in front of you") {
                // The landing scatters about a centimetre from the game RNG and neighbouring cards sit about
                // a centimetre apart, so an unlucky roll clips the wrong card. Pin the seed: test the aim.
                val game = baseScenario()
                    .withRngSeed(1L)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val decision = game.activateOrb()
                val target = decision.table.cards.single { it.entityId == bears }
                val own = decision.table.cards.single { it.entityId == giant }
                // Short by exactly the distance between the two creatures, along the line of the throw.
                val gap = kotlin.math.hypot(own.x - target.x, own.y - target.y)
                val early = decision.release.sweetSpot - gap / decision.release.depthPerStrength
                val event = game.throwAtCard(decision, bears, strength = early)

                event.landing.drift shouldBeLessThan -100.0
                event.affected shouldContain giant
                event.affected shouldNotContain bears
                game.isInGraveyard(1, "Hill Giant").shouldBeTrue()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                game.assertBoardMatches(event)
            }

            test("let go mid-sway and it pulls sideways — onto the creature next to your target") {
                // The landing scatters about a centimetre from the game RNG and neighbouring cards sit about
                // a centimetre apart, so an unlucky roll clips the wrong card. Pin the seed: test the aim.
                val game = baseScenario()
                    .withRngSeed(1L)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val decision = game.activateOrb()
                val target = decision.table.cards.single { it.entityId == bears }
                val neighbour = decision.table.cards.single { it.entityId == giant }
                // From the thrower's seat, +x is their right: sway towards the neighbour by exactly the gap.
                val sway = (neighbour.x - target.x) / decision.release.sidePerSway
                val event = game.throwAtCard(decision, bears, sway = sway)

                kotlin.math.abs(event.landing.pull) shouldBeGreaterThan 60.0
                event.affected shouldContain giant
                event.affected shouldNotContain bears
                game.isInGraveyard(2, "Hill Giant").shouldBeTrue()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                game.assertBoardMatches(event)
            }

            test("tokens it touches survive — it destroys only nontoken permanents") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                    .build()
                val token = game.findPermanent("Grizzly Bears")!!

                val decision = game.activateOrb()
                decision.table.cards.single { it.entityId == token }.eligible.shouldBeFalse()
                val event = game.throwAtCard(decision, token)

                event.touched shouldContain token
                event.affected shouldNotContain token
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                game.isInGraveyard(1, "Chaos Orb").shouldBeTrue()
            }

            test("a flip that misses the table touches nothing, and the Orb is still destroyed") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()

                val decision = game.activateOrb()
                // Well past the far edge of the table.
                val event = game.throwAt(decision, decision.table.width / 2, -350.0)

                event.touched.shouldBeEmpty()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                game.isInGraveyard(1, "Chaos Orb").shouldBeTrue()
            }

            test("if it doesn't turn over completely, nothing it touches is destroyed") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!

                val decision = game.activateOrb()
                // A limp release barely leaves the hand spinning.
                val event = game.throwAtCard(decision, bears, strength = 0.0)

                event.landing.turnedOver.shouldBeFalse()
                event.affected.shouldBeEmpty()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
                game.isInGraveyard(1, "Chaos Orb").shouldBeTrue()
            }

            test("if the Orb has left the battlefield when the ability resolves, there is no flip") {
                val game = baseScenario()
                    .withCardInHand(1, "Shatter")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .build()
                val orb = game.findPermanent("Chaos Orb")!!

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = orb, abilityId = orbAbilityId))
                    .error shouldBe null
                // Respond by shattering the Orb, so it's gone before its ability resolves.
                game.castSpell(1, "Shatter", orb).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeNull()
                game.isInGraveyard(1, "Chaos Orb").shouldBeTrue()
                game.isOnBattlefield("Grizzly Bears").shouldBeTrue()
            }

            test("the table seats the flipper at the bottom, turns tapped cards sideways and leaves the Orb out") {
                val game = baseScenario()
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .build()
                val orb = game.findPermanent("Chaos Orb")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val decision = game.activateOrb()
                val table = decision.table

                table.cards.none { it.entityId == orb }.shouldBeTrue()
                table.bands.last().playerId shouldBe game.player1Id
                table.bands.first().playerId shouldBe game.player2Id

                val bearsCard = table.cards.single { it.entityId == bears }
                bearsCard.width shouldBe FlipTableLayout.CARD_HEIGHT
                bearsCard.height shouldBe FlipTableLayout.CARD_WIDTH
                val giantCard = table.cards.single { it.entityId == giant }
                giantCard.y shouldBeGreaterThan bearsCard.y
                // The flipper's hand is past their own edge of the table.
                table.handY shouldBeGreaterThan table.height
                bearsCard.y shouldBeLessThan table.bands.first().top + table.bands.first().height
            }
        }
    }
}
