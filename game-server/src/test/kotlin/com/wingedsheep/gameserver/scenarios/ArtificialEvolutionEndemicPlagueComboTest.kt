package com.wingedsheep.gameserver.scenarios

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
 * Combo scenario: Artificial Evolution + Endemic Plague
 *
 * The combo:
 * 1. Control a Festering Goblin (Zombie Goblin)
 * 2. Cast Artificial Evolution on Festering Goblin, changing "Goblin" to "Elf"
 *    (it becomes a Zombie Elf)
 * 3. Cast Endemic Plague, sacrificing the now-Elf Festering Goblin
 * 4. Festering Goblin's death trigger fires (target creature gets -1/-1)
 * 5. Endemic Plague destroys all creatures sharing a type with the sacrificed creature
 *    → all Elves are destroyed
 */
class ArtificialEvolutionEndemicPlagueComboTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.chooseCreatureType(typeName: String) {
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

    init {
        context("Artificial Evolution + Endemic Plague combo") {

            test("change Festering Goblin to Elf type, sacrifice with Endemic Plague to destroy all Elves") {
                val game = scenario()
                    .withPlayers("Combo Player", "Elf Tribal")
                    .withCardOnBattlefield(1, "Festering Goblin", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withCardInHand(1, "Endemic Plague")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Wellwisher", summoningSickness = false)
                    .withCardOnBattlefield(2, "Elvish Warrior", summoningSickness = false)
                    .withCardOnBattlefield(2, "Elvish Pioneer", summoningSickness = false)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify opponent has 3 Elves on the battlefield
                game.findPermanent("Wellwisher").shouldNotBeNull()
                game.findPermanent("Elvish Warrior").shouldNotBeNull()
                game.findPermanent("Elvish Pioneer").shouldNotBeNull()

                // Step 1: Cast Artificial Evolution targeting Festering Goblin
                val festeringGoblin = game.findPermanent("Festering Goblin")!!
                game.castSpell(1, "Artificial Evolution", festeringGoblin)
                game.resolveStack()

                // Choose FROM type: Goblin → TO type: Elf
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Elf")

                // Verify Festering Goblin is now a Zombie Elf
                val clientState = game.getClientState(1)
                val goblinCard = clientState.cards.values.find { it.name == "Festering Goblin" }
                goblinCard.shouldNotBeNull()
                withClue("Festering Goblin should now have Elf subtype") {
                    goblinCard.subtypes shouldBe listOf("Zombie", "Elf")
                }

                // Step 2: Cast Endemic Plague, sacrificing the now-Elf Festering Goblin
                // Festering Goblin's death trigger fires: "target creature gets -1/-1 until end of turn"
                game.castSpellWithAdditionalSacrifice(1, "Endemic Plague", "Festering Goblin")

                // Handle Festering Goblin's death trigger — select a target for -1/-1
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.selectTargets(listOf(wellwisher))

                // Resolve the stack: death trigger resolves first (LIFO), then Endemic Plague
                game.resolveStack()

                // All Elves should be destroyed by Endemic Plague
                withClue("Wellwisher should be destroyed (Elf)") {
                    game.isOnBattlefield("Wellwisher") shouldBe false
                }
                withClue("Elvish Warrior should be destroyed (Elf)") {
                    game.isOnBattlefield("Elvish Warrior") shouldBe false
                }
                withClue("Elvish Pioneer should be destroyed (Elf)") {
                    game.isOnBattlefield("Elvish Pioneer") shouldBe false
                }

                // Opponent should have 3 creatures in graveyard (all 3 Elves)
                withClue("Opponent should have 3 creatures in graveyard") {
                    game.graveyardSize(2) shouldBe 3
                }
            }

            test("non-Elf creatures survive the combo") {
                val game = scenario()
                    .withPlayers("Combo Player", "Mixed Tribal")
                    .withCardOnBattlefield(1, "Festering Goblin", summoningSickness = false)
                    .withCardInHand(1, "Artificial Evolution")
                    .withCardInHand(1, "Endemic Plague")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Wellwisher", summoningSickness = false)
                    .withCardOnBattlefield(2, "Glory Seeker", summoningSickness = false)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Artificial Evolution on Festering Goblin: Goblin → Elf
                val festeringGoblin = game.findPermanent("Festering Goblin")!!
                game.castSpell(1, "Artificial Evolution", festeringGoblin)
                game.resolveStack()
                game.chooseCreatureType("Goblin")
                game.chooseCreatureType("Elf")

                // Cast Endemic Plague, sacrificing Festering Goblin
                game.castSpellWithAdditionalSacrifice(1, "Endemic Plague", "Festering Goblin")

                // Handle Festering Goblin's death trigger — target an Elf for -1/-1
                val wellwisher = game.findPermanent("Wellwisher")!!
                game.selectTargets(listOf(wellwisher))

                // Resolve stack
                game.resolveStack()

                // Wellwisher (Elf) should be destroyed
                withClue("Wellwisher should be destroyed (Elf)") {
                    game.isOnBattlefield("Wellwisher") shouldBe false
                }

                // Glory Seeker (Human Soldier) should survive
                withClue("Glory Seeker should survive (not an Elf)") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }
        }
    }
}
