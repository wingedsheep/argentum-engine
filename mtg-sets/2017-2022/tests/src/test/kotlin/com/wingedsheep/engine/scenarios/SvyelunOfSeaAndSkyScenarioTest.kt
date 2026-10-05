package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ManaSourcesSelectedResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Svyelun of Sea and Sky (MH2 #69) — {1}{U}{U} Legendary Creature — Merfolk God, 3/4.
 *
 *   Svyelun has indestructible as long as you control at least two other Merfolk.
 *   Whenever Svyelun attacks, draw a card.
 *   Other Merfolk you control have ward {1}.
 *
 * "Island Walker" (TestCards) is a 2/2 Merfolk; "Doom Blade" (TestCards) is a {1}{B} sorcery
 * that destroys target nonblack creature.
 */
class SvyelunOfSeaAndSkyScenarioTest : ScenarioTestBase() {

    init {
        context("Svyelun of Sea and Sky — conditional indestructible") {

            test("with two other Merfolk, Svyelun is indestructible and survives a destroy spell") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val svyelun = game.findPermanent("Svyelun of Sea and Sky")!!
                game.state.projectedState.hasKeyword(svyelun, Keyword.INDESTRUCTIBLE) shouldBe true

                game.castSpell(2, "Doom Blade", svyelun).error shouldBe null
                game.resolveStack()

                withClue("Svyelun is indestructible, so the destroy does nothing") {
                    game.isOnBattlefield("Svyelun of Sea and Sky") shouldBe true
                }
            }

            test("with only one other Merfolk, Svyelun is not indestructible and has no ward of its own") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardInHand(2, "Doom Blade")
                    // Exactly the spell's cost: had Svyelun ward {1}, the spell would be countered.
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val svyelun = game.findPermanent("Svyelun of Sea and Sky")!!
                game.state.projectedState.hasKeyword(svyelun, Keyword.INDESTRUCTIBLE) shouldBe false
                game.state.projectedState.hasKeyword(svyelun, Keyword.WARD) shouldBe false

                game.castSpell(2, "Doom Blade", svyelun).error shouldBe null
                game.resolveStack()

                withClue("Svyelun counts only *other* Merfolk, so one isn't enough") {
                    game.isOnBattlefield("Svyelun of Sea and Sky") shouldBe false
                    game.isInGraveyard(1, "Svyelun of Sea and Sky") shouldBe true
                }
            }

            test("indestructible is lost once a Merfolk leaves") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val svyelun = game.findPermanent("Svyelun of Sea and Sky")!!
                val walker = game.findPermanents("Island Walker").first()

                // Pay {1}{B} plus ward {1} to destroy one of the Merfolk.
                game.castSpell(2, "Doom Blade", walker).error shouldBe null
                game.resolveStack()

                // Ward {1} resolves first and asks the caster to pay.
                val ward = game.getPendingDecision()
                ward.shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitDecision(ManaSourcesSelectedResponse(ward.id, autoPay = true)).error shouldBe null
                game.resolveStack()

                game.findPermanents("Island Walker").size shouldBe 1
                game.state.projectedState.hasKeyword(svyelun, Keyword.INDESTRUCTIBLE) shouldBe false
            }
        }

        context("Svyelun of Sea and Sky — attack trigger") {

            test("whenever Svyelun attacks, its controller draws a card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky", summoningSickness = false)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Svyelun of Sea and Sky" to 2)).error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
            }
        }

        context("Svyelun of Sea and Sky — other Merfolk have ward {1}") {

            test("an opponent's spell targeting another Merfolk is countered when ward can't be paid") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val walker = game.findPermanent("Island Walker")!!
                game.state.projectedState.hasKeyword(walker, Keyword.WARD) shouldBe true

                game.castSpell(2, "Doom Blade", walker).error shouldBe null
                game.resolveStack()

                withClue("ward {1} counters the spell; the Merfolk survives") {
                    game.isOnBattlefield("Island Walker") shouldBe true
                    game.isInGraveyard(2, "Doom Blade") shouldBe true
                }
            }

            test("without Svyelun the Merfolk has no ward") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Island Walker")
                    .withCardInHand(2, "Doom Blade")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val walker = game.findPermanent("Island Walker")!!
                game.state.projectedState.hasKeyword(walker, Keyword.WARD) shouldBe false
                game.castSpell(2, "Doom Blade", walker).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Island Walker") shouldBe false
            }

            test("the opponent's Merfolk don't get ward") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Svyelun of Sea and Sky")
                    .withCardOnBattlefield(2, "Island Walker")
                    .build()

                val walker = game.findPermanent("Island Walker")!!
                game.state.projectedState.hasKeyword(walker, Keyword.WARD) shouldBe false
            }
        }
    }
}
