package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine coverage for [com.wingedsheep.sdk.scripting.effects.CantBeBlockedGroupEffect] —
 * "creatures you control can't be blocked this turn".
 *
 * "Can't be blocked" changes no characteristic, so CR 611.2c does not lock the affected set in at
 * resolution: creatures that come under your control later in the turn are covered too. And "you"
 * is the controller of the resolved spell — a sorcery sitting in the graveyard has no controller
 * of its own to read, which is exactly the source-gone case these tests resolve through.
 */
class CantBeBlockedGroupTest : ScenarioTestBase() {

    private val sneakAttack = card("Test Unseen Advance") {
        manaCost = "{U}"
        typeLine = "Sorcery"
        oracleText = "Creatures you control can't be blocked this turn."
        spell {
            effect = Effects.CantBeBlockedGroup(GroupFilter.AllCreaturesYouControl)
        }
    }

    private val threaten = card("Test Borrow") {
        manaCost = "{R}"
        typeLine = "Sorcery"
        oracleText = "Gain control of target creature until end of turn. It gains haste until end of turn."
        spell {
            val creature = target(TargetFilter.Creature)
            effect = Effects.GainControl(creature, Duration.EndOfTurn) then
                Effects.GrantKeyword(Keyword.HASTE, creature)
        }
    }

    private val sprinter = card("Test Sprinter") {
        manaCost = "{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
        keywords(Keyword.HASTE)
    }

    private val bruiser = card("Test Bruiser") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val guard = card("Test Guard") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Soldier"
        power = 0
        toughness = 4
    }

    private fun bruiserOf(game: TestGame, player: com.wingedsheep.sdk.model.EntityId) =
        game.state.getBattlefield(player).single { game.state.getEntity(it)?.get<CardComponent>()?.name == "Test Bruiser" }

    init {
        cardRegistry.register(listOf(sneakAttack, threaten, sprinter, bruiser, guard))

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Test Unseen Advance")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(1, "Test Bruiser", summoningSickness = false)
            .withCardOnBattlefield(2, "Test Bruiser")
            .withCardOnBattlefield(2, "Test Guard")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("covers your creatures after the spell is gone, and not the opponent's") {
            val game = board().build()
            game.castSpell(1, "Test Unseen Advance").error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val mine = bruiserOf(game, game.player1Id)
            val theirs = bruiserOf(game, game.player2Id)
            withClue("the resolved sorcery is in the graveyard, yet its \"you\" still covers your creature") {
                projected.hasKeyword(mine, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }
            withClue("the opponent's creature is not covered") {
                projected.hasKeyword(theirs, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Bruiser" to 2)).error shouldBe null
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            withClue("the Guard can't block the covered attacker") {
                game.declareBlockers(mapOf("Test Guard" to listOf("Test Bruiser"))).error shouldNotBe null
            }
        }

        test("a creature that enters after resolution is covered too (CR 611.2c)") {
            val game = board()
                .withCardInHand(1, "Test Sprinter")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            game.castSpell(1, "Test Unseen Advance").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Test Sprinter").error shouldBe null
            game.resolveStack()

            val sprinterId = game.findPermanent("Test Sprinter")!!
            game.state.projectedState.hasKeyword(sprinterId, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }

        test("a creature you gain control of after resolution is covered too") {
            val game = board()
                .withCardInHand(1, "Test Borrow")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()
            val stolen = bruiserOf(game, game.player2Id)

            game.castSpell(1, "Test Unseen Advance").error shouldBe null
            game.resolveStack()
            withClue("before the steal it is the opponent's and uncovered") {
                game.state.projectedState.hasKeyword(stolen, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
            }

            game.castSpell(1, "Test Borrow", stolen).error shouldBe null
            game.resolveStack()
            withClue("after the steal it is yours and covered") {
                game.state.projectedState.hasKeyword(stolen, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
            }
        }

        test("the restriction ends with the turn") {
            val game = board().build()
            game.castSpell(1, "Test Unseen Advance").error shouldBe null
            game.resolveStack()
            val mine = bruiserOf(game, game.player1Id)
            game.state.projectedState.hasKeyword(mine, AbilityFlag.CANT_BE_BLOCKED) shouldBe true

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.hasKeyword(mine, AbilityFlag.CANT_BE_BLOCKED) shouldBe false
        }
    }
}
