package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Cult of the Waxing Moon (SOI #201) — {4}{G} Creature — Human Shaman, 5/4.
 *
 * "Whenever a permanent you control transforms into a non-Human creature, create a 2/2 green Wolf
 * creature token."
 *
 * The filter reads the face that's up *after* the transform (ruling 2016-04-08): Heir of Falkenrath
 * becoming the Vampire Heir to the Night triggers it; Daring Sleuth becoming the still-Human Bearer of
 * Overwhelming Truths doesn't; an opponent's permanent transforming doesn't either.
 */
class CultOfTheWaxingMoonScenarioTest : ScenarioTestBase() {

    private val heirAbilityId get() = cardRegistry.getCard("Heir of Falkenrath")!!.activatedAbilities.single().id
    private val clueAbilityId get() = cardRegistry.getCard("Clue")!!.activatedAbilities.single().id

    init {
        context("Cult of the Waxing Moon") {

            test("a permanent you control transforming into a non-Human creature creates a Wolf") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cult of the Waxing Moon")
                    .withCardOnBattlefield(1, "Heir of Falkenrath", summoningSickness = false)
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heir = game.findPermanent("Heir of Falkenrath")!!
                val toDiscard = game.findCardsInHand(1, "Forest").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = heir,
                        abilityId = heirAbilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(heir)!!.get<CardComponent>()!!.name shouldBe "Heir to the Night"
                val wolves = game.findPermanents("Wolf Token")
                withClue("one 2/2 Wolf token was created") {
                    wolves shouldHaveSize 1
                    game.state.projectedState.getPower(wolves.single()) shouldBe 2
                    game.state.projectedState.getToughness(wolves.single()) shouldBe 2
                }
            }

            test("transforming into a Human creature doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cult of the Waxing Moon")
                    .withCardOnBattlefield(1, "Daring Sleuth", summoningSickness = false)
                    .withCardOnBattlefield(1, "Clue", isToken = true)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sleuth = game.findPermanent("Daring Sleuth")!!
                val clue = game.findPermanents("Clue").single()

                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = clue, abilityId = clueAbilityId))
                    .error shouldBe null
                game.resolveStack()

                game.state.getEntity(sleuth)!!.get<CardComponent>()!!.name shouldBe "Bearer of Overwhelming Truths"
                withClue("Bearer of Overwhelming Truths is a Human, so no Wolf") {
                    game.findPermanents("Wolf Token") shouldHaveSize 0
                }
            }

            test("an opponent's permanent transforming doesn't trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Cult of the Waxing Moon")
                    .withCardOnBattlefield(2, "Heir of Falkenrath", summoningSickness = false)
                    .withCardInHand(2, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heir = game.findPermanent("Heir of Falkenrath")!!
                val toDiscard = game.findCardsInHand(2, "Forest").single()

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = heir,
                        abilityId = heirAbilityId,
                        costPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getEntity(heir)!!.get<CardComponent>()!!.name shouldBe "Heir to the Night"
                game.findPermanents("Wolf Token") shouldHaveSize 0
            }
        }
    }
}
