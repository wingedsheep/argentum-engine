package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.mechanics.battle.Battles
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The battle type predicate (CR 110.4 lists battle as a permanent type; CR 310 is the battle
 * section): `CardPredicate.IsBattle` / `GameObjectFilter.Battle` in filters and targets,
 * [Targets.PlayerOrBattle], and the "deals combat damage to a player or battle" recipient
 * [Recipient.AnyPlayerOrBattle].
 *
 * Board shape: Player (seat 1) controls the Siege, Opponent (seat 2) protects it.
 */
class BattlePredicateScenarioTest : ScenarioTestBase() {

    private val testSiege = card("Test Bulwark") {
        manaCost = "{2}{W}"
        colorIdentity = "W"
        typeLine = "Battle — Siege"
        startingDefense = 5
        oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack it.)"
    }

    private val destroyBattle = card("Test Battle Breaker") {
        manaCost = "{G}"
        colorIdentity = "G"
        typeLine = "Instant"
        oracleText = "Destroy target battle."
        spell {
            val t = target(TargetFilter.Battle)
            effect = Effects.Destroy(t)
        }
    }

    private val siegeShot = card("Test Siege Shot") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Test Siege Shot deals 2 damage to target player or battle."
        spell {
            val t = target(Targets.PlayerOrBattle)
            effect = Effects.DealDamage(2, t)
        }
    }

    private val raider = card("Test Siege Raider") {
        manaCost = "{1}{R}"
        colorIdentity = "R"
        typeLine = "Creature — Human Warrior"
        power = 2
        toughness = 2
        oracleText = "Whenever this creature deals combat damage to a player or battle, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrBattle)
            effect = Effects.GainLife(3)
        }
    }

    private fun defenseOf(game: TestGame): Int =
        game.findPermanent("Test Bulwark")
            ?.let { game.state.getEntity(it)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) }
            ?: 0

    private fun board(configure: (ScenarioBuilder) -> Unit = {}): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Test Bulwark")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        configure(builder)
        val game = builder.build()
        game.checkStateBasedActions()
        withClue("setup: the Siege is protected by the opponent (CR 310.12a)") {
            Battles.protectorOf(game.state, game.findPermanent("Test Bulwark")!!) shouldBe game.player2Id
        }
        return game
    }

    private fun TestGame.isBattle(id: EntityId): Boolean =
        services.predicateEvaluator.matches(
            state, state.projectedState, id, GameObjectFilter.Battle, PredicateContext(controllerId = player1Id)
        )

    private fun TestGame.cardInHand(playerNumber: Int, name: String): EntityId =
        state.getHand(if (playerNumber == 1) player1Id else player2Id)
            .first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** Runs combat to its end, declaring no blocks for the opponent. */
    private fun TestGame.finishCombat() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareNoBlockers()
        passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
    }

    init {
        cardRegistry.register(testSiege)
        cardRegistry.register(destroyBattle)
        cardRegistry.register(siegeShot)
        cardRegistry.register(raider)

        context("GameObjectFilter.Battle") {

            test("matches a battle on the battlefield and in hand, and nothing else") {
                val game = board {
                    it.withCardOnBattlefield(1, "Grizzly Bears")
                        .withCardOnBattlefield(1, "Ajani Goldmane")
                        .withCardInHand(1, "Test Bulwark")
                }
                withClue("the Siege on the battlefield is a battle") {
                    game.isBattle(game.findPermanent("Test Bulwark")!!) shouldBe true
                }
                withClue("a battle card in hand reads its printed type") {
                    game.isBattle(game.cardInHand(1, "Test Bulwark")) shouldBe true
                }
                withClue("a creature and a planeswalker are not battles") {
                    game.isBattle(game.findPermanent("Grizzly Bears")!!) shouldBe false
                    game.isBattle(game.findPermanent("Ajani Goldmane")!!) shouldBe false
                }
            }
        }

        context("target battle") {

            test("a battle is a legal target and is destroyed") {
                val game = board { it.withCardInHand(1, "Test Battle Breaker").withLandsOnBattlefield(1, "Forest", 1) }
                game.castSpell(1, "Test Battle Breaker", game.findPermanent("Test Bulwark")!!).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Test Bulwark") shouldBe false
                game.isInGraveyard(1, "Test Bulwark") shouldBe true
            }

            test("a creature is not a legal battle target") {
                val game = board {
                    it.withCardInHand(1, "Test Battle Breaker")
                        .withLandsOnBattlefield(1, "Forest", 1)
                        .withCardOnBattlefield(2, "Grizzly Bears")
                }
                game.castSpell(1, "Test Battle Breaker", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }

        context("target player or battle") {

            test("damage to a battle removes defense counters (CR 120.3h)") {
                val game = board { it.withCardInHand(1, "Test Siege Shot").withLandsOnBattlefield(1, "Mountain", 1) }
                game.castSpell(1, "Test Siege Shot", game.findPermanent("Test Bulwark")!!).error shouldBe null
                game.resolveStack()
                defenseOf(game) shouldBe 3
            }

            test("a player is a legal target") {
                val game = board { it.withCardInHand(1, "Test Siege Shot").withLandsOnBattlefield(1, "Mountain", 1) }
                game.execute(
                    CastSpell(game.player1Id, game.cardInHand(1, "Test Siege Shot"), listOf(ChosenTarget.Player(game.player2Id)))
                ).error shouldBe null
                game.resolveStack()
                game.getLifeTotal(2) shouldBe 18
            }

            test("a creature is not") {
                val game = board {
                    it.withCardInHand(1, "Test Siege Shot")
                        .withLandsOnBattlefield(1, "Mountain", 1)
                        .withCardOnBattlefield(2, "Grizzly Bears")
                }
                game.castSpell(1, "Test Siege Shot", game.findPermanent("Grizzly Bears")!!).error shouldNotBe null
            }
        }

        context("deals combat damage to a player or battle") {

            test("fires on combat damage to a battle") {
                val game = board { it.withCardOnBattlefield(1, "Test Siege Raider", summoningSickness = false) }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Test Siege Raider" to "Test Bulwark")
                ).error shouldBe null
                game.finishCombat()
                defenseOf(game) shouldBe 3
                game.getLifeTotal(1) shouldBe 23
            }

            test("fires on combat damage to a player") {
                val game = board { it.withCardOnBattlefield(1, "Test Siege Raider", summoningSickness = false) }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Test Siege Raider" to 2)).error shouldBe null
                game.finishCombat()
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 23
            }

            test("does not fire on combat damage to a blocking creature") {
                val game = board {
                    it.withCardOnBattlefield(1, "Test Siege Raider", summoningSickness = false)
                        .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                }
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Test Siege Raider" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Siege Raider"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
