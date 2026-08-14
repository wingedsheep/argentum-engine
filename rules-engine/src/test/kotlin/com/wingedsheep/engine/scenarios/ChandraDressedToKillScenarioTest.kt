package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra, Dressed to Kill (VOW #149, {1}{R}{R}, Loyalty 3).
 *
 *   +1: Add {R}. Chandra deals 1 damage to up to one target player or planeswalker.
 *   +1: Exile the top card of your library. If it's red, you may cast it this turn.
 *   −7: Exile the top five cards of your library. You may cast red spells from among them this
 *       turn. You get an emblem with "Whenever you cast a red spell, this emblem deals X damage to
 *       any target, where X is the amount of mana spent to cast that spell."
 *
 * The two impulse abilities are the interesting pair: the middle +1 gates on the *exiled card's*
 * colour (a collection filter), while the −7 gates on the *spell's* colour at cast time (the new
 * `MayPlayPermission.castColorRestriction`). Both are covered, plus the emblem's X.
 */
class ChandraDressedToKillScenarioTest : ScenarioTestBase() {

    init {
        context("the damage +1") {

            test("adds {R} and pings the targeted player") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Dressed to Kill")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Dressed to Kill")!!
                // Targets for an activated ability are chosen as it goes on the stack.
                activate(
                    game, chandra, index = 0,
                    targets = listOf(ChosenTarget.Player(game.player2Id))
                )
                game.resolveStack()

                withClue("1 damage to the chosen player") { game.getLifeTotal(2) shouldBe 19 }
                withClue("+1 moved Chandra from 3 to 4 loyalty") { loyalty(game, chandra) shouldBe 4 }
                withClue("the {R} landed in the pool") {
                    game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()?.red shouldBe 1
                }
            }
        }

        context("the impulse +1") {

            test("a red top card becomes castable this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Dressed to Kill")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Dressed to Kill")!!
                activate(game, chandra, index = 1)
                game.resolveStack()

                game.isInExile(1, "Lightning Bolt") shouldBe true
                withClue("a red exiled card is offered for casting") {
                    castableFromExile(game, "Lightning Bolt") shouldBe true
                }
            }

            test("a nonred top card is exiled but stays uncastable") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Dressed to Kill")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Dressed to Kill")!!
                activate(game, chandra, index = 1)
                game.resolveStack()

                game.isInExile(1, "Grizzly Bears") shouldBe true
                withClue("\"if it's red\" gates the permission, not the exile") {
                    castableFromExile(game, "Grizzly Bears") shouldBe false
                }
            }
        }

        context("the −7") {

            test("exiles five, offers only the red spells, and leaves a burn emblem behind") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Chandra, Dressed to Kill")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Counterspell")
                    .withCardInLibrary(1, "Giant Growth")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val chandra = game.findPermanent("Chandra, Dressed to Kill")!!
                setLoyalty(game, chandra, 7)
                activate(game, chandra, index = 2)
                game.resolveStack()

                withClue("Chandra hit 0 loyalty and died; the emblem is global and survives her") {
                    game.findPermanent("Chandra, Dressed to Kill") shouldBe null
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }
                withClue("all five cards were exiled") {
                    game.librarySize(1) shouldBe 0
                    listOf("Lightning Bolt", "Grizzly Bears", "Hill Giant", "Counterspell", "Giant Growth")
                        .all { game.isInExile(1, it) } shouldBe true
                }
                withClue("only red spells among them may be cast") {
                    castableFromExile(game, "Lightning Bolt") shouldBe true
                    castableFromExile(game, "Grizzly Bears") shouldBe false
                    castableFromExile(game, "Counterspell") shouldBe false
                    castableFromExile(game, "Giant Growth") shouldBe false
                }

                // Casting a red spell for {R} fires the emblem for X = 1.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("3 from the Bolt plus X=1 from the emblem (one mana was spent)") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }
        }
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<ChosenTarget> = emptyList(),
    ) {
        val ability = cardRegistry.getCard("Chandra, Dressed to Kill")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets,
            )
        ).error shouldBe null
    }

    /** Is [cardName] offered as a CastSpell action from a zone other than the hand? */
    private fun castableFromExile(game: TestGame, cardName: String): Boolean =
        game.getLegalActions(1).any {
            it.actionType == "CastSpell" && it.description.contains(cardName) && it.sourceZone == "EXILE"
        }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent(mapOf(CounterType.LOYALTY to amount)))
        }
    }
}
