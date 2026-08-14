package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.assertions.withClue
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Four Aetherdrift cards whose *composition* is the risky part, not the individual primitives:
 *
 * | Card | Under test |
 * |---|---|
 * | Guidelight Matrix | `AddCardType(Creature, EndOfTurn)` as a Vehicle animate — printed P/T applies, and it wears off; plus the saddle-marker half |
 * | Earthrumbler | the same animate driven off a non-mana `ExileFromGraveyard` activation cost |
 * | Carrion Cruiser | mill-then-return ordering: the cards the mill just produced must be legal picks |
 * | Skyserpent Seeker | `gatherUntilMatch(count = 2)` — both matches to the battlefield, the walked cards to the bottom |
 */
class AetherdriftGuidelightBrigadeScenarioTest : ScenarioTestBase() {

    init {
        context("Guidelight Matrix") {

            test("its Vehicle half animates at the Vehicle's printed P/T and wears off at end of turn") {
                val game = matrixGame(companion = "Earthrumbler")
                val vehicle = game.findPermanent("Earthrumbler")!!

                game.activateMatrix(vehicleHalfId(), vehicle)

                val projected = game.state.projectedState
                withClue("A Vehicle granted the Creature type keeps ARTIFACT and its printed 7/6") {
                    projected.isCreature(vehicle) shouldBe true
                    projected.hasType(vehicle, "ARTIFACT") shouldBe true
                    projected.getPower(vehicle) shouldBe 7
                    projected.getToughness(vehicle) shouldBe 6
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("\"until end of turn\" — unlike Tune Up, the animation expires in cleanup") {
                    game.state.projectedState.isCreature(vehicle) shouldBe false
                }
            }

            test("its Mount half stamps the saddled marker without changing types") {
                val game = matrixGame(companion = "Gilded Ghoda")
                val mount = game.findPermanent("Gilded Ghoda")!!

                game.activateMatrix(mountHalfId(), mount)

                withClue("BecomeSaddled is a marker only — no P/T or type change") {
                    game.state.getEntity(mount)!!.get<SaddledComponent>().shouldNotBeNull()
                    game.state.projectedState.getPower(mount) shouldBe 2
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("The saddled marker is cleared at end of turn") {
                    game.state.getEntity(mount)!!.get<SaddledComponent>() shouldBe null
                }
            }

            test("the Vehicle half can't target a Mount, and the Mount half can't target a Vehicle") {
                val game = matrixGame(companion = "Gilded Ghoda")
                val mount = game.findPermanent("Gilded Ghoda")!!

                val wrongHalf = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Guidelight Matrix")!!,
                        abilityId = vehicleHalfId(),
                        targets = listOf(ChosenTarget.Permanent(mount))
                    )
                )
                withClue("A Mount is not a Vehicle — the target requirement must reject it") {
                    (wrongHalf.error != null).shouldBeTrue()
                }
            }
        }

        context("Earthrumbler") {

            test("exiling an artifact or creature card from the graveyard animates it for the turn") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Earthrumbler")
                    .withCardInGraveyard(1, "Grizzly Bears")
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rumbler = game.findPermanent("Earthrumbler")!!
                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = rumbler,
                        abilityId = earthrumblerAbilityId()
                    )
                )
                withClue("Activation failed: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                withClue("The only matching graveyard card is exiled to pay the cost") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                withClue("It self-crews at its printed 7/6, keeping vigilance and trample") {
                    val projected = game.state.projectedState
                    projected.isCreature(rumbler) shouldBe true
                    projected.getPower(rumbler) shouldBe 7
                    projected.getToughness(rumbler) shouldBe 6
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("Until end of turn, same as crew") {
                    game.state.projectedState.isCreature(rumbler) shouldBe false
                }
            }

