package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.SoundTheTrumpets
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sound the Trumpets (HOB #55) — {1}{U}{U} Instant.
 *
 * "Counter target spell. If that spell's mana value was 2 or less, recruit."
 *
 * The card hoists the mana-value test above the counter so it reads the spell while it is still on
 * the stack. These tests pin both sides of that gate: a mana value of 2 recruits, a mana value of 4
 * is countered with no recruit at all (no discard prompt, no Soldier).
 */
class SoundTheTrumpetsScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(SoundTheTrumpets)

        context("Sound the Trumpets") {

            test("countering a mana value 2 spell recruits") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sound the Trumpets")
                    .withCardInHand(1, "Savannah Lions") // the nonland card recruit will discard
                    .withCardInLibrary(1, "Island") // recruit draws this first
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Grizzly Bears") // {1}{G} — mana value 2
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Sound the Trumpets", "Grizzly Bears")
                    .error shouldBe null
                game.resolveStack()

                withClue("mana value 2 passes the gate, so recruit pauses for the discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                withClue("recruit drew before the discard") {
                    game.isInHand(1, "Island") shouldBe true
                }

                val lions = game.findCardsInHand(1, "Savannah Lions").single()
                game.selectCards(listOf(lions))
                game.resolveStack()

                withClue("the Bears were countered") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("discarding a nonland card mints a Soldier") {
                    game.isInGraveyard(1, "Savannah Lions") shouldBe true
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
            }

            test("countering a mana value 4 spell does not recruit") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Sound the Trumpets")
                    .withCardInHand(1, "Savannah Lions")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Hill Giant") // {3}{R} — mana value 4
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Hill Giant").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(1, "Sound the Trumpets", "Hill Giant")
                    .error shouldBe null
                game.resolveStack()

                withClue("the Giant was still countered — the gate only guards the recruit rider") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("no recruit: no discard prompt") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no recruit: nothing drawn, nothing discarded, no Soldier") {
                    game.isInHand(1, "Island") shouldBe false
                    game.isInHand(1, "Savannah Lions") shouldBe true
                    game.findAllPermanents("Human Soldier Token").size shouldBe 0
                }
            }
        }
    }
}
