package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Spectacular Pileup (DFT #29).
 *
 * "All creatures and Vehicles lose indestructible until end of turn, then destroy all creatures
 *  and Vehicles.
 *  Cycling {2}"
 *
 * The load-bearing case is the **uncrewed Vehicle**. A Vehicle that hasn't been crewed is an
 * artifact, not a creature, so it is exactly the permanent a creature-scoped keyword-removal would
 * skip — and `RemoveKeywordExecutor` used to reject non-creature targets outright, which silently
 * exempted the half of the board this card names explicitly. These tests pin both halves: the
 * keyword really comes off an uncrewed indestructible Vehicle, and the sweeper then kills it.
 */
class SpectacularPileupScenarioTest : ScenarioTestBase() {

    // An intrinsically indestructible Vehicle. Uncrewed it is an artifact and *not* a creature,
    // which is the case the executor's old creature-only guard broke.
    private val indestructibleVehicle = card("Indestructible Test Vehicle") {
        manaCost = "{3}"
        typeLine = "Artifact — Vehicle"
        power = 4
        toughness = 4
        keywords(Keyword.INDESTRUCTIBLE)
        keywordAbility(KeywordAbility.crew(1))
    }

    private val indestructibleBear = card("Indestructible Test Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        keywords(Keyword.INDESTRUCTIBLE)
    }

    private val plainVehicle = card("Plain Test Vehicle") {
        manaCost = "{2}"
        typeLine = "Artifact — Vehicle"
        power = 3
        toughness = 3
        keywordAbility(KeywordAbility.crew(1))
    }

    init {
        cardRegistry.register(indestructibleVehicle)
        cardRegistry.register(indestructibleBear)
        cardRegistry.register(plainVehicle)

        context("Spectacular Pileup") {

            test("strips indestructible from creatures and uncrewed Vehicles, then destroys them all") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spectacular Pileup")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Indestructible Test Vehicle")
                    .withCardOnBattlefield(1, "Indestructible Test Bear")
                    .withCardOnBattlefield(2, "Plain Test Vehicle")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Spectacular Pileup")
                withClue("Casting Spectacular Pileup should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                withClue("An uncrewed indestructible Vehicle loses indestructible and is destroyed") {
                    game.isOnBattlefield("Indestructible Test Vehicle") shouldBe false
                    game.isInGraveyard(1, "Indestructible Test Vehicle") shouldBe true
                }
                withClue("An indestructible creature loses indestructible and is destroyed") {
                    game.isOnBattlefield("Indestructible Test Bear") shouldBe false
                    game.isInGraveyard(1, "Indestructible Test Bear") shouldBe true
                }
                withClue("Ordinary creatures and Vehicles are swept too, on both sides") {
                    game.isInGraveyard(2, "Plain Test Vehicle") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("leaves noncreature, non-Vehicle permanents alone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spectacular Pileup")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Spectacular Pileup").error shouldBe null
                game.resolveStack()

                withClue("The creature is gone but the lands that paid for it are untouched") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Plains") shouldBe true
                }
            }

            test("can be cycled for {2} instead of cast") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Spectacular Pileup")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                val cycle = game.cycleCard(1, "Spectacular Pileup")
                withClue("Cycling Spectacular Pileup should succeed: ${cycle.error}") {
                    cycle.error shouldBe null
                }
                game.resolveStack()

                withClue("Cycling discards the Pileup and draws one — net hand size unchanged") {
                    game.handSize(1) shouldBe handBefore
                    game.isInGraveyard(1, "Spectacular Pileup") shouldBe true
                }
                withClue("Cycling does not sweep the board") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
