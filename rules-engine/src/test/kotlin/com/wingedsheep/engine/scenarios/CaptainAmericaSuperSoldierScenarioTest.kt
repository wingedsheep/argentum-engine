package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.msh.cards.CaptainAmericaSuperSoldier
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Captain America, Super-Soldier (MSH #9).
 *
 * {1}{W}{W} Legendary Creature — Human Soldier Hero 3/2
 * "First strike
 *  Captain America enters with a shield counter on him.
 *  As long as Captain America has a shield counter on him, you and other Heroes you control have
 *  hexproof."
 *
 * The shield counter's own behavior (CR 122.1c) is covered by `ShieldCounterScenarioTest`; this
 * covers the card: that he enters with one, that the grant covers *you* and *other* Heroes but not
 * himself, and that the whole grant switches off the moment the counter is spent.
 */
class CaptainAmericaSuperSoldierScenarioTest : ScenarioTestBase() {

    // A second Hero to stand behind the shield, defined locally so the test doesn't depend on
    // another card's stats.
    private val sidekick = card("Test Hero Sidekick") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Hero"
        power = 2
        toughness = 2
    }

    private fun shieldCounters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.SHIELD) ?: 0

    /** Put a shield counter on [id] directly, standing in for the as-enters replacement. */
    private fun giveShieldCounter(game: TestGame, id: EntityId) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.SHIELD, 1))
        }
    }

    private fun shockAt(game: TestGame, target: ChosenTarget) =
        game.execute(
            CastSpell(
                playerId = game.player2Id,
                cardId = game.findCardsInHand(2, "Shock").first(),
                targets = listOf(target),
            )
        )

    init {
        cardRegistry.register(CaptainAmericaSuperSoldier)
        cardRegistry.register(sidekick)

        context("Captain America, Super-Soldier") {

            test("he enters with a shield counter on him") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Captain America, Super-Soldier")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Captain America, Super-Soldier").error shouldBe null
                game.resolveStack()

                val cap = game.findPermanent("Captain America, Super-Soldier")!!
                withClue("the as-enters replacement gives him exactly one shield counter") {
                    shieldCounters(game, cap) shouldBe 1
                }
            }

            test("while he has the counter, you and other Heroes have hexproof — but he does not") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Captain America, Super-Soldier")
                    .withCardOnBattlefield(1, "Test Hero Sidekick")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cap = game.findPermanent("Captain America, Super-Soldier")!!
                giveShieldCounter(game, cap)

                withClue("'you' — the opponent can't target Captain America's controller") {
                    (shockAt(game, ChosenTarget.Player(game.player1Id)).error != null) shouldBe true
                }
                withClue("'other Heroes you control' — the sidekick is protected") {
                    val sidekickId = game.findPermanent("Test Hero Sidekick")!!
                    (shockAt(game, ChosenTarget.Permanent(sidekickId)).error != null) shouldBe true
                }
                withClue("'other' excludes Captain America himself — he stays targetable") {
                    shockAt(game, ChosenTarget.Permanent(cap)).error shouldBe null
                }
            }

            test("without the counter nothing is protected") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Captain America, Super-Soldier")
                    .withCardOnBattlefield(1, "Test Hero Sidekick")
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // No counter placed — the conditional statics are inert.
                withClue("the player half of the grant is gated on the counter, not his presence") {
                    shockAt(game, ChosenTarget.Player(game.player1Id)).error shouldBe null
                }
                game.resolveStack()

                withClue("and so is the 'other Heroes' half — a separate conditional static") {
                    val sidekickId = game.findPermanent("Test Hero Sidekick")!!
                    shockAt(game, ChosenTarget.Permanent(sidekickId)).error shouldBe null
                }
            }

            test("spending the shield counter switches the hexproof grant back off") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Captain America, Super-Soldier")
                    .withCardOnBattlefield(1, "Test Hero Sidekick")
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Shock")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 6)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cap = game.findPermanent("Captain America, Super-Soldier")!!
                giveShieldCounter(game, cap)

                // Shock him: CR 122.1c prevents the damage and eats the counter. He survives — a
                // 3/2 would otherwise die to 2 damage.
                shockAt(game, ChosenTarget.Permanent(cap)).error shouldBe null
                game.resolveStack()

                withClue("the shield counter absorbed the Shock and he lived") {
                    game.isOnBattlefield("Captain America, Super-Soldier") shouldBe true
                    shieldCounters(game, cap) shouldBe 0
                }
                withClue("with the counter gone, the controller is targetable again") {
                    shockAt(game, ChosenTarget.Player(game.player1Id)).error shouldBe null
                }
                game.resolveStack()

                // The two halves are separate statics reached by different machinery — the player
                // half through the GrantsControllerHexproofComponent marker, the permanents half
                // through a projected continuous effect — so the sidekick needs its own assertion.
                withClue("and so are the other Heroes he was shielding") {
                    val sidekickId = game.findPermanent("Test Hero Sidekick")!!
                    shockAt(game, ChosenTarget.Permanent(sidekickId)).error shouldBe null
                }
            }
        }
    }
}
