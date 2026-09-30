package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.CountersLockedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CapCounterPlacementThisTurn
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * [CapCounterPlacementThisTurn] — "If you would get one or more poison counters, instead you get one
 * poison counter and you can't get additional poison counters this turn" (Melira, the Living Cure).
 *
 * | Rule / ruling | Covered by |
 * |---|---|
 * | N ≥ 1 poison becomes exactly one | "three poison counters become one" |
 * | once applied, later placements this turn don't happen (Melira ruling 2) | "a second placement the same turn adds nothing" |
 * | the lock is the replacement's result — it outlives the source | "the lock survives the source leaving" |
 * | "this turn" — the lock is gone next turn | "the lock ends with the turn" |
 * | "you" — an opponent isn't protected | "an opponent is not capped" |
 * | toxic (CR 702.164c) is a placement too; simultaneous combat damage gives one | "two toxic attackers give one" |
 * | proliferate is a placement too | "proliferate can't add past the lock" |
 * | a player-recipient additive modifier is applied before the cap (the player's natural order) | "an additive modifier can't push past the cap" |
 */
class CapCounterPlacementThisTurnScenarioTest : ScenarioTestBase() {

    private val cap = card("Test Poison Cap") {
        manaCost = "{G}{W}"
        typeLine = "Creature — Human Scout"
        power = 3
        toughness = 3
        replacementEffect(CapCounterPlacementThisTurn())
    }

    private val poison = card("Test Poison Three") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val p = target(Targets.Player)
            effect = Effects.AddCounters(CounterType.POISON, 3, p)
        }
    }

    private val kill = card("Test Kill") {
        manaCost = "{B}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    private val proliferate = card("Test Proliferate") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell {
            effect = Effects.Proliferate()
        }
    }

    private val toxicTwo = card("Test Toxic Two") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Phyrexian Rat"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.toxic(2))
    }

    private val toxicThree = card("Test Toxic Three") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Phyrexian Rat"
        power = 1
        toughness = 1
        keywordAbility(KeywordAbility.toxic(3))
    }

    private val morePoison = card("Test More Poison") {
        manaCost = "{B}"
        typeLine = "Enchantment"
        replacementEffect(
            ModifyCounterPlacement(
                modifier = 2,
                appliesTo = EventPattern.CounterPlacementEvent(CounterType.POISON, Recipient.AnyPlayer)
            )
        )
    }

    private fun TestGame.poisonOf(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    private fun TestGame.poisonPlayer(caster: Int, victim: Int) {
        castSpellTargetingPlayer(caster, "Test Poison Three", victim).error shouldBe null
        resolveStack()
    }

    private fun board(active: Int = 2) = scenario()
        .withPlayers("Melira", "Opponent")
        .withCardOnBattlefield(1, "Test Poison Cap")
        .withCardInHand(2, "Test Poison Three")
        .withCardInHand(2, "Test Poison Three")
        .withLandsOnBattlefield(2, "Swamp", 4)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(active)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        listOf(cap, poison, kill, proliferate, toxicTwo, toxicThree, morePoison).forEach { cardRegistry.register(it) }

        test("three poison counters become one") {
            val game = board().build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.poisonOf(game.player1Id) shouldBe 1
        }

        test("a second placement the same turn adds nothing") {
            val game = board().build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.poisonPlayer(caster = 2, victim = 1)
            game.poisonOf(game.player1Id) shouldBe 1
        }

        test("the lock survives the source leaving the battlefield") {
            val game = board().withCardInHand(2, "Test Kill").build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.castSpell(2, "Test Kill", targetId = game.findPermanent("Test Poison Cap")!!).error shouldBe null
            game.resolveStack()
            game.findPermanent("Test Poison Cap") shouldBe null

            game.poisonPlayer(caster = 2, victim = 1)
            withClue("the cap is gone but the \"can't get additional\" lock remains") {
                game.poisonOf(game.player1Id) shouldBe 1
            }
        }

        test("the lock ends with the turn") {
            val game = board().build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.state.getEntity(game.player1Id)?.get<CountersLockedThisTurnComponent>().shouldNotBeNull()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.getEntity(game.player1Id)?.get<CountersLockedThisTurnComponent>() shouldBe null
        }

        test("without a lock, the cap applies afresh on a new turn") {
            val game = board(active = 1).withCardInHand(1, "Test Poison Three").withLandsOnBattlefield(1, "Swamp", 1)
                .build()
            // Opponent's poison on turn 1 locks player 1; next turn, a fresh placement is capped again.
            game.poisonPlayer(caster = 1, victim = 1)
            game.poisonOf(game.player1Id) shouldBe 1
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.poisonPlayer(caster = 2, victim = 1)
            game.poisonOf(game.player1Id) shouldBe 2
        }

        test("an opponent is not capped") {
            val game = board().build()
            game.poisonPlayer(caster = 2, victim = 2)
            game.poisonOf(game.player2Id) shouldBe 3
            game.state.getEntity(game.player2Id)?.get<CountersLockedThisTurnComponent>() shouldBe null
        }

        test("two toxic attackers dealing combat damage together give one poison counter") {
            val game = board()
                .withCardOnBattlefield(2, "Test Toxic Two", summoningSickness = false)
                .withCardOnBattlefield(2, "Test Toxic Three", summoningSickness = false)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Toxic Two" to 1, "Test Toxic Three" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(1) shouldBe 18
            game.poisonOf(game.player1Id) shouldBe 1
        }

        test("proliferate can't add past the lock") {
            val game = board().withCardInHand(2, "Test Proliferate").withLandsOnBattlefield(2, "Island", 1).build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.castSpell(2, "Test Proliferate").error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision
            if (decision is SelectCardsDecision) {
                game.selectCards(decision.options.filter { it == game.player1Id }).error shouldBe null
                game.resolveStack()
            }
            game.poisonOf(game.player1Id) shouldBe 1
        }

        test("an additive modifier can't push past the cap") {
            val game = board().withCardOnBattlefield(2, "Test More Poison").build()
            game.poisonPlayer(caster = 2, victim = 1)
            game.poisonOf(game.player1Id) shouldBe 1
        }
    }
}
