package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize

/**
 * Scenario tests for **Ashiok, Wicked Manipulator** (WOE #78).
 *
 * The load-bearing piece is the static replacement — "If you would pay life while your library has
 * at least that many cards in it, exile that many cards from the top of your library instead" —
 * which is the first effect in the engine to intercept a life *payment*. It is implemented as
 * `ReplaceLifePaymentWithLibraryExile` on the new `EventPattern.LifePaymentEvent`, consulted by
 * `LifePaymentService`, the choke point every life payment now funnels through.
 *
 * The three loyalty abilities are all built from existing primitives, so they get lighter coverage:
 * enough to prove the composition is wired to the right pieces (impulse-to-hand rather than
 * impulse-to-exile on the +1, the `CARDS_PUT_INTO_EXILE` intervening-if on the −2's tokens, and the
 * exile-mana-value X on the −7).
 */
class AshiokWickedManipulatorScenarioTest : ScenarioTestBase() {

    /** A permanent with a pure "Pay N life" activated cost — the cheapest way to trigger a payment. */
    private val lifeTap = card("Test Life Tap") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayLife(3)
            effect = Effects.GainLife(0)
        }
    }

    /** "You lose 3 life" — life *loss*, not a payment, so Ashiok must leave it alone. */
    private val lifeDrain = card("Test Life Drain") {
        manaCost = "{1}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mana("{0}")
            effect = Effects.LoseLife(3, EffectTarget.Controller)
        }
    }

    /** How many cards named [cardName] sit in a player's exile. */
    private fun TestGame.exiledCount(playerNumber: Int, cardName: String): Int {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).count {
            state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == cardName
        }
    }

    private fun TestGame.plusOneCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Activate the loyalty ability whose loyalty change is [loyaltyChange]. */
    private fun TestGame.activateLoyalty(
        playerNumber: Int,
        loyaltyChange: Int,
        targets: List<EntityId> = emptyList(),
    ) = run {
        val ashiok = findPermanent("Ashiok, Wicked Manipulator")!!
        val ability = cardRegistry.getCard("Ashiok, Wicked Manipulator")!!
            .script.activatedAbilities
            .first { (it.cost as? AbilityCost.Loyalty)?.change == loyaltyChange }
        execute(
            ActivateAbility(
                playerId = if (playerNumber == 1) player1Id else player2Id,
                sourceId = ashiok,
                abilityId = ability.id,
                targets = targets.map { entityIdToChosenTarget(state, it) },
            )
        )
    }

    private fun TestGame.activateFirstAbility(playerNumber: Int, cardName: String) =
        run {
            val permanent = findPermanent(cardName)!!
            val ability = cardRegistry.getCard(cardName)!!.script.activatedAbilities.first()
            execute(ActivateAbility(if (playerNumber == 1) player1Id else player2Id, permanent, ability.id))
        }

    private fun ScenarioBuilder.withLibrary(playerNumber: Int, cardName: String, count: Int) =
        also { repeat(count) { withCardInLibrary(playerNumber, cardName) } }

    init {
        cardRegistry.register(lifeTap)
        cardRegistry.register(lifeDrain)

        context("Static ability — paying life becomes exiling from the top of your library") {

            test("a life payment covered by the library exiles that many cards and costs no life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withCardOnBattlefield(1, "Test Life Tap")
                    .withLibrary(1, "Grizzly Bears", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.activateFirstAbility(1, "Test Life Tap")
                withClue("paying the life cost should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("no life is paid — the payment was replaced") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("three cards came off the top of the library instead") {
                    game.librarySize(1) shouldBe 2
                }
                withClue("and they went to exile") {
                    game.exiledCount(1, "Grizzly Bears") shouldBe 3
                }
            }

            test("a library shallower than the payment falls through to paying life normally") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withCardOnBattlefield(1, "Test Life Tap")
                    .withLibrary(1, "Grizzly Bears", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFirstAbility(1, "Test Life Tap").error shouldBe null
                game.resolveStack()

                withClue("2 cards can't cover a 3-life payment, so life is paid as normal") {
                    game.getLifeTotal(1) shouldBe 17
                    game.librarySize(1) shouldBe 2
                    game.exiledCount(1, "Grizzly Bears") shouldBe 0
                }
            }

            test("life loss is untouched — only payments are replaced") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withCardOnBattlefield(1, "Test Life Drain")
                    .withLibrary(1, "Grizzly Bears", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFirstAbility(1, "Test Life Drain").error shouldBe null
                game.resolveStack()

                withClue("\"you lose 3 life\" is life loss, not a payment (the printed reminder text)") {
                    game.getLifeTotal(1) shouldBe 17
                    game.librarySize(1) shouldBe 5
                }
            }

            test("an opponent's life payment is unaffected — the replacement is yours only") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withCardOnBattlefield(2, "Test Life Tap")
                    .withLibrary(2, "Grizzly Bears", 5)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateFirstAbility(2, "Test Life Tap").error shouldBe null
                game.resolveStack()

                withClue("Ashiok reads \"you\" — the opponent pays life the normal way") {
                    game.getLifeTotal(2) shouldBe 17
                    game.librarySize(2) shouldBe 5
                }
            }
        }

        context("+1 — look at the top two, exile one, the other to hand") {

            test("the kept card goes to hand and the remainder is exiled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withLibrary(1, "Grizzly Bears", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateLoyalty(1, 1).error shouldBe null
                game.resolveStack()

                withClue("the ability pauses to pick which of the two to keep") {
                    game.state.pendingDecision shouldNotBe null
                }
                val looked = game.findCardsInLibrary(1, "Grizzly Bears").take(1)
                game.selectCards(looked)
                game.resolveStack()

                withClue("one card to hand, one to exile, one left in the library") {
                    game.handSize(1) shouldBe 1
                    game.exiledCount(1, "Grizzly Bears") shouldBe 1
                    game.librarySize(1) shouldBe 1
                }
            }
        }

        context("−2 — two Nightmare tokens with the exile-gated combat trigger") {

            test("creates two 1/1 black Nightmares") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateLoyalty(1, -2).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Nightmare Token") shouldHaveSize 2
            }

            test("the tokens grow at combat when a card was put into exile this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withCardOnBattlefield(1, "Test Life Tap")
                    .withLibrary(1, "Grizzly Bears", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateLoyalty(1, -2).error shouldBe null
                game.resolveStack()

                // Ashiok's own static ability feeds the CARDS_PUT_INTO_EXILE tracker: paying 3 life
                // exiles three cards off the top instead.
                game.activateFirstAbility(1, "Test Life Tap").error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                val tokens = game.findAllPermanents("Nightmare Token")
                tokens shouldHaveSize 2
                withClue("cards were exiled this turn, so the intervening-if holds for both tokens") {
                    tokens.forEach { game.plusOneCounters(it) shouldBe 1 }
                }
            }

            test("no counters at combat when nothing was put into exile this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.activateLoyalty(1, -2).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("nothing was exiled, so the intervening-if is false") {
                    game.findAllPermanents("Nightmare Token").forEach { game.plusOneCounters(it) shouldBe 0 }
                }
            }
        }

        context("−7 — target player exiles the top X, X = total mana value of cards you own in exile") {

            test("X counts the mana value of your exiled cards, and the target mills to exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ashiok, Wicked Manipulator")
                    // Two Grizzly Bears ({1}{G}) already in your exile → X = 4.
                    .withCardInExile(1, "Grizzly Bears")
                    .withCardInExile(1, "Grizzly Bears")
                    .withLibrary(2, "Grizzly Bears", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Ashiok enters at 5 loyalty; the ultimate needs 7, so tick it up first.
                val ashiok = game.findPermanent("Ashiok, Wicked Manipulator")!!
                game.state = game.state.updateEntity(ashiok) { c ->
                    c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.LOYALTY, 2))
                }

                game.activateLoyalty(1, -7, targets = listOf(game.player2Id)).error shouldBe null
                game.resolveStack()

                withClue("X = 2 + 2 = 4, so the opponent exiles four cards off the top") {
                    game.librarySize(2) shouldBe 2
                    game.exiledCount(2, "Grizzly Bears") shouldBe 4
                }
            }
        }
    }
}
