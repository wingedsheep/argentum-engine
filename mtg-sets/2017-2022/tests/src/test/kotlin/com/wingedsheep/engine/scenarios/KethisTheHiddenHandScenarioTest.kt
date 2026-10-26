package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kethis, the Hidden Hand (M20 #211) — {W}{B}{G} 3/4.
 *
 *   Legendary spells you cast cost {1} less to cast.
 *   Exile two legendary cards from your graveyard: Until end of turn, each legendary card in your
 *   graveyard gains "You may play this card from your graveyard."
 *
 * Rulings pinned here: only legendary cards in the graveyard as the ability resolves gain it, and
 * a legendary card that is played and comes back to the graveyard no longer has it.
 */
class KethisTheHiddenHandScenarioTest : ScenarioTestBase() {

    init {
        context("Kethis, the Hidden Hand") {

            fun ScenarioTestBase.TestGame.graveyardCard(name: String): EntityId =
                findCardsInGraveyard(1, name).single()

            fun ScenarioTestBase.TestGame.canCastFromGraveyard(name: String): Boolean {
                val id = findCardsInGraveyard(1, name).singleOrNull() ?: return false
                return getLegalActions(1).any { (it.action as? CastSpell)?.cardId == id }
            }

            /** Activate Kethis exiling [fodder] (two legendary cards in the graveyard) and resolve it. */
            fun ScenarioTestBase.TestGame.activateKethis(fodder: List<EntityId>) {
                val kethis = findPermanent("Kethis, the Hidden Hand")!!
                val abilityId = cardRegistry.getCard("Kethis, the Hidden Hand")!!.activatedAbilities.first().id
                val result = execute(
                    ActivateAbility(
                        playerId = player1Id,
                        sourceId = kethis,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(exiledCards = fodder),
                    )
                )
                withClue("activation: ${result.error}") { result.error shouldBe null }
                resolveStack()
            }

            test("legendary spells you cast cost {1} less") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethis, the Hidden Hand")
                    .withCardInHand(1, "Kamahl, Pit Fighter")
                    .withLandsOnBattlefield(1, "Mountain", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Kamahl, Pit Fighter is {4}{R}{R}; five lands pay for it only with the reduction.
                game.castSpell(1, "Kamahl, Pit Fighter").error shouldBe null
                game.resolveStack()
                game.findPermanent("Kamahl, Pit Fighter") shouldNotBe null
            }

            test("each legendary card left in the graveyard may be played this turn; others may not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethis, the Hidden Hand")
                    .withCardInGraveyard(1, "Kamahl, Pit Fighter")
                    .withCardInGraveyard(1, "Sivitri Scarzam")
                    .withCardInGraveyard(1, "Isamaru, Hound of Konda")
                    .withCardInGraveyard(1, "Eiganjo Castle")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe false
                game.activateKethis(listOf(game.graveyardCard("Kamahl, Pit Fighter"), game.graveyardCard("Sivitri Scarzam")))

                game.isInExile(1, "Kamahl, Pit Fighter") shouldBe true
                game.isInExile(1, "Sivitri Scarzam") shouldBe true
                game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe true
                game.canCastFromGraveyard("Grizzly Bears") shouldBe false

                // A legendary land is played from the graveyard as the turn's land drop.
                val castle = game.graveyardCard("Eiganjo Castle")
                game.execute(PlayLand(game.player1Id, castle)).error shouldBe null
                game.findPermanent("Eiganjo Castle") shouldNotBe null

                // Isamaru costs {W}: the reduction only takes generic mana.
                game.castSpellFromGraveyard(1, "Isamaru, Hound of Konda").error shouldBe null
                game.resolveStack()
                game.findPermanent("Isamaru, Hound of Konda") shouldNotBe null
            }

            test("a legendary card put into the graveyard after the ability resolves does not gain it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethis, the Hidden Hand")
                    .withCardInGraveyard(1, "Kamahl, Pit Fighter")
                    .withCardInGraveyard(1, "Sivitri Scarzam")
                    .withCardOnBattlefield(1, "Isamaru, Hound of Konda")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateKethis(listOf(game.graveyardCard("Kamahl, Pit Fighter"), game.graveyardCard("Sivitri Scarzam")))
                val isamaru = game.findPermanent("Isamaru, Hound of Konda")!!
                game.castSpell(1, "Lightning Bolt", isamaru).error shouldBe null
                game.resolveStack()
                game.state.getGraveyard(game.player1Id) shouldContain isamaru
                game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe false
            }

            test("a legendary spell cast this way and countered returns without the ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethis, the Hidden Hand")
                    .withCardInGraveyard(1, "Kamahl, Pit Fighter")
                    .withCardInGraveyard(1, "Sivitri Scarzam")
                    .withCardInGraveyard(1, "Isamaru, Hound of Konda")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInHand(2, "Counterspell")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateKethis(listOf(game.graveyardCard("Kamahl, Pit Fighter"), game.graveyardCard("Sivitri Scarzam")))
                game.castSpellFromGraveyard(1, "Isamaru, Hound of Konda").error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Counterspell", "Isamaru, Hound of Konda").error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Isamaru, Hound of Konda") shouldBe true
                withClue("the card came back as a new object, so Kethis's grant no longer applies") {
                    game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe false
                    game.castSpellFromGraveyard(1, "Isamaru, Hound of Konda").error shouldNotBe null
                }
            }
            test("a legendary card that leaves the graveyard any other way comes back without the ability") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kethis, the Hidden Hand")
                    .withCardInGraveyard(1, "Kamahl, Pit Fighter")
                    .withCardInGraveyard(1, "Sivitri Scarzam")
                    .withCardInGraveyard(1, "Isamaru, Hound of Konda")
                    .withCardInHand(1, "Raise Dead")
                    .withCardInHand(1, "Cathartic Reunion")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateKethis(listOf(game.graveyardCard("Kamahl, Pit Fighter"), game.graveyardCard("Sivitri Scarzam")))
                val isamaru = game.graveyardCard("Isamaru, Hound of Konda")
                game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe true

                // Graveyard -> hand (Raise Dead) -> graveyard (discarded to Cathartic Reunion).
                game.castSpellTargetingGraveyardCard(1, "Raise Dead", listOf(isamaru)).error shouldBe null
                game.resolveStack()
                val reunion = game.findCardsInHand(1, "Cathartic Reunion").single()
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        reunion,
                        additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(isamaru, bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.state.getGraveyard(game.player1Id) shouldContain isamaru
                game.canCastFromGraveyard("Isamaru, Hound of Konda") shouldBe false
            }
        }
    }
}
