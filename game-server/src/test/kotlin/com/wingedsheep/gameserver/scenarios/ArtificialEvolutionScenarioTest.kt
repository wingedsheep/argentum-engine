package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Artificial Evolution.
 *
 * Card reference:
 * - Artificial Evolution ({U}): Instant
 *   "Change the text of target spell or permanent by replacing all instances of one
 *   creature type with another. The new creature type can't be Wall.
 *   (This effect lasts indefinitely.)"
 */
class ArtificialEvolutionScenarioTest : ScenarioTestBase() {

    /**
     * Helper to choose a creature type from a ChooseOptionDecision by name.
     */
    private fun ScenarioTestBase.TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Artificial Evolution text replacement") {

            test("changes creature subtype on permanent") {
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution targeting Wellwisher
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.castSpell(1, "Artificial Evolution", wellwisher)
                game.resolveStack()

                // Decision 1: Choose FROM creature type (Elf)
                game.chooseCreatureType("Elf")

                // Decision 2: Choose TO creature type (Goblin)
                game.chooseCreatureType("Goblin")

                // Wellwisher should now have TextReplacementComponent
                val entity = game.state.getEntity(wellwisher)
                entity.shouldNotBeNull()
                val textReplacement = entity.get<TextReplacementComponent>()
                textReplacement.shouldNotBeNull()
                withClue("Should have Elf→Goblin replacement") {
                    textReplacement.replacements.size shouldBe 1
                    textReplacement.replacements[0].fromWord shouldBe "Elf"
                    textReplacement.replacements[0].toWord shouldBe "Goblin"
                }
            }

            test("changed Wellwisher counts Goblins instead of Elves for life gain") {
                // Setup: Wellwisher + 2 Goblins on battlefield
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardOnBattlefield(1, "Raging Goblin")
                    .withCardOnBattlefield(2, "Goblin Bully")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLifeTotal(1, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Wellwisher: Elf → Goblin
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.castSpell(1, "Artificial Evolution", wellwisher)
                game.resolveStack()
                game.chooseCreatureType("Elf")
                game.chooseCreatureType("Goblin")

                // Now activate Wellwisher's ability (tap: gain 1 life per Goblin)
                val cardDef = cardRegistry.getCard("Wellwisher")!!
                val ability = cardDef.script.activatedAbilities.first()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = wellwisher,
                        abilityId = ability.id
                    )
                )
                game.resolveStack()

                // Wellwisher's ability now counts Goblins: 2 Goblins on battlefield = 2 life
                // (Wellwisher itself is an Elf, not a Goblin, so it doesn't count)
                withClue("Should gain 2 life (2 Goblins on battlefield)") {
                    game.getLifeTotal(1) shouldBe 12
                }
            }

            test("changed Elvish Vanguard triggers on Beast entering instead of Elf") {
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Vanguard")
                    .withCardInHand(1, "Artificial Evolution")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Elvish Vanguard: Elf → Bear
                val vanguard = game.findPermanent("Elvish Vanguard")!!
                game.castSpell(1, "Artificial Evolution", vanguard)
                game.resolveStack()
                game.chooseCreatureType("Elf")
                game.chooseCreatureType("Bear")

                // Now cast Grizzly Bears (a Bear creature)
                game.castSpell(1, "Grizzly Bears")
                game.resolveStack()

                // Elvish Vanguard should have triggered from a Bear entering
                // and gotten a +1/+1 counter
                val clientState = game.getClientState(1)
                val vanguardCard = clientState.cards.values.find { it.name == "Elvish Vanguard" }
                vanguardCard.shouldNotBeNull()
                withClue("Elvish Vanguard should have gotten a +1/+1 counter from Bear entering") {
                    vanguardCard.counters[CounterType.PLUS_ONE_PLUS_ONE] shouldBe 1
                }
            }

            test("Wall can be chosen as FROM type but not as TO type") {
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wellwisher = game.findPermanent("Wellwisher")!!
                game.castSpell(1, "Artificial Evolution", wellwisher)
                game.resolveStack()

                // Step 1: FROM type options should include Wall
                val fromDecision = game.getPendingDecision()
                fromDecision.shouldNotBeNull()
                fromDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                val fromOptions = (fromDecision as ChooseOptionDecision).options
                withClue("Wall should be available as the FROM creature type") {
                    fromOptions shouldContain "Wall"
                }

                // Choose Wall as FROM type
                game.chooseCreatureType("Wall")

                // Step 2: TO type options must NOT include Wall
                // ("The new creature type can't be Wall.")
                val toDecision = game.getPendingDecision()
                toDecision.shouldNotBeNull()
                toDecision.shouldBeInstanceOf<ChooseOptionDecision>()
                val toOptions = (toDecision as ChooseOptionDecision).options
                withClue("Wall must not be available as the TO creature type (card restriction)") {
                    toOptions shouldNotContain "Wall"
                }
            }

            test("projected subtypes reflect text replacement") {
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial subtypes
                var clientState = game.getClientState(1)
                var wellwisherCard = clientState.cards.values.find { it.name == "Wellwisher" }
                wellwisherCard.shouldNotBeNull()
                withClue("Wellwisher should initially have Elf subtype") {
                    wellwisherCard.subtypes shouldContain "Elf"
                }

                // Cast Artificial Evolution: Elf → Goblin
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.castSpell(1, "Artificial Evolution", wellwisher)
                game.resolveStack()
                game.chooseCreatureType("Elf")
                game.chooseCreatureType("Goblin")

                // Verify projected subtypes changed
                clientState = game.getClientState(1)
                wellwisherCard = clientState.cards.values.find { it.name == "Wellwisher" }
                wellwisherCard.shouldNotBeNull()
                withClue("Wellwisher should now have Goblin subtype instead of Elf") {
                    wellwisherCard.subtypes shouldContain "Goblin"
                    wellwisherCard.subtypes shouldNotContain "Elf"
                }
            }

            test("text replacement persists indefinitely") {
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Wellwisher")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution: Elf → Goblin
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.castSpell(1, "Artificial Evolution", wellwisher)
                game.resolveStack()
                game.chooseCreatureType("Elf")
                game.chooseCreatureType("Goblin")

                // Advance to end of turn and back
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                // TextReplacementComponent should still be on the entity
                val entity = game.state.getEntity(wellwisher)
                entity.shouldNotBeNull()
                val textReplacement = entity.get<TextReplacementComponent>()
                withClue("Text replacement should persist across turns") {
                    textReplacement.shouldNotBeNull()
                    textReplacement.replacements.size shouldBe 1
                }
            }
        }
    }
}
