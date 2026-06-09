package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goliath Daydreamer (ECL) — {2}{R}{R} Giant Wizard, 4/4.
 *
 * "Whenever you cast an instant or sorcery spell from your hand, exile that card with a dream
 * counter on it instead of putting it into your graveyard as it resolves.
 * Whenever this creature attacks, you may cast a spell from among cards you own in exile with
 * dream counters on them without paying its mana cost."
 *
 * Per the card's rulings the attack trigger only allows the cast **while the ability is
 * resolving** — "You can't wait to cast the spell later in the turn. Timing restrictions based
 * on the card's type are ignored." So the trigger must use the mid-resolution
 * `CastFromCollectionWithoutPayingCostEffect` (Sunbird's Invocation pattern), never a lingering
 * until-end-of-turn may-play grant. These tests pin that:
 *  - a *sorcery* is cast during the declare attackers step (type timing ignored) with no mana
 *    available (cost ignored);
 *  - declining leaves the card exiled with **no** lingering may-play permission (the bug this
 *    test was written against: the old implementation granted an until-end-of-turn permission).
 */
class GoliathDaydreamerScenarioTest : ScenarioTestBase() {

    init {
        context("first ability — instants/sorceries cast from hand are exiled with a dream counter") {

            test("a sorcery cast from hand resolves, then goes to exile with a dream counter instead of the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goliath Daydreamer")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpellTargetingPlayer(1, "Volcanic Hammer", targetPlayerNumber = 2)
                withClue("Casting Volcanic Hammer should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Volcanic Hammer resolved normally first (3 damage to Player2)") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("As it resolves the spell is exiled, not put into the graveyard") {
                    namesInExile(game, 1) shouldBe setOf("Volcanic Hammer")
                    namesInGraveyard(game, 1) shouldBe emptySet()
                }
                withClue("The exiled card carries a dream counter") {
                    dreamCounters(game, exiledNamed(game, "Volcanic Hammer")) shouldBe 1
                }
            }
        }

        context("attack trigger — cast a dream-counter card for free during the trigger's resolution") {

            test("a sorcery in exile is cast for free, mid-combat, while the trigger resolves") {
                val game = setupWithExiledHammer()

                game.declareAttackers(mapOf("Goliath Daydreamer" to 2))
                advanceToDreamDecision(game)

                val decision = game.getPendingDecision()
                withClue("The attack trigger pauses mid-resolution with a cast-for-free choice") {
                    (decision is SelectCardsDecision) shouldBe true
                }
                decision as SelectCardsDecision
                withClue("The dream-counter card is offered") {
                    optionNames(game, decision) shouldBe setOf("Volcanic Hammer")
                }

                // Cast it. Both Mountains are tapped from casting it the first time and it's a
                // sorcery during the declare attackers step — so this cast is free and ignores
                // card-type timing, both only possible because it happens during resolution.
                game.selectCards(listOf(exiledNamed(game, "Volcanic Hammer")))
                withClue("The targeted sorcery pauses for its target as part of the free cast") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("The free-cast Volcanic Hammer resolves (17 → 14)") {
                    game.getLifeTotal(2) shouldBe 14
                }
                withClue("Cast from exile (not from hand), the first ability doesn't apply — it goes to the graveyard") {
                    namesInGraveyard(game, 1) shouldBe setOf("Volcanic Hammer")
                    namesInExile(game, 1) shouldBe emptySet()
                }
            }

            test("declining the cast leaves the card exiled with no lingering may-play permission") {
                val game = setupWithExiledHammer()

                game.declareAttackers(mapOf("Goliath Daydreamer" to 2))
                advanceToDreamDecision(game)

                val decision = game.getPendingDecision()
                decision as SelectCardsDecision
                game.selectCards(emptyList())
                game.resolveStack()

                withClue("The declined card stays in exile, dream counter intact") {
                    namesInExile(game, 1) shouldBe setOf("Volcanic Hammer")
                    dreamCounters(game, exiledNamed(game, "Volcanic Hammer")) shouldBe 1
                }
                withClue("Per the ruling you can't wait to cast it later — no may-play permission lingers") {
                    game.state.mayPlayPermissions
                        .filter { it.controllerId == game.player1Id }
                        .flatMap { it.cardIds }
                        .toSet() shouldBe emptySet<EntityId>()
                }
            }

            test("exiled cards without dream counters are not offered at all") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goliath Daydreamer", tapped = false, summoningSickness = false)
                    .withCardInExile(1, "Volcanic Hammer") // no dream counter
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Goliath Daydreamer" to 2))
                advanceToDreamDecision(game)

                withClue("With no dream-counter cards in exile the trigger resolves without any prompt") {
                    (game.getPendingDecision() is SelectCardsDecision) shouldBe false
                }
                withClue("The counterless card stays in exile and no permission was granted") {
                    namesInExile(game, 1) shouldBe setOf("Volcanic Hammer")
                    game.state.mayPlayPermissions
                        .filter { it.controllerId == game.player1Id }
                        .flatMap { it.cardIds }
                        .toSet() shouldBe emptySet<EntityId>()
                }
            }
        }
    }

    /**
     * Player1 casts Volcanic Hammer from hand with Goliath Daydreamer on the battlefield, so the
     * hammer ends up in exile with a dream counter (first ability) and both Mountains are tapped —
     * the attack-trigger tests then run with zero available mana, proving the cast is free.
     */
    private fun setupWithExiledHammer(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Goliath Daydreamer", tapped = false, summoningSickness = false)
            .withCardInHand(1, "Volcanic Hammer")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpellTargetingPlayer(1, "Volcanic Hammer", targetPlayerNumber = 2).error shouldBe null
        game.resolveStack()
        namesInExile(game, 1) shouldBe setOf("Volcanic Hammer")

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        return game
    }

    /** Pass priority until the attack trigger pauses for the cast choice (or combat moves on). */
    private fun advanceToDreamDecision(game: TestGame) {
        var iterations = 0
        while (game.state.pendingDecision == null &&
            game.state.step == Step.DECLARE_ATTACKERS &&
            iterations++ < 20
        ) {
            game.passPriority()
        }
    }

    private fun optionNames(game: TestGame, decision: SelectCardsDecision): Set<String> =
        decision.options.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }.toSet()

    private fun exiledNamed(game: TestGame, name: String): EntityId =
        game.state.getExile(game.player1Id).first {
            game.state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    private fun dreamCounters(game: TestGame, cardId: EntityId): Int =
        game.state.getEntity(cardId)?.get<CountersComponent>()?.getCount(CounterType.DREAM) ?: 0

    private fun namesInExile(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getExile(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }.toSet()
    }

    private fun namesInGraveyard(game: TestGame, playerNumber: Int): Set<String> {
        val playerId = if (playerNumber == 1) game.player1Id else game.player2Id
        return game.state.getGraveyard(playerId).mapNotNull {
            game.state.getEntity(it)?.get<CardComponent>()?.name
        }.toSet()
    }
}
