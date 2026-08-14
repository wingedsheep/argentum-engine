package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.speed.SpeedService
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Speed
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Three Aetherdrift cards whose behaviour the snapshot can't prove:
 *
 * | Card | Under test |
 * |---|---|
 * | Point the Way | a library search whose "up to X" ceiling is a *dynamic* amount — the controller's speed |
 * | Gastal Raider | a targeted reveal-and-discard filtered to instants and sorceries, including the empty case |
 * | Howler's Heavy | the "when you cycle this card" trigger targeting from the graveyard |
 *
 * Swiftwing Assailant and Goblin Surveyor are deliberately absent: both are the plain
 * `maxSpeed { }` shapes already pinned down by Nesting Bot (gated statics) and Loxodon Surveyor
 * (a gated graveyard ability) in [AetherdriftDriftersScenarioTest].
 */
class AetherdriftRoadCrewScenarioTest : ScenarioTestBase() {

    init {
        context("Point the Way") {

            test("at starting speed the search ceiling is one basic land, and it enters tapped") {
                val game = pointTheWayGame()
                withClue("Start your engines! sets an unset speed to 1 as a state-based action") {
                    game.state.speed(game.player1Id) shouldBe Speed.STARTING
                }

                game.activatePointTheWay()
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("X is the controller's speed, so exactly one Island may be found") {
                    decision.maxSelections shouldBe 1
                }

                game.selectCards(decision.options.take(1))
                game.resolveStack()

                val islands = game.findAllPermanents("Island")
                withClue("One Island fetched onto the battlefield") { islands.size shouldBe 1 }
                withClue("Put onto the battlefield tapped") {
                    game.state.getEntity(islands.single())!!.get<TappedComponent>().shouldNotBeNull()
                }
                withClue("The enchantment was sacrificed as a cost") {
                    game.isInGraveyard(1, "Point the Way") shouldBe true
                }
            }

            test("at max speed the ceiling rises to four, and finding fewer is still legal") {
                val game = pointTheWayGame()
                game.state = SpeedService.set(game.state, game.player1Id, Speed.MAX, "test").first

                game.activatePointTheWay()
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("Speed 4 means 'up to four basic land cards'") {
                    decision.maxSelections shouldBe 4
                }
                withClue("'Up to' — the search may find nothing at all") {
                    decision.minSelections shouldBe 0
                }

                game.selectCards(decision.options.take(2))
                game.resolveStack()

                withClue("Only the two chosen Islands arrive, both tapped") {
                    val islands = game.findAllPermanents("Island")
                    islands.size shouldBe 2
                    islands.all { game.state.getEntity(it)?.get<TappedComponent>() != null } shouldBe true
                }
            }
        }

        context("Gastal Raider") {

            test("you pick from the opponent's instants and sorceries only, and they discard it") {
                val game = raiderGame(opponentHand = listOf("Lightning Strike", "Duress", "Grizzly Bears"))

                game.castRaider()
                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("The Bears are neither instant nor sorcery, so they aren't offered") {
                    decision.options.map { game.cardName(it) }.sortedBy { it } shouldBe
                        listOf("Duress", "Lightning Strike")
                }

                val duress = decision.options.single { game.cardName(it) == "Duress" }
                game.selectCards(listOf(duress))
                game.resolveStack()

                withClue("Only the card *you* chose leaves the targeted opponent's hand") {
                    game.isInGraveyard(2, "Duress") shouldBe true
                    game.isInHand(2, "Lightning Strike") shouldBe true
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }

            test("a single matching card is taken without a pointless prompt") {
                val game = raiderGame(opponentHand = listOf("Lightning Strike", "Grizzly Bears"))

                game.castRaider()
                withClue("One eligible card and one required pick — there is nothing to decide") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()

                withClue("It is still a real discard, just an unprompted one") {
                    game.isInGraveyard(2, "Lightning Strike") shouldBe true
                    game.isInHand(2, "Grizzly Bears") shouldBe true
                }
            }

            test("an opponent holding no instant or sorcery reveals and discards nothing") {
                val game = raiderGame(opponentHand = listOf("Grizzly Bears", "Hill Giant"))

                game.castRaider()
                withClue("Nothing matches the filter, so there is no choice to make") {
                    game.hasPendingDecision() shouldBe false
                }
                game.resolveStack()

                withClue("The trigger resolves as a no-op rather than forcing an illegal discard") {
                    game.handSize(2) shouldBe 2
                    game.graveyardSize(2) shouldBe 0
                }
                withClue("The Raider itself still resolved onto the battlefield") {
                    game.isOnBattlefield("Gastal Raider") shouldBe true
                }
            }
        }

        context("Howler's Heavy") {

            test("cycling it shrinks a targeted opposing creature by three power") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Howler's Heavy")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Island", 2)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val cycle = game.cycleCard(1, "Howler's Heavy")
                withClue("Cycling failed: ${cycle.error}") { cycle.error shouldBe null }
                game.autoPayIfAsked()

                // The trigger targets on the way to the stack, from the graveyard.
                if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(listOf(giant))
                game.resolveStack()

                withClue("3/3 becomes 0/3 — power only") {
                    game.state.projectedState.getPower(giant) shouldBe 0
                    game.state.projectedState.getToughness(giant) shouldBe 3
                }
                withClue("Cycling still discarded the card and drew a replacement") {
                    game.isInGraveyard(1, "Howler's Heavy") shouldBe true
                }
            }
        }
    }

    /**
     * Player 1's precombat main with Point the Way out, four Forests for the {3}{G} activation and
     * six Islands in the library to find. Built in the upkeep step so the CR 704.5z
     * start-your-engines state-based action has a step change to run on.
     */
    private fun pointTheWayGame(): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Point the Way")
            .withLandsOnBattlefield(1, "Forest", 4)
        repeat(6) { builder.withCardInLibrary(1, "Island") }
        repeat(6) { builder.withCardInLibrary(2, "Grizzly Bears") }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /** Activate "{3}{G}, Sacrifice this enchantment: …" and resolve up to the search decision. */
    private fun TestGame.activatePointTheWay() {
        val abilityId = cardRegistry.getCard("Point the Way")!!.script.activatedAbilities.single().id
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Point the Way")!!,
                abilityId = abilityId
            )
        )
        withClue("Activation failed: ${result.error}") { result.error shouldBe null }
        autoPayIfAsked()
        resolveStack()
    }

    /** Player 1 holds Gastal Raider with the mana to cast it; player 2 holds [opponentHand]. */
    private fun raiderGame(opponentHand: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Gastal Raider")
            .withLandsOnBattlefield(1, "Swamp", 3)
        opponentHand.forEach { builder.withCardInHand(2, it) }
        stockLibraries(builder)
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        return game
    }

    /** Cast the Raider, let it resolve, and put its targeted enters trigger onto the stack. */
    private fun TestGame.castRaider() {
        val cast = castSpell(1, "Gastal Raider")
        withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
        autoPayIfAsked()
        resolveStack()
        if (getPendingDecision() is ChooseTargetsDecision) selectTargets(listOf(player2Id))
        resolveStack()
    }

    private fun stockLibraries(builder: ScenarioBuilder) {
        repeat(8) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
    }

    private fun TestGame.cardName(id: EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }
}
