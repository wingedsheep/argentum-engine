package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.j25.cards.ApheliaViperWhisperer
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aphelia, Viper Whisperer (J25 #40).
 *
 * Pins the {4}{B} floating "until end of turn, whenever one or more Gorgons and/or Snakes you
 * control deal combat damage to a player, that player loses half their life, rounded up": it
 * fires once per player for the whole batch, reads the life total *after* combat damage (the
 * 2024-11-08 ruling's 12 → 9 → 4), and ignores damage from creatures that are neither Gorgons nor
 * Snakes. Also covers the attack trigger's optional {1}{B/G} payment for the deathtouch Snake.
 */
class ApheliaViperWhispererScenarioTest : ScenarioTestBase() {

    init {
        context("Aphelia, Viper Whisperer") {

            fun TestGame.activateHalving() {
                execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = findPermanent("Aphelia, Viper Whisperer")!!,
                        abilityId = ApheliaViperWhisperer.activatedAbilities[0].id
                    )
                ).error shouldBe null
                resolveStack()
            }

            test("Gorgon connecting halves the player's post-damage life, rounded up, once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aphelia, Viper Whisperer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLifeTotal(2, 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateHalving()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(
                    mapOf("Aphelia, Viper Whisperer" to 2, "Grizzly Bears" to 2)
                ).error shouldBe null
                // Declines the (now unaffordable) Snake payment and takes no blocks.
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("12 - 3 combat damage = 9, then loses ceil(9 / 2) = 5") {
                    game.getLifeTotal(2) shouldBe 4
                }
            }

            test("damage from creatures that are neither Gorgons nor Snakes doesn't trigger it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aphelia, Viper Whisperer")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withLifeTotal(2, 12)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateHalving()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                game.getLifeTotal(2) shouldBe 10
            }

            test("paying {1}{B/G} on attack creates a 1/1 black Snake with deathtouch") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aphelia, Viper Whisperer")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Aphelia, Viper Whisperer" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                val snakes = game.findPermanents("Snake Token")
                snakes.size shouldBe 1
                val snake = snakes.single()
                game.state.projectedState.getPower(snake) shouldBe 1
                game.state.projectedState.getToughness(snake) shouldBe 1
                game.state.projectedState.hasKeyword(snake, Keyword.DEATHTOUCH) shouldBe true
            }

            test("declining the payment creates no Snake") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Aphelia, Viper Whisperer")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Aphelia, Viper Whisperer" to 2)).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findPermanents("Snake Token").size shouldBe 0
            }
        }
    }
}
