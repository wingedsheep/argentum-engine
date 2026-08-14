package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.EmblemStaticAbilityComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Tamiyo, Field Researcher (EMN #190, {1}{G}{W}{U}, loyalty 4).
 *
 *   +1: Choose up to two target creatures. Until your next turn, whenever either of those creatures
 *       deals combat damage, you draw a card.
 *   −2: Tap up to two target nonland permanents. They don't untap during their controller's next
 *       untap step.
 *   −7: Draw three cards. You get an emblem with "You may cast spells from your hand without paying
 *       their mana costs."
 *
 * The three things that could silently resolve wrong, each with its own test: the +1 draws for
 * *Tamiyo's controller* even when the watched creature belongs to an opponent (printed ruling) and
 * lives exactly until that controller's next turn — the new
 * [com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry.UntilControllersNextTurn]; the −2's
 * don't-untap rider has to survive the target controller's untap step rather than the caster's; and
 * the −7 emblem carries a static ability on the emblem itself, so the free cast has to be visible to
 * a cost scan that otherwise only walks the battlefield.
 */
class TamiyoFieldResearcherScenarioTest : ScenarioTestBase() {

    init {
        context("the +1") {

            test("a watched creature's combat damage draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Field Researcher")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, Field Researcher")!!
                setLoyalty(game, tamiyo, 4)
                val bears = game.findPermanent("Grizzly Bears")!!

                activate(game, tamiyo, index = 0, targets = listOf(bears))
                game.resolveStack()

                withClue("+1 installed one watcher and moved loyalty 4 -> 5") {
                    game.state.delayedTriggers.size shouldBe 1
                    loyalty(game, tamiyo) shouldBe 5
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                val libraryBefore = game.librarySize(1)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("2 combat damage got through and the watcher drew one card") {
                    game.getLifeTotal(2) shouldBe 18
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }

            test("an opponent's creature draws for you, not for them, and stops after your next untap") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Field Researcher")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, Field Researcher")!!
                setLoyalty(game, tamiyo, 4)
                val bears = game.findPermanent("Grizzly Bears")!!

                activate(game, tamiyo, index = 0, targets = listOf(bears))
                game.resolveStack()

                // Out to the opponent's turn — the watcher is not scoped to this turn.
                advanceToNextTurn(game)
                withClue("the watcher survived the turn boundary") {
                    game.state.activePlayerId shouldBe game.player2Id
                    game.state.delayedTriggers.size shouldBe 1
                }
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                val ownerLibraryBefore = game.librarySize(1)
                val attackerLibraryBefore = game.librarySize(2)
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("Tamiyo's controller drew, the creature's controller did not") {
                    game.getLifeTotal(1) shouldBe 18
                    game.librarySize(1) shouldBe ownerLibraryBefore - 1
                    game.librarySize(2) shouldBe attackerLibraryBefore
                }

                // Back around to Tamiyo's controller: the untap step is where it wears off.
                advanceToNextTurn(game)
                withClue("\"until your next turn\" expired on the controller's next untap step") {
                    game.state.activePlayerId shouldBe game.player1Id
                    game.state.delayedTriggers.size shouldBe 0
                }
            }
        }

        context("the −2") {

            test("taps both targets and holds them down through their controller's untap step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Field Researcher")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    // The test runs out to turn 4; neither player may deck out on the way.
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withCardInLibrary(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, Field Researcher")!!
                setLoyalty(game, tamiyo, 4)
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                activate(game, tamiyo, index = 1, targets = listOf(bears, giant))
                game.resolveStack()

                withClue("both targets tapped; −2 moved loyalty 4 -> 2") {
                    isTapped(game, bears) shouldBe true
                    isTapped(game, giant) shouldBe true
                    loyalty(game, tamiyo) shouldBe 2
                }

                advanceToNextTurn(game)
                withClue("their controller's untap step came and went without untapping them") {
                    game.state.activePlayerId shouldBe game.player2Id
                    isTapped(game, bears) shouldBe true
                    isTapped(game, giant) shouldBe true
                }

                advanceToNextTurn(game)
                advanceToNextTurn(game)
                withClue("the untap step after that is unaffected") {
                    game.state.activePlayerId shouldBe game.player2Id
                    isTapped(game, bears) shouldBe false
                    isTapped(game, giant) shouldBe false
                }
            }
        }

        context("the −7") {

            test("draws three and leaves an emblem that pays for a spell") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tamiyo, Field Researcher")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tamiyo = game.findPermanent("Tamiyo, Field Researcher")!!
                setLoyalty(game, tamiyo, 7)
                val handBefore = game.handSize(1)

                activate(game, tamiyo, index = 2)
                game.resolveStack()

                withClue("Tamiyo drew three and died to the 0-loyalty state-based action") {
                    game.handSize(1) shouldBe handBefore + 3
                    game.findPermanent("Tamiyo, Field Researcher") shouldBe null
                }
                withClue("the emblem outlives her, carrying the static on itself") {
                    game.state.entities.values.count {
                        it.get<EmblemStaticAbilityComponent>() != null
                    } shouldBe 1
                }

                // No lands, no mana in pool — only the emblem can pay for this.
                val giant = game.findCardsInHand(1, "Hill Giant").first()
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = giant,
                        useWithoutPayingManaCost = true,
                        paymentStrategy = PaymentStrategy.FromPool
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("the {4} creature resolved having paid nothing") {
                    game.state.getBattlefield().contains(giant) shouldBe true
                }
            }
        }
    }

    /** Step out through the end step so the next [Phase.PRECOMBAT_MAIN] is the *following* turn's. */
    private fun advanceToNextTurn(game: TestGame) {
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
    }

    private fun activate(
        game: TestGame,
        source: EntityId,
        index: Int,
        targets: List<EntityId> = emptyList()
    ) {
        val ability = cardRegistry.getCard("Tamiyo, Field Researcher")!!.script.activatedAbilities[index]
        game.execute(
            ActivateAbility(
                playerId = game.player1Id,
                sourceId = source,
                abilityId = ability.id,
                targets = targets.map { ChosenTarget.Permanent(it) }
            )
        ).error shouldBe null
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun setLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true
}
