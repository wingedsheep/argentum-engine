package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Nowhere to Run (DSK).
 *
 * Card reference:
 * - Nowhere to Run ({1}{B}): Enchantment, Flash
 *   "When this enchantment enters, target creature an opponent controls gets -3/-3 until end of turn."
 *   "Creatures your opponents control can be the targets of spells and abilities as though they
 *    didn't have hexproof. Ward abilities of those creatures don't trigger."
 */
class NowhereToRunScenarioTest : ScenarioTestBase() {

    init {
        context("ETB trigger — target creature gets -3/-3") {

            test("target creature gets -3/-3 until end of turn on entry") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Nowhere to Run")
                    .withCardOnBattlefield(2, "Grizzly Bears")   // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                // Nowhere to Run is an enchantment with no cast-time targets — targets for
                // the ETB triggered ability are chosen when the trigger resolves.
                val castResult = game.castSpell(1, "Nowhere to Run")
                withClue("Should cast Nowhere to Run: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Spell resolves → NtR enters battlefield → ETB trigger fires, waiting for target
                game.resolveStack()
                withClue("Should have pending decision for ETB trigger target") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(bearsId))

                // ETB trigger resolves: Grizzly Bears gets -3/-3 → 2/2 becomes -1/-1 → dies from SBA
                game.resolveStack()
                withClue("Grizzly Bears should be in graveyard after -3/-3 lethal reduction") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }

        context("Static — hexproof bypass") {

            test("opponent's hexproof creature can be targeted while Nowhere to Run is on battlefield") {
                // Sagu Mauler has hexproof — normally can't be targeted by opponents
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nowhere to Run")
                    .withCardInHand(1, "Cast Down")
                    .withCardOnBattlefield(2, "Sagu Mauler")   // 6/6 hexproof
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val maulerId = game.findPermanent("Sagu Mauler")!!

                val castResult = game.castSpell(1, "Cast Down", maulerId)
                withClue("Cast Down should target Sagu Mauler while Nowhere to Run suppresses hexproof: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Sagu Mauler should be destroyed after Cast Down resolved") {
                    game.isOnBattlefield("Sagu Mauler") shouldBe false
                }
            }

            test("opponent's hexproof creature cannot be targeted WITHOUT Nowhere to Run") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cast Down")
                    .withCardOnBattlefield(2, "Sagu Mauler")   // 6/6 hexproof — no suppression
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val maulerId = game.findPermanent("Sagu Mauler")!!

                val castResult = game.castSpell(1, "Cast Down", maulerId)
                withClue("Cast Down should fail to target hexproof Sagu Mauler without suppression") {
                    castResult.error shouldNotBe null
                }
            }
        }

        context("Static — ward suppression") {

            test("ward does not trigger when targeting opponent's creature while Nowhere to Run is on battlefield") {
                // Long River Lurker has ward {1}
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nowhere to Run")
                    .withCardInHand(1, "Cast Down")
                    .withCardOnBattlefield(2, "Long River Lurker")  // 4/4 ward {1}
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurkerId = game.findPermanent("Long River Lurker")!!

                val castResult = game.castSpell(1, "Cast Down", lurkerId)
                withClue("Cast Down should target Long River Lurker: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve — ward should NOT fire; Cast Down resolves and destroys the lurker
                game.resolveStack()

                withClue("Long River Lurker should be destroyed (ward was suppressed by Nowhere to Run)") {
                    game.isOnBattlefield("Long River Lurker") shouldBe false
                }
            }

            test("ward triggers normally WITHOUT Nowhere to Run") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cast Down")
                    .withCardOnBattlefield(2, "Long River Lurker")  // 4/4 ward {1}
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lurkerId = game.findPermanent("Long River Lurker")!!

                val castResult = game.castSpell(1, "Cast Down", lurkerId)
                withClue("Cast Down should target Long River Lurker: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Ward triggers — opponent has mana to pay
                game.resolveStack()

                withClue("Long River Lurker should still be on battlefield (ward countered Cast Down)") {
                    game.isOnBattlefield("Long River Lurker") shouldBe true
                }
                withClue("Cast Down should be in graveyard (countered by ward)") {
                    game.isInGraveyard(1, "Cast Down") shouldBe true
                }
            }
        }
    }
}
