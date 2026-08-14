package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Troyan, Gutsy Explorer.
 *
 * Troyan's mana ability is the first user of `ManaRestriction.SpellsWithManaValueAtLeast` at a
 * threshold other than 4 and the first with the {X} clause but no creature-only clause, so these
 * tests pin all three axes of the restriction: below-threshold is refused, at-threshold is allowed,
 * and an {X} spell is allowed *despite* being below the threshold.
 *
 * The Goose Mother is the {X} probe on purpose: {X}{G}{U} cast for X=0 has mana value 2 on the stack,
 * well under 5, so only the "or spells with {X} in their mana costs" clause can make Troyan's two
 * mana pay for it.
 */
class TroyanGutsyExplorerScenarioTest : ScenarioTestBase() {

    private val manaAbilityId by lazy {
        cardRegistry.requireCard("Troyan, Gutsy Explorer").activatedAbilities[0].id
    }

    private val lootAbilityId by lazy {
        cardRegistry.requireCard("Troyan, Gutsy Explorer").activatedAbilities[1].id
    }

    private fun TestGame.pool() = state.getEntity(player1Id)?.get<ManaPoolComponent>()!!

    init {
        context("{T}: Add {G}{U} with the mana-value/{X} restriction") {
            test("adds two separately restricted mana, not one lump") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Troyan, Gutsy Explorer", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troyan = game.findPermanent("Troyan, Gutsy Explorer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troyan, abilityId = manaAbilityId)
                ).error shouldBe null

                withClue("the printed ruling says you needn't spend both on the same spell") {
                    game.pool().restrictedMana.size shouldBe 2
                }
            }

            test("cannot pay for a mana value 2 spell") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Troyan, Gutsy Explorer", summoningSickness = false)
                    .withCardInHand(1, "Rootrider Faun")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troyan = game.findPermanent("Troyan, Gutsy Explorer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troyan, abilityId = manaAbilityId)
                ).error shouldBe null

                // {1}{G} is exactly what {G}{U} would cover, so only the restriction can refuse it.
                game.castSpell(1, "Rootrider Faun").error shouldNotBe null
                game.pool().restrictedMana.size shouldBe 2
            }

            test("pays for a mana value 5 spell") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Troyan, Gutsy Explorer", summoningSickness = false)
                    .withCardInHand(1, "Beanstalk Wurm")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troyan = game.findPermanent("Troyan, Gutsy Explorer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troyan, abilityId = manaAbilityId)
                ).error shouldBe null

                game.castSpell(1, "Beanstalk Wurm").error shouldBe null
                withClue("restricted mana is spent preferentially when the spell qualifies") {
                    game.pool().restrictedMana.size shouldBe 0
                }
            }

            test("pays for an {X} spell that is below the mana value threshold") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Troyan, Gutsy Explorer", summoningSickness = false)
                    .withCardInHand(1, "The Goose Mother")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troyan = game.findPermanent("Troyan, Gutsy Explorer")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troyan, abilityId = manaAbilityId)
                ).error shouldBe null

                // {X}{G}{U} for X=0 is mana value 2 on the stack — under the threshold, but it has
                // {X} in its cost, and the mana may pay any part of that cost.
                game.castXSpell(1, "The Goose Mother", xValue = 0).error shouldBe null
                game.pool().restrictedMana.size shouldBe 0
            }
        }

        context("{U}, {T}: Draw a card, then discard a card") {
            test("loots, and competes with the mana ability for the same tap") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Troyan, Gutsy Explorer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInHand(1, "Rootrider Faun")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val troyan = game.findPermanent("Troyan, Gutsy Explorer")!!
                val handBefore = game.handSize(1)
                val libraryBefore = game.librarySize(1)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = troyan, abilityId = lootAbilityId)
                ).error shouldBe null
                game.resolveStack()

                withClue("the draw happens first, so the discard chooses among both cards") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                game.selectCards(listOf(game.findCardsInHand(1, "Rootrider Faun").first())).error shouldBe null
                game.resolveStack()

                withClue("drew one and discarded one, so hand size is unchanged") {
                    game.handSize(1) shouldBe handBefore
                    game.librarySize(1) shouldBe libraryBefore - 1
                    game.isInGraveyard(1, "Rootrider Faun") shouldBe true
                }
                withClue("Troyan is now tapped, so the mana ability is unavailable") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = troyan,
                            abilityId = manaAbilityId,
                        )
                    ).error shouldNotBe null
                }
            }
        }
    }
}
