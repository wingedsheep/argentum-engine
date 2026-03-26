package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.matchers.shouldBe

class RepelCalamityScenarioTest : ScenarioTestBase() {

    private val bigPowerCreature = CardDefinition.creature(
        name = "Big Power Creature",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype("Beast")),
        power = 5, toughness = 2
    )

    private val bigToughnessCreature = CardDefinition.creature(
        name = "Big Toughness Creature",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Wall")),
        power = 1, toughness = 5
    )

    private val smallCreature = CardDefinition.creature(
        name = "Small Creature",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2, toughness = 2
    )

    init {
        cardRegistry.register(bigPowerCreature)
        cardRegistry.register(bigToughnessCreature)
        cardRegistry.register(smallCreature)

        context("Repel Calamity") {
            test("destroys creature with power 4 or greater") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Repel Calamity")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Big Power Creature")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Big Power Creature")!!
                game.castSpell(1, "Repel Calamity", target)
                game.resolveStack()

                game.isInGraveyard(2, "Big Power Creature") shouldBe true
                game.isOnBattlefield("Big Power Creature") shouldBe false
            }

            test("destroys creature with toughness 4 or greater but low power") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Repel Calamity")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Big Toughness Creature")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val target = game.findPermanent("Big Toughness Creature")!!
                game.castSpell(1, "Repel Calamity", target)
                game.resolveStack()

                game.isInGraveyard(2, "Big Toughness Creature") shouldBe true
            }

            test("cannot target creature with both power and toughness below 4") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Repel Calamity")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(2, "Small Creature")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Small creature should not be a legal target - casting should fail
                val smallCreatureId = game.findPermanent("Small Creature")!!
                val result = game.castSpell(1, "Repel Calamity", smallCreatureId)
                (result.error != null) shouldBe true
            }
        }
    }
}
