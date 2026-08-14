package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Winter, Cursed Rider (DFT #228).
 *
 * Winter, Cursed Rider {U}{B} — Legendary Creature — Human Warlock 3/2
 * Ward—Pay 2 life.
 * Artifacts you control have "Ward—Pay 2 life."
 * Exhaust — {2}{U}{B}, {T}, Exile X artifact cards from your graveyard: Each other nonartifact
 * creature gets -X/-X until end of turn.
 *
 * The load-bearing claim is the **X binding**: the printed cost has no `{X}` mana symbol, so X is
 * bound purely by the `ExileXFromGraveyard` cost — X *is* how many graveyard cards you choose to
 * exile. So activating pauses for one card selection over the artifact cards in your graveyard, and
 * the size of that selection becomes X. Nonartifact cards must not be offered, and a zero selection
 * has to settle as X = 0 rather than re-prompting.
 */
class WinterCursedRiderScenarioTest : ScenarioTestBase() {

    private val exhaustAbilityId
        get() = cardRegistry.getCard("Winter, Cursed Rider")!!.script.activatedAbilities[0].id

    init {
        context("Winter, Cursed Rider") {

            test("only artifact cards are offered for the exile cost, and the count becomes X") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Winter, Cursed Rider")
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3 nonartifact, yours
                    // Two artifact cards — the only legal fodder for the exhaust cost …
                    .withCardInGraveyard(1, "Guidelight Matrix")
                    .withCardInGraveyard(1, "Guidelight Matrix")
                    // … alongside three nonartifact cards that must not be offered.
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("five cards in the graveyard, only two of them artifacts") {
                    game.graveyardSize(1) shouldBe 5
                }
                val artifactCards = game.findCardsInGraveyard(1, "Guidelight Matrix")
                artifactCards.size shouldBe 2

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Winter, Cursed Rider")!!,
                        abilityId = exhaustAbilityId
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("only the artifact cards are selectable — X can't exceed them") {
                    decision.options.toSet() shouldBe artifactCards.toSet()
                    decision.maxSelections shouldBe 2
                }
                withClue("exiling nothing is a legal answer (X = 0)") {
                    decision.minSelections shouldBe 0
                }

                game.selectCards(artifactCards).error shouldBe null
                game.resolveStack()

                withClue("both chosen artifact cards went to exile, X = 2") {
                    game.isInExile(1, "Guidelight Matrix") shouldBe true
                    game.findCardsInGraveyard(1, "Guidelight Matrix").size shouldBe 0
                }
                withClue("-2/-2 from a 3/3 leaves a 1/1") {
                    game.state.projectedState.getPower(game.findPermanent("Centaur Courser")!!) shouldBe 1
                }
            }

            test("the sweep is -X/-X to each OTHER nonartifact creature, and Winter is spared") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Winter, Cursed Rider")
                    .withCardOnBattlefield(1, "Centaur Courser") // 3/3 nonartifact, yours
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 nonartifact, theirs
                    .withCardOnBattlefield(2, "Marshals' Pathcruiser") // artifact Vehicle, theirs
                    .withCardInGraveyard(1, "Guidelight Matrix")
                    .withCardInGraveyard(1, "Guidelight Matrix")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val winter = game.findPermanent("Winter, Cursed Rider")!!
                val courser = game.findPermanent("Centaur Courser")!!
                val theirVehicle = game.findPermanent("Marshals' Pathcruiser")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = winter,
                        abilityId = exhaustAbilityId,
                        xValue = 2,
                        costPayment = AdditionalCostPayment(
                            exiledCards = game.findCardsInGraveyard(1, "Guidelight Matrix")
                        )
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                withClue("both artifact cards left the graveyard for exile") {
                    game.graveyardSize(1) shouldBe 0
                    game.isInExile(1, "Guidelight Matrix") shouldBe true
                }

                val projected = game.state.projectedState
                withClue("your own nonartifact creature is hit too — the text isn't 'you control'") {
                    projected.getPower(courser) shouldBe 1
                    projected.getToughness(courser) shouldBe 1
                }
                withClue("the opponent's nonartifact creature dies to -2/-2 (2/2)") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("an artifact Vehicle is not a nonartifact creature — untouched") {
                    game.isOnBattlefield("Marshals' Pathcruiser") shouldBe true
                    projected.getPower(theirVehicle) shouldBe 6
                }
                withClue("Winter itself is a nonartifact creature but 'each other' excludes it") {
                    projected.getPower(winter) shouldBe 3
                    projected.getToughness(winter) shouldBe 2
                }
            }

            test("selecting no cards settles as X = 0 instead of re-prompting") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Winter, Cursed Rider")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withCardInGraveyard(1, "Guidelight Matrix")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Winter, Cursed Rider")!!,
                        abilityId = exhaustAbilityId
                    )
                ).error shouldBe null

                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                withClue("nothing exiled, and -0/-0 leaves the board alone") {
                    game.isInExile(1, "Guidelight Matrix") shouldBe false
                    game.state.projectedState.getPower(game.findPermanent("Centaur Courser")!!) shouldBe 3
                }
                withClue("the decision is done — the engine did not ask again") {
                    game.hasPendingDecision() shouldBe false
                }
            }

            test("both instances of ward exist: Winter's own and the one it grants artifacts") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Winter, Cursed Rider")
                    .withCardOnBattlefield(1, "Guidelight Matrix")
                    .withCardOnBattlefield(2, "Guidelight Matrix")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val winter = game.findPermanent("Winter, Cursed Rider")!!
                val yourMatrix = game.findAllPermanents("Guidelight Matrix")
                    .single { game.state.projectedState.getController(it) == game.player1Id }
                val theirMatrix = game.findAllPermanents("Guidelight Matrix")
                    .single { game.state.projectedState.getController(it) == game.player2Id }

                val projected = game.state.projectedState
                withClue("Winter has its printed ward") {
                    projected.hasKeyword(winter, Keyword.WARD) shouldBe true
                }
                withClue("your artifacts pick ward up from the lord") {
                    projected.hasKeyword(yourMatrix, Keyword.WARD) shouldBe true
                }
                withClue("the lord is scoped to artifacts YOU control") {
                    projected.hasKeyword(theirMatrix, Keyword.WARD) shouldBe false
                }
            }
        }
    }
}
