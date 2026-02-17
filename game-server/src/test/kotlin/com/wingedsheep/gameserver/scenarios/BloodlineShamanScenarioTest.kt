package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Bloodline Shaman.
 *
 * Card reference:
 * - Bloodline Shaman ({1}{G}): Creature — Elf Wizard Shaman 1/1
 *   "{T}: Choose a creature type. Reveal the top card of your library.
 *   If that card is a creature card of the chosen type, put it into your hand.
 *   Otherwise, put it into your graveyard."
 */
class BloodlineShamanScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.activateBloodlineShaman() {
        val shamanId = findPermanent("Bloodline Shaman")!!
        val cardDef = cardRegistry.getCard("Bloodline Shaman")!!
        val ability = cardDef.script.activatedAbilities.first()
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = shamanId,
                abilityId = ability.id
            )
        )
        withClue("Ability should activate successfully: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Bloodline Shaman - choose creature type, reveal top card") {

            test("matching creature type - card goes to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodline Shaman")
                    .withCardInLibrary(1, "Elvish Warrior") // Elf Warrior creature on top
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                // Activate the tap ability
                game.activateBloodlineShaman()

                // Resolve the ability on the stack
                game.resolveStack()

                // Should be asked to choose a creature type
                game.chooseCreatureType("Elf")

                // Elvish Warrior is an Elf, so it goes to hand
                withClue("Elvish Warrior should be in hand") {
                    game.isInHand(1, "Elvish Warrior") shouldBe true
                }
                withClue("Hand size should increase by 1") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }
                withClue("Library should be empty") {
                    game.librarySize(1) shouldBe 0
                }
            }

            test("non-matching creature type - card goes to graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodline Shaman")
                    .withCardInLibrary(1, "Elvish Warrior") // Elf Warrior creature on top
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                game.activateBloodlineShaman()
                game.resolveStack()

                // Choose Goblin - Elvish Warrior is not a Goblin
                game.chooseCreatureType("Goblin")

                withClue("Elvish Warrior should be in graveyard") {
                    game.isInGraveyard(1, "Elvish Warrior") shouldBe true
                }
                withClue("Hand size should not change") {
                    game.handSize(1) shouldBe initialHandSize
                }
                withClue("Library should be empty") {
                    game.librarySize(1) shouldBe 0
                }
            }

            test("non-creature card - always goes to graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodline Shaman")
                    .withCardInLibrary(1, "Naturalize") // Instant, not a creature
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                game.activateBloodlineShaman()
                game.resolveStack()

                // Choose Elf - Naturalize is not a creature at all
                game.chooseCreatureType("Elf")

                withClue("Naturalize should be in graveyard") {
                    game.isInGraveyard(1, "Naturalize") shouldBe true
                }
                withClue("Hand size should not change") {
                    game.handSize(1) shouldBe initialHandSize
                }
            }

            test("empty library - nothing happens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodline Shaman")
                    // No cards in library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)

                game.activateBloodlineShaman()
                game.resolveStack()

                // Still asks for creature type choice (pipeline always does this step),
                // but with empty library nothing is revealed or moved
                game.chooseCreatureType("Elf")

                withClue("Hand size should not change") {
                    game.handSize(1) shouldBe initialHandSize
                }
            }

            test("creature of matching subtype among multiple subtypes - goes to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bloodline Shaman")
                    .withCardInLibrary(1, "Bloodline Shaman") // Elf Wizard Shaman - has multiple subtypes
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateBloodlineShaman()
                game.resolveStack()

                // Choose Wizard - Bloodline Shaman is an Elf Wizard Shaman
                game.chooseCreatureType("Wizard")

                withClue("Bloodline Shaman should be in hand") {
                    game.isInHand(1, "Bloodline Shaman") shouldBe true
                }
            }
        }
    }
}
