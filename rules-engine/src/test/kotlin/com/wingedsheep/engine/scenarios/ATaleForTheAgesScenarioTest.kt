package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for A Tale for the Ages. */
class ATaleForTheAgesScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("A Tale for the Ages — 'enchanted creatures you control get +2/+2'") {
            test("buffs your enchanted creature, and leaves your unenchanted one alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Redtooth Genealogist")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val elf = game.findPermanent("Redtooth Genealogist").shouldNotBeNull()

                withClue("the enchanted 2/2 Bears projects as 4/4") {
                    power(game, bears) shouldBe 4
                    toughness(game, bears) shouldBe 4
                }
                withClue("the unenchanted 2/3 Genealogist is untouched") {
                    power(game, elf) shouldBe 2
                    toughness(game, elf) shouldBe 3
                }
            }

            test("an Equipment attached is not 'enchanted' — the predicate is not IsModified") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Whispersilk Cloak", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("Whispersilk Cloak is Equipment, not an Aura — the Bears stays 2/2") {
                    power(game, bears) shouldBe 2
                    toughness(game, bears) shouldBe 2
                }
            }

            test("your creature enchanted by an opponent's Aura still gets the buff (CR 303.4)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    // Player 2 owns and controls the Pacifism; player 1 controls the creature.
                    .withCardAttachedTo(2, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("'enchanted' asks only that an Aura be attached, whoever controls it") {
                    power(game, bears) shouldBe 4
                    toughness(game, bears) shouldBe 4
                }
            }

            test("an opponent's enchanted creature is not buffed — 'you control' scopes the creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    // Player 1's own Aura, on player 2's creature.
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                withClue("the creature is the opponent's, so the anthem does not see it") {
                    power(game, bears) shouldBe 2
                    toughness(game, bears) shouldBe 2
                }
            }

            test("the buff falls away when the Aura leaves — it is projected, not applied once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "A Tale for the Ages")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withCardInHand(1, "Stonesplitter Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                power(game, bears) shouldBe 4

                // Destroy the Aura by bouncing it off: kill the host with X=4 damage, which takes
                // the Aura to the graveyard with it (CR 704.5m) and removes the anthem's subject.
                game.castXSpell(1, "Stonesplitter Bolt", xValue = 4, targetId = bears)
                    .error shouldBe null
                game.resolveStack()

                withClue("a 4/4 enchanted Bears dies to 4 damage, and its Aura follows it") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Pacifism") shouldBe true
                }
            }
        }
    }
}
