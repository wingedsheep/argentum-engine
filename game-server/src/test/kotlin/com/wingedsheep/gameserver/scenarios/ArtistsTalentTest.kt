package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Artist's Talent.
 *
 * Artist's Talent {1}{R}
 * Enchantment — Class
 *
 * Level 1: Whenever you cast a noncreature spell, you may discard a card. If you do, draw a card.
 * Level 2 ({2}{R}): Noncreature spells you cast cost {1} less to cast.
 * Level 3 ({2}{R}): If a source you control would deal noncombat damage to an opponent or a
 * permanent an opponent controls, it deals that much damage plus 2 instead.
 */
class ArtistsTalentTest : ScenarioTestBase() {

    init {
        context("Artist's Talent Level 1 — rummage on noncreature spell cast") {
            test("casting a noncreature spell triggers may discard/draw") {
                // Volcanic Hammer is {1}{R} Sorcery — deals 3 damage to any target
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Artist's Talent")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withCardInHand(1, "Quaketusk Boar") // card to discard
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1) // 2 cards: Volcanic Hammer + Quaketusk Boar

                // Cast Volcanic Hammer targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Volcanic Hammer", 2)
                withClue("Should cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // "Whenever you cast a noncreature spell" trigger fires on cast (before spell resolves)
                // The trigger is on top of stack above Volcanic Hammer
                // Answer "yes" to the may rummage effect
                game.resolveStack()
                game.answerYesNo(true)

                // Now resolve the Volcanic Hammer
                game.resolveStack()

                // Initial hand: 2, cast (-1), discard 1 (-1), draw 1 (+1) = 1
                withClue("Hand size should be 1 (cast spell, then rummage is net 0)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }

            test("can decline the may effect") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Artist's Talent")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withCardInHand(1, "Quaketusk Boar")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1) // 2 cards

                val castResult = game.castSpellTargetingPlayer(1, "Volcanic Hammer", 2)
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Trigger resolves first — decline the may effect
                game.resolveStack()
                game.answerYesNo(false)

                // Now resolve the Volcanic Hammer
                game.resolveStack()

                // Only the hammer was cast (-1 from hand), no discard/draw
                withClue("Hand size should be initial - 1 (just cast spell)") {
                    game.handSize(1) shouldBe initialHandSize - 1
                }
            }
        }

        context("Artist's Talent Level 3 — noncombat damage bonus") {
            test("noncombat damage to opponent is increased by 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Artist's Talent", classLevel = 3)
                    .withCardInHand(1, "Volcanic Hammer") // deals 3 damage
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialLife = game.getLifeTotal(2)

                // Cast Volcanic Hammer targeting opponent
                val castResult = game.castSpellTargetingPlayer(1, "Volcanic Hammer", 2)
                withClue("Should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Level 1 trigger fires first (noncreature spell cast) — decline it
                game.resolveStack()
                game.answerYesNo(false)

                // Now Volcanic Hammer resolves — should deal 3 + 2 = 5 damage
                game.resolveStack()

                withClue("Opponent should take 5 damage (3 + 2 bonus)") {
                    game.getLifeTotal(2) shouldBe initialLife - 5
                }
            }
        }

        context("Artist's Talent level-up") {
            test("can level up from 1 to 2") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Artist's Talent")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val talentId = game.findPermanent("Artist's Talent")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = talentId,
                        abilityId = AbilityId.classLevelUp(2)
                    )
                )
                withClue("Level-up should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                val classComponent = game.state.getEntity(talentId)?.get<ClassLevelComponent>()
                withClue("Should be at level 2") {
                    classComponent?.currentLevel shouldBe 2
                }
            }
        }
    }
}
