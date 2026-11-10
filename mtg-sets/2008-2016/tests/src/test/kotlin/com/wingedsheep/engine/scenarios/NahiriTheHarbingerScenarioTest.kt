package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Nahiri, the Harbinger (SOI).
 *
 * What the snapshot can't see:
 *  - the −2's target is a heterogeneous union — "tapped" binds to the artifact and creature
 *    branches, never the enchantment;
 *  - the −8 normally takes Nahiri to 0 loyalty, so she dies to state-based actions before the
 *    ability resolves; the search, the haste grant and the delayed return must all still work
 *    with the source gone, and the return must not chase a card that already left the
 *    battlefield (printed ruling).
 */
class NahiriTheHarbingerScenarioTest : ScenarioTestBase() {

    init {
        context("the −2") {

            test("exiles an untapped enchantment") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Harbinger")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nahiri = game.findPermanent("Nahiri, the Harbinger")!!
                val anthem = game.findPermanent("Glorious Anthem")!!

                activate(game, nahiri, index = 1, targets = listOf(anthem)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Glorious Anthem") shouldBe true
                loyalty(game, nahiri) shouldBe 2
            }

            test("exiles a tapped creature but cannot target an untapped one") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Harbinger")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nahiri = game.findPermanent("Nahiri, the Harbinger")!!
                val untappedGiant = game.findPermanent("Hill Giant")!!
                val tappedBears = game.findPermanent("Grizzly Bears")!!

                withClue("an untapped creature is not a legal target") {
                    activate(game, nahiri, index = 1, targets = listOf(untappedGiant)).error shouldNotBe null
                }

                activate(game, nahiri, index = 1, targets = listOf(tappedBears)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("cannot target an untapped artifact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Harbinger")
                    .withCardOnBattlefield(2, "Darksteel Ingot")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nahiri = game.findPermanent("Nahiri, the Harbinger")!!
                val ingot = game.findPermanent("Darksteel Ingot")!!

                activate(game, nahiri, index = 1, targets = listOf(ingot)).error shouldNotBe null

                game.state = game.state.updateEntity(ingot) { it.with(TappedComponent) }
                activate(game, nahiri, index = 1, targets = listOf(ingot)).error shouldBe null
                game.resolveStack()

                game.isInExile(2, "Darksteel Ingot") shouldBe true
            }
        }

        context("the −8") {

            test("finds a creature that has haste and returns to hand at the next end step, though Nahiri died") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Harbinger")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nahiri = game.findPermanent("Nahiri, the Harbinger")!!
                setLoyalty(game, nahiri, 8)

                activate(game, nahiri, index = 2).error shouldBe null
                game.resolveStack()

                withClue("loyalty 8 -> 0, so Nahiri is put into the graveyard") {
                    game.isInGraveyard(1, "Nahiri, the Harbinger") shouldBe true
                }

                val bearsInLibrary = game.findCardsInLibrary(1, "Grizzly Bears").single()
                game.selectCards(listOf(bearsInLibrary)).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")
                withClue("the creature entered and gained haste") {
                    (bears != null) shouldBe true
                    game.state.projectedState.hasKeyword(bears!!, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("it returns to its owner's hand at the beginning of the end step") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("a found card that leaves the battlefield before the end step is not returned") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Nahiri, the Harbinger")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val nahiri = game.findPermanent("Nahiri, the Harbinger")!!
                setLoyalty(game, nahiri, 8)

                activate(game, nahiri, index = 2).error shouldBe null
                game.resolveStack()
                game.selectCards(listOf(game.findCardsInLibrary(1, "Grizzly Bears").single())).error shouldBe null
                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the card in the graveyard is a new object; the delayed return leaves it there") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<EntityId> = emptyList()
    ) = game.execute(
        ActivateAbility(
            playerId = game.player1Id,
            sourceId = source,
            abilityId = cardRegistry.getCard("Nahiri, the Harbinger")!!.script.activatedAbilities[index].id,
            targets = targets.map { ChosenTarget.Permanent(it) }
        )
    )

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }
}
