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

                // Wellwisher's ability now counts Goblins: 3 Goblins on battlefield = 3 life
                // (Wellwisher itself is now a Goblin Druid — text replacement changes type line too)
                withClue("Should gain 3 life (3 Goblins: Wellwisher + Raging Goblin + Goblin Bully)") {
                    game.getLifeTotal(1) shouldBe 13
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

            test("text replacement on spell changes effect when spell resolves") {
                // Sage Aven (Bird) is on the battlefield.
                // Player 2 casts Airborne Aid ("Draw a card for each Bird on the battlefield").
                // Player 1 responds with Artificial Evolution targeting Airborne Aid,
                // changing Bird → Ape. Since there are no Apes, Player 2 draws 0 cards.
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Sage Aven", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(2, "Airborne Aid")
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(2)

                // Player 2 casts Airborne Aid
                game.castSpell(2, "Airborne Aid")

                // Player 2 passes priority to Player 1
                game.passPriority()

                // Player 1 casts Artificial Evolution targeting Airborne Aid on the stack
                game.castSpellTargetingStackSpell(1, "Artificial Evolution", "Airborne Aid")

                // Resolve Artificial Evolution (top of stack)
                game.resolveStack()

                // Choose Bird as FROM type, Ape as TO type
                game.chooseCreatureType("Bird")
                game.chooseCreatureType("Ape")

                // Now Airborne Aid resolves — it now reads "Draw a card for each Ape"
                // There are no Apes on the battlefield, so Player 2 should draw 0 cards
                game.resolveStack()

                withClue("Player 2 should not have drawn any cards (no Apes on battlefield)") {
                    // Hand had Airborne Aid which was cast, so hand should be initialHandSize - 1
                    game.handSize(2) shouldBe initialHandSize - 1
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

            test("text-replacing a lord changes which creatures it affects") {
                // Aven Brigadier: "Other Bird creatures get +1/+1"
                // Storm Crow: 1/2 Bird (should normally get +1/+1 from Brigadier)
                // Grizzly Bears: 2/2 Bear (should not normally get +1/+1)
                // After Artificial Evolution on Brigadier changing Bird → Bear:
                //   - Storm Crow should NO LONGER get the Bird bonus
                //   - Grizzly Bears SHOULD now get the Bear bonus
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Storm Crow")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Before Artificial Evolution: Storm Crow should be 2/3 (1/2 + Bird bonus)
                var clientState = game.getClientState(1)
                var crowCard = clientState.cards.values.find { it.name == "Storm Crow" }
                crowCard.shouldNotBeNull()
                withClue("Storm Crow should be 2/3 with Bird lord bonus") {
                    crowCard.power shouldBe 2
                    crowCard.toughness shouldBe 3
                }

                // Grizzly Bears should be 2/2 (no bonus)
                var bearsCard = clientState.cards.values.find { it.name == "Grizzly Bears" }
                bearsCard.shouldNotBeNull()
                withClue("Grizzly Bears should be 2/2 without lord bonus") {
                    bearsCard.power shouldBe 2
                    bearsCard.toughness shouldBe 2
                }

                // Cast Artificial Evolution on Aven Brigadier: Bird → Bear
                val brigadier = game.findPermanent("Aven Brigadier")!!
                game.castSpell(1, "Artificial Evolution", brigadier)
                game.resolveStack()
                game.chooseCreatureType("Bird")
                game.chooseCreatureType("Bear")

                // After: Storm Crow should be 1/2 (lost Bird bonus, not a Soldier)
                clientState = game.getClientState(1)
                crowCard = clientState.cards.values.find { it.name == "Storm Crow" }
                crowCard.shouldNotBeNull()
                withClue("Storm Crow should be 1/2 after lord no longer gives Bird bonus") {
                    crowCard.power shouldBe 1
                    crowCard.toughness shouldBe 2
                }

                // Grizzly Bears should be 3/3 (now gets Bear bonus)
                bearsCard = clientState.cards.values.find { it.name == "Grizzly Bears" }
                bearsCard.shouldNotBeNull()
                withClue("Grizzly Bears should be 3/3 with new Bear lord bonus") {
                    bearsCard.power shouldBe 3
                    bearsCard.toughness shouldBe 3
                }
            }

            test("creature with text-changed subtype is seen by existing lord") {
                // Aven Brigadier: "Other Bird creatures get +1/+1"
                // Grizzly Bears: 2/2 Bear (not a Bird, no bonus)
                // After Artificial Evolution on Grizzly Bears changing Bear → Bird:
                //   - Grizzly Bears should get the Bird lord bonus from Brigadier
                val game = scenario()
                    .withPlayers("Blue Mage", "Opponent")
                    .withCardOnBattlefield(1, "Aven Brigadier")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Artificial Evolution")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Before: Grizzly Bears should be 2/2 (no bonus)
                var clientState = game.getClientState(1)
                var bearsCard = clientState.cards.values.find { it.name == "Grizzly Bears" }
                bearsCard.shouldNotBeNull()
                withClue("Grizzly Bears should be 2/2 without lord bonus") {
                    bearsCard.power shouldBe 2
                    bearsCard.toughness shouldBe 2
                }

                // Cast Artificial Evolution on Grizzly Bears: Bear → Bird
                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Artificial Evolution", bears)
                game.resolveStack()
                game.chooseCreatureType("Bear")
                game.chooseCreatureType("Bird")

                // After: Grizzly Bears is now a Bird, should get +1/+1 from Aven Brigadier
                clientState = game.getClientState(1)
                bearsCard = clientState.cards.values.find { it.name == "Grizzly Bears" }
                bearsCard.shouldNotBeNull()
                withClue("Grizzly Bears should be 3/3 as a Bird with lord bonus") {
                    bearsCard.power shouldBe 3
                    bearsCard.toughness shouldBe 3
                }
            }
        }
    }
}
