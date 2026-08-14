package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Four Aetherdrift cards whose behaviour isn't already covered by the mechanic-level suites:
 *
 * | Card | Under test |
 * |---|---|
 * | Greasewrench Goblin | `Patterns.Hand.discardUpToThenDraw` — the draw counts what was *actually* discarded, and the exhaust fires once |
 * | Loxodon Surveyor | a max-speed gate on an ability that functions from the **graveyard** |
 * | Nesting Bot | an ungated dies trigger under a gated P/T buff |
 * | Stall Out | tap-then-stun on one target |
 */
class AetherdriftDriftersScenarioTest : ScenarioTestBase() {

    init {
        context("Greasewrench Goblin") {

            test("discarding two draws two, and the +1/+1 counter lands alongside") {
                val game = goblinGame(handCards = 2)
                val goblin = game.findPermanent("Greasewrench Goblin")!!
                val handBefore = game.handSize(1)

                game.activateExhaust(goblin)
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(2))
                game.resolveStack()

                withClue("Two discarded, two drawn — hand size is unchanged") {
                    game.handSize(1) shouldBe handBefore
                }
                withClue("Both chosen cards are in the graveyard") {
                    game.graveyardSize(1) shouldBe 2
                }
                withClue("The counter is a sibling of the loot, not a rider on it") {
                    game.plusOneCounters(goblin) shouldBe 1
                }
            }

            test("declining the discard draws nothing but still adds the counter") {
                val game = goblinGame(handCards = 2)
                val goblin = game.findPermanent("Greasewrench Goblin")!!
                val handBefore = game.handSize(1)
                val libraryBefore = game.librarySize(1)

                game.activateExhaust(goblin)
                game.skipSelection()
                game.resolveStack()

                withClue("Ruling 2025-02-07: activating without discarding is legal, and draws zero") {
                    game.handSize(1) shouldBe handBefore
                    game.librarySize(1) shouldBe libraryBefore
                    game.graveyardSize(1) shouldBe 0
                }
                withClue("The counter still lands") { game.plusOneCounters(goblin) shouldBe 1 }
            }

            test("discarding one card draws exactly one, not the ceiling of two") {
                val game = goblinGame(handCards = 1)
                val goblin = game.findPermanent("Greasewrench Goblin")!!
                val libraryBefore = game.librarySize(1)

                game.activateExhaust(goblin)
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears").take(1))
                game.resolveStack()

                withClue("The draw reads discarded_count, so one discard means one draw") {
                    game.librarySize(1) shouldBe libraryBefore - 1
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("the exhaust ability can only be activated once") {
                val game = goblinGame(handCards = 0, lands = 6)
                val goblin = game.findPermanent("Greasewrench Goblin")!!

                // Empty hand: the gather yields nothing, so there is no discard prompt to answer.
                game.activateExhaust(goblin)
                withClue("Nothing to discard, so no selection decision is raised") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()

                val second = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = goblin,
                        abilityId = exhaustAbilityId()
                    )
                )
                withClue("CR 702.177a: activate each exhaust ability only once") {
                    (second.error != null).shouldBeTrue()
                }
            }
        }

        context("Loxodon Surveyor") {

            test("its graveyard ability is gated on max speed and exiles the card to draw") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Loxodon Surveyor")
                    .withLandsOnBattlefield(1, "Forest", 3)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val surveyor = game.findCardsInGraveyard(1, "Loxodon Surveyor").single()
                val abilityId = cardRegistry.getCard("Loxodon Surveyor")!!
                    .script.activatedAbilities.single().id

                val blocked = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = surveyor, abilityId = abilityId)
                )
                withClue("Below max speed the graveyard ability isn't there to activate") {
                    (blocked.error != null).shouldBeTrue()
                }

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first
                val libraryBefore = game.librarySize(1)

                val allowed = game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = surveyor, abilityId = abilityId)
                )
                withClue("At max speed the gate opens in the graveyard too: ${allowed.error}") {
                    allowed.error shouldBe null
                }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("Exiled as a cost, and one card drawn") {
                    game.isInExile(1, "Loxodon Surveyor") shouldBe true
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }
        }

        context("Nesting Bot") {

            test("leaves a Servo behind when it dies below max speed") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nesting Bot", summoningSickness = false)
                    .withCardInHand(1, "Lightning Strike")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                val bot = game.findPermanent("Nesting Bot")!!
                withClue("Speed 1: the +1/+0 gate is shut, so it's still a 1/1") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                    game.state.projectedState.getPower(bot) shouldBe 1
                }

                // Three damage to the 1/1 kills it for real, so the dies trigger actually fires
                // (the moveToGraveyard test helper skips dies triggers by design).
                val bolt = game.castSpell(1, "Lightning Strike", bot)
                withClue("Lightning Strike cast failed: ${bolt.error}") { bolt.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("The dies trigger is ungated, so the Servo arrives at speed 1") {
                    game.isOnBattlefield("Nesting Bot") shouldBe false
                    game.servoTokens() shouldBe 1
                }
            }

            test("gets +1/+0 at max speed") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nesting Bot", summoningSickness = false)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UPKEEP)
                    .build()
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val bot = game.findPermanent("Nesting Bot")!!

                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                withClue("1/1 becomes 2/1") {
                    game.state.projectedState.getPower(bot) shouldBe 2
                    game.state.projectedState.getToughness(bot) shouldBe 1
                }
            }
        }

        context("Stall Out") {

            test("taps the target and puts three stun counters on it") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stall Out")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Stall Out", bear)
                withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("Tapped, and holding three stun counters") {
                    game.state.getEntity(bear)!!.get<TappedComponent>().shouldNotBeNull()
                    game.state.getEntity(bear)?.get<CountersComponent>()
                        ?.getCount(CounterType.STUN) shouldBe 3
                }
            }
        }
    }

    /**
     * Player 1's precombat main phase with Greasewrench Goblin out, [handCards] discardable cards in
     * hand and enough red mana for the exhaust. Built in the upkeep step so the CR 704.5z
     * start-your-engines state-based action has a step change to run on.
     */
    private fun goblinGame(handCards: Int, lands: Int = 3): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Greasewrench Goblin", summoningSickness = false)
            .withLandsOnBattlefield(1, "Mountain", lands)
        if (handCards > 0) builder.withCardsInHand(1, "Grizzly Bears", handCards)
        stockLibraries(builder)
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /** Both libraries stocked so nobody decks out and draws always have something to take. */
    private fun stockLibraries(builder: ScenarioBuilder) {
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
    }

    private fun exhaustAbilityId() =
        cardRegistry.getCard("Greasewrench Goblin")!!
            .script.activatedAbilities.single { it.isExhaust }.id

    /** Activate the exhaust ability, pay for it, and resolve up to the discard decision. */
    private fun TestGame.activateExhaust(sourceId: EntityId) {
        val result = execute(
            ActivateAbility(playerId = player1Id, sourceId = sourceId, abilityId = exhaustAbilityId())
        )
        withClue("Exhaust activation failed: ${result.error}") { result.error shouldBe null }
        autoPayIfAsked()
        resolveStack()
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Servo artifact creature tokens on the battlefield. */
    private fun TestGame.servoTokens(): Int =
        state.getBattlefield().count { id ->
            val container = state.getEntity(id) ?: return@count false
            container.has<TokenComponent>() &&
                container.get<CardComponent>()?.typeLine?.subtypes?.any { it.value == "Servo" } == true
        }

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }
}
