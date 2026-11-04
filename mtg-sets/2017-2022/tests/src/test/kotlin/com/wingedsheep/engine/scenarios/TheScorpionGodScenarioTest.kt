package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.TheScorpionGod
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Scorpion God (HOU #133).
 *
 * Whenever a creature with a -1/-1 counter on it dies, draw a card.
 * {1}{B}{R}: Put a -1/-1 counter on another target creature.
 *
 * The draw trigger has to read the dead creature's last-known counters: a 1/1 given a -1/-1
 * counter dies to state-based actions with the counter on it.
 */
class TheScorpionGodScenarioTest : ScenarioTestBase() {

    init {
        context("The Scorpion God") {

            test("a -1/-1 counter kills an opposing 1/1, and its death draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Scorpion God")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val god = game.findPermanent("The Scorpion God")!!
                val elves = game.findPermanent("Llanowar Elves")!!
                val handBefore = game.handSize(1)

                game.execute(
                    ActivateAbility(
                        game.player1Id,
                        god,
                        TheScorpionGod.activatedAbilities.single().id,
                        listOf(ChosenTarget.Permanent(elves)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the 1/1 died with a -1/-1 counter on it") {
                    game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                }
                withClue("its death drew The Scorpion God's controller a card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("a creature dying without a -1/-1 counter draws nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "The Scorpion God")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elves = game.findPermanent("Llanowar Elves")!!
                game.castSpell(1, "Lightning Bolt", elves).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                game.handSize(1) shouldBe 0
            }
        }
    }
}