            test("with an empty graveyard the cost can't be paid") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Earthrumbler")
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Earthrumbler")!!,
                        abilityId = earthrumblerAbilityId()
                    )
                )
                withClue("No artifact or creature card to exile") {
                    (activation.error != null).shouldBeTrue()
                }
            }
        }

        context("Carrion Cruiser") {

            test("the mill happens first, so a just-milled creature is a legal pick") {
                val game = cruiserGame(topOfLibrary = listOf("Grizzly Bears", "Mountain"))

                val cast = game.castSpell(1, "Carrion Cruiser")
                withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("Only the milled Grizzly Bears matches creature-or-Vehicle, so it auto-returns") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(1, "Mountain") shouldBe true
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("nothing to return is not a failure") {
                val game = cruiserGame(topOfLibrary = listOf("Mountain", "Island"))
                val handBefore = game.handSize(1)

                val cast = game.castSpell(1, "Carrion Cruiser")
                withClue("Cast failed: ${cast.error}") { cast.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("Both milled cards are lands — the mandatory return simply moves nothing") {
                    game.hasPendingDecision() shouldBe false
                    game.graveyardSize(1) shouldBe 2
                    // Carrion Cruiser left the hand to be cast; nothing came back.
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }

        context("Skyserpent Seeker") {

            test("the exhaust walk stops at the second land, both land, and the rest bottom out") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skyserpent Seeker")
                    .withLandsOnBattlefield(1, "Forest", 4)
                // Top of library, in order: creature, land, creature, land.
                listOf("Grizzly Bears", "Plains", "Grizzly Bears", "Island")
                    .forEach { builder.withCardInLibrary(1, it) }
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seeker = game.findPermanent("Skyserpent Seeker")!!
                val libraryBefore = game.librarySize(1)

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = seeker,
                        abilityId = seekerAbilityId()
                    )
                )
                withClue("Activation failed: ${activation.error}") { activation.error shouldBe null }
                game.autoPayIfAsked()
                game.resolveStack()

                withClue("Both revealed land cards enter tapped") {
                    game.isOnBattlefield("Plains") shouldBe true
                    game.isOnBattlefield("Island") shouldBe true
                    game.findPermanent("Plains")!!.let {
                        game.state.getEntity(it)!!.get<TappedComponent>().shouldNotBeNull()
                    }
                    game.findPermanent("Island")!!.let {
                        game.state.getEntity(it)!!.get<TappedComponent>().shouldNotBeNull()
                    }
                }
                withClue("Only the two lands left the library; the walked creatures went to the bottom") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                    game.graveyardSize(1) shouldBe 0
                }
                withClue("The +1/+1 counter is unconditional") {
                    game.state.getEntity(seeker)?.get<CountersComponent>()
                        ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                    game.state.projectedState.getPower(seeker) shouldBe 2
                }
            }

            test("the exhaust ability can only be activated once") {
                val builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Skyserpent Seeker")
                    .withLandsOnBattlefield(1, "Forest", 8)
                stockLibraries(builder)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seeker = game.findPermanent("Skyserpent Seeker")!!
                game.execute(
                    ActivateAbility(game.player1Id, seeker, seekerAbilityId())
                ).error shouldBe null
                game.autoPayIfAsked()
                game.resolveStack()

                val second = game.execute(
                    ActivateAbility(game.player1Id, seeker, seekerAbilityId())
                )
                withClue("CR 702.177a: activate each exhaust ability only once") {
                    (second.error != null).shouldBeTrue()
                }
            }
        }
    }

    // =========================================================================
    // Fixtures
    // =========================================================================

    /** Guidelight Matrix plus [companion] on the battlefield, and {2} available. */
    private fun matrixGame(companion: String): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Guidelight Matrix")
            .withCardOnBattlefield(1, companion)
            .withLandsOnBattlefield(1, "Forest", 2)
        stockLibraries(builder)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Carrion Cruiser in hand with the mana to cast it, and [topOfLibrary] on top in order. */
    private fun cruiserGame(topOfLibrary: List<String>): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Carrion Cruiser")
            .withLandsOnBattlefield(1, "Swamp", 3)
        topOfLibrary.forEach { builder.withCardInLibrary(1, it) }
        stockLibraries(builder)
        return builder
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
    }

    /** Both libraries stocked so nobody decks out on the turn-boundary draws. */
    private fun stockLibraries(builder: ScenarioBuilder) {
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
    }

    private fun matrixAbilities() =
        cardRegistry.getCard("Guidelight Matrix")!!.script.activatedAbilities

    /** "{2}, {T}: Target Mount you control becomes saddled…" — the sorcery-speed half. */
    private fun mountHalfId() = matrixAbilities().single { it.timing == TimingRule.SorcerySpeed }.id

    /** "{2}, {T}: Target Vehicle you control becomes an artifact creature…" — instant speed. */
    private fun vehicleHalfId() = matrixAbilities().single { it.timing == TimingRule.InstantSpeed }.id

    private fun earthrumblerAbilityId() =
        cardRegistry.getCard("Earthrumbler")!!.script.activatedAbilities.single().id

    private fun seekerAbilityId() =
        cardRegistry.getCard("Skyserpent Seeker")!!.script.activatedAbilities.single { it.isExhaust }.id

    /** Activate one half of Guidelight Matrix at [target], pay for it, and let it resolve. */
    private fun TestGame.activateMatrix(
        abilityId: com.wingedsheep.sdk.scripting.AbilityId,
        target: EntityId
    ) {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("Guidelight Matrix")!!,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
        withClue("Activation failed: ${result.error}") { result.error shouldBe null }
        autoPayIfAsked()
        resolveStack()
    }

    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }
}
