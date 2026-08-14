package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.TheQueenOfDale
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Queen of Dale (HOB #24) — {1}{W} Legendary Creature — Human Noble 2/1.
 *
 * "Whenever an opponent casts their first noncreature spell each turn, you recruit."
 *
 * The point of interest is the new `spellFilter` on the Nth-spell-cast event: the ordinal has to
 * run over the opponent's *noncreature* casts alone, so a creature spell cast first must neither
 * trigger it nor consume the window, and a second noncreature spell in the same turn must not
 * trigger it again.
 */
class TheQueenOfDaleScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(TheQueenOfDale)

        context("The Queen of Dale") {

            test("an opponent's first noncreature spell recruits") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Queen of Dale")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("recruit pauses for the discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
                game.resolveStack()

                withClue("recruit drew the Forest") { game.isInHand(1, "Forest") shouldBe true }
                withClue("the nonland discard mints one Human Soldier token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
            }

            test("a creature spell neither triggers it nor consumes the turn's window") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Queen of Dale")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardInHand(2, "Goblin Guide")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 6)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Goblin Guide").error shouldBe null
                game.resolveStack()

                withClue("a creature spell is outside the filter, so nothing triggers") {
                    game.hasPendingDecision() shouldBe false
                }

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("the noncreature spell is still the opponent's *first* one, so it triggers") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
                game.resolveStack()
                game.findAllPermanents("Human Soldier Token").size shouldBe 1
            }

            test("a second noncreature spell in the same turn does not trigger it again") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "The Queen of Dale")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Forest")
                    .withCardsInHand(2, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(2, "Mountain", 6)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
                game.resolveStack()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()

                withClue("the window closed with the turn's first noncreature spell") {
                    game.hasPendingDecision() shouldBe false
                    game.findAllPermanents("Human Soldier Token").size shouldBe 1
                }
            }
        }
    }
}
