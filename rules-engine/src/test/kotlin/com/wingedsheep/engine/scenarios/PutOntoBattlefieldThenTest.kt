package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PutOntoBattlefieldThen
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [PutOntoBattlefieldThen] — "if an opponent would put a land onto the battlefield, that player
 * instead puts it onto the battlefield, then …" (Land Equilibrium's shape).
 *
 * Pins: the rider applies to a land play and to a land a spell puts there, and runs before anyone
 * gets priority (the player may sacrifice the land that just entered); the putter relation is to
 * the replacement's controller, so the controller's own lands are untouched; the condition reads
 * the board as it was before the land arrived; and the land still entered, so its enter trigger
 * fires.
 */
class PutOntoBattlefieldThenTest : ScenarioTestBase() {

    private val testEquilibrium = card("Test Equilibrium") {
        manaCost = "{2}{U}{U}"
        typeLine = "Enchantment"
        oracleText = "If an opponent who controls at least as many lands as you do would put a land onto the " +
            "battlefield, that player instead puts that land onto the battlefield then sacrifices a land of their choice."
        replacementEffect(
            PutOntoBattlefieldThen(
                putter = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.count(Player.TriggeringPlayer, Zone.BATTLEFIELD, GameObjectFilter.Land),
                    ComparisonOperator.GTE,
                    DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land)
                ),
                effect = Effects.Sacrifice(GameObjectFilter.Land, 1, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Land, to = Zone.BATTLEFIELD)
            )
        )
        metadata { rarity = Rarity.RARE }
    }

    private val testRamp = card("Test Ramp") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Search your library for a basic land card, put it onto the battlefield, then shuffle."
        spell {
            effect = com.wingedsheep.sdk.dsl.Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                destination = com.wingedsheep.sdk.scripting.effects.SearchDestination.BATTLEFIELD
            )
        }
        metadata { rarity = Rarity.COMMON }
    }

    private val testDoubleRamp = card("Test Double Ramp") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        oracleText = "Search your library for up to two basic land cards, put them onto the battlefield, then shuffle."
        spell {
            effect = com.wingedsheep.sdk.dsl.Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 2,
                destination = com.wingedsheep.sdk.scripting.effects.SearchDestination.BATTLEFIELD
            )
        }
        metadata { rarity = Rarity.COMMON }
    }

    private val testHeraldLand = card("Test Herald Land") {
        typeLine = "Land"
        oracleText = "When this land enters, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
        metadata { rarity = Rarity.COMMON }
    }

    /** Player 1 controls the Equilibrium and [youLands] Islands; player 2 is active with [theirLands] Forests. */
    private fun board(youLands: Int, theirLands: Int, vararg hand: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Test Equilibrium")
        .withLandsOnBattlefield(1, "Island", youLands)
        .withLandsOnBattlefield(2, "Forest", theirLands)
        .let { b -> hand.fold(b) { acc, name -> acc.withCardInHand(2, name) } }
        .withActivePlayer(2)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.lands(player: Int) =
        state.getBattlefield().count { id ->
            state.projectedState.hasType(id, "LAND") && state.projectedState.getController(id) == (if (player == 1) player1Id else player2Id)
        }

    init {
        listOf(testEquilibrium, testRamp, testDoubleRamp, testHeraldLand).forEach(cardRegistry::register)

        test("an opponent with at least as many lands plays a land, then sacrifices one — and may pick the new land") {
            val game = board(youLands = 2, theirLands = 2, "Mountain")
            val mountain = game.findCardsInHand(2, "Mountain").single()
            game.execute(PlayLand(game.player2Id, mountain)).error shouldBe null

            withClue("the rider asks before anyone gets priority") { game.hasPendingDecision() shouldBe true }
            game.state.pendingDecision!!.playerId shouldBe game.player2Id
            withClue("the land is on the battlefield, untapped, when the choice is made") {
                game.state.getBattlefield().contains(mountain) shouldBe true
                game.state.getEntity(mountain)!!.has<TappedComponent>() shouldBe false
            }
            game.selectCards(listOf(mountain)).error shouldBe null

            game.isInGraveyard(2, "Mountain") shouldBe true
            game.lands(2) shouldBe 2
        }

        test("the land count is compared before the land enters") {
            // 1 Forest vs 2 Islands: fewer lands, so no sacrifice — even though the new land makes it 2-2.
            val game = board(youLands = 2, theirLands = 1, "Mountain")
            game.execute(PlayLand(game.player2Id, game.findCardsInHand(2, "Mountain").single())).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.lands(2) shouldBe 2
        }

        test("the controller's own land play is not replaced") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Equilibrium")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInHand(1, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.execute(PlayLand(game.player1Id, game.findCardsInHand(1, "Island").single())).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.lands(1) shouldBe 2
        }

        test("a land an opponent's spell puts onto the battlefield is replaced too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Equilibrium")
                .withLandsOnBattlefield(1, "Island", 1)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInHand(2, "Test Ramp")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Test Ramp").error shouldBe null
            game.resolveStack()
            val plains = game.findCardsInLibrary(2, "Plains").single()
            game.selectCards(listOf(plains)).error shouldBe null

            withClue("the opponent now chooses a land to sacrifice") { game.hasPendingDecision() shouldBe true }
            val forest = game.findPermanents("Forest").first()
            game.selectCards(listOf(forest)).error shouldBe null
            game.isOnBattlefield("Plains") shouldBe true
            game.lands(2) shouldBe 2
        }

        test("lands entering together are counted against the board before the whole event") {
            // 2 Forests vs 3 Islands: neither fetched Plains is on the board when the other would enter.
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Test Equilibrium")
                .withLandsOnBattlefield(1, "Island", 3)
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInHand(2, "Test Double Ramp")
                .withCardInLibrary(2, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Test Double Ramp").error shouldBe null
            game.resolveStack()
            game.selectCards(game.findCardsInLibrary(2, "Plains")).error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.lands(2) shouldBe 4
        }

        test("the land still entered, so its enter trigger fires after the sacrifice") {
            val game = board(youLands = 1, theirLands = 1, "Test Herald Land")
            val herald = game.findCardsInHand(2, "Test Herald Land").single()
            game.execute(PlayLand(game.player2Id, herald)).error shouldBe null
            game.selectCards(listOf(herald)).error shouldBe null
            game.isInGraveyard(2, "Test Herald Land") shouldBe true
            game.state.stack shouldNotBe emptyList<Any>()
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 23
        }
    }
}
