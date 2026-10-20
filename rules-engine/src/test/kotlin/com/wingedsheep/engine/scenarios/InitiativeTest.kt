package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.Concede
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.InitiativeTakenEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.event.InitiativeAbilities
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Engine tests for **the initiative** (CR 726, Commander Legends: Battle for Baldur's Gate).
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 726.1 | no initiative until an effect hands it out | "the game starts with nobody holding it" |
 * | 726.2 (take) | whenever a player takes it, that player ventures into Undercity | "taking the initiative ventures into Undercity" |
 * | 726.5 | taking it while holding it triggers that venture again, no second designation | "taking it again ventures again" |
 * | 726.2 (upkeep) | at the beginning of the holder's upkeep, they venture into Undercity | "the holder ventures at their upkeep", "not at an opponent's upkeep" |
 * | 726.2 (steal) | one or more creatures a player controls dealing combat damage to the holder → that player takes it | "combat damage steals it", "several attackers steal once", "noncombat damage doesn't" |
 * | 726.3 | one holder at a time | "combat damage steals it" (the old holder loses it) |
 * | 726.4 | a holder who leaves hands it to the active player, who takes it — and ventures | "a holder who leaves the game …" |
 * | — | the initiative and the monarch are independent designations | "initiative and monarch coexist" |
 * | 701.49d | a player already in another dungeon advances it when they venture into Undercity | "in another dungeon, the initiative advances it" |
 */
class InitiativeTest : ScenarioTestBase() {

    private val takeIt = card("Test Take Initiative") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.TakeTheInitiative() }
    }

    private val crowning = card("Test Initiative Crowning") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.BecomeMonarch() }
    }

    private val reward = card("Test Initiative Reward") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell { effect = Effects.If(Conditions.YouHaveTheInitiative, Effects.GainLife(5)) }
    }

    private val bear = card("Test Initiative Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    private val sparker = card("Test Initiative Sparker") {
        manaCost = "{0}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    private val herald = card("Test Initiative Herald") {
        manaCost = "{1}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.you.takesTheInitiative()
            effect = Effects.GainLife(3)
        }
    }

    init {
        listOf(takeIt, crowning, reward, bear, sparker, herald).forEach { cardRegistry.register(it) }

        test("the game starts with nobody holding the initiative, and the condition reads false (CR 726.1)") {
            val game = initiativeGame { withCardInHand(1, "Test Initiative Reward") }
            game.state.initiative shouldBe null
            game.castSpell(1, "Test Initiative Reward").error shouldBe null
            game.resolveAll()
            game.getLifeTotal(1) shouldBe 20
        }

        test("taking the initiative ventures into Undercity (CR 726.2) and the condition reads it") {
            val game = initiativeGame {
                withCardInHand(1, "Test Take Initiative")
                withCardInHand(1, "Test Initiative Reward")
                withCardOnBattlefield(1, "Test Initiative Herald")
            }
            game.castSpell(1, "Test Take Initiative").error shouldBe null
            val events = game.resolveAll()

            game.state.initiative shouldBe game.player1Id
            events.filterIsInstance<InitiativeTakenEvent>().single().let {
                it.previousHolderId shouldBe null
                it.newHolderId shouldBe game.player1Id
                it.sourceName shouldBe "Test Take Initiative"
            }
            withClue("The inherent trigger entered Undercity's first room — no dungeon choice") {
                DungeonService.marker(game.state, game.player1Id)!!.let {
                    it.dungeon shouldBe "Undercity"
                    it.room shouldBe 0
                }
            }
            withClue("'When you take the initiative' triggered") { game.getLifeTotal(1) shouldBe 23 }

            game.castSpell(1, "Test Initiative Reward").error shouldBe null
            game.resolveAll()
            game.getLifeTotal(1) shouldBe 28
        }

        test("taking it again while holding it ventures again (CR 726.5)") {
            val game = initiativeGame { withCardsInHand(1, "Test Take Initiative", 2) }
            game.castSpell(1, "Test Take Initiative").error shouldBe null
            game.resolveAll()
            game.castSpell(1, "Test Take Initiative").error shouldBe null
            val events = game.resolveAll(roomChoice = "Lost Well")

            events.filterIsInstance<InitiativeTakenEvent>().single().previousHolderId shouldBe game.player1Id
            game.state.initiative shouldBe game.player1Id
            withClue("Secret Entrance → Lost Well: the second take ventured a second time") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 2
            }
        }

        test("the holder ventures into Undercity at the beginning of their upkeep (CR 726.2)") {
            val game = initiativeGame(activePlayer = 1) {}
            game.state = game.state.copy(initiative = game.player1Id)

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            withClue("Player 2's upkeep: player 1 has the initiative, so nothing triggers") {
                game.state.stack.size shouldBe 0
                DungeonService.marker(game.state, game.player1Id) shouldBe null
            }
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveAll()
            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Undercity"
        }

        test("combat damage to the holder steals the initiative, and the new holder ventures (CR 726.2, 726.3)") {
            val game = initiativeGame {
                withCardOnBattlefield(1, "Test Initiative Bear", summoningSickness = false)
            }
            game.state = game.state.copy(initiative = game.player2Id)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Initiative Bear" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            val events = game.resolveAll()

            game.getLifeTotal(2) shouldBe 18
            game.state.initiative shouldBe game.player1Id
            events.filterIsInstance<InitiativeTakenEvent>().single().let {
                it.previousHolderId shouldBe game.player2Id
                it.sourceName shouldBe "The Initiative"
            }
            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Undercity"
            DungeonService.marker(game.state, game.player2Id) shouldBe null
        }

        test("several attackers from one player steal it once — one batched trigger (CR 726.2)") {
            val game = initiativeGame {
                withCardOnBattlefield(1, "Test Initiative Bear", summoningSickness = false)
                withCardOnBattlefield(1, "Test Initiative Bear", summoningSickness = false)
            }
            game.state = game.state.copy(initiative = game.player2Id)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            val bears = game.findPermanents("Test Initiative Bear")
            game.execute(
                com.wingedsheep.engine.core.DeclareAttackers(game.player1Id, bears.associateWith { game.player2Id })
            ).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)

            withClue("One steal trigger on the stack for the one attacking player") {
                game.state.stack.count { id ->
                    game.state.getEntity(id)
                        ?.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()
                        ?.descriptionOverride == InitiativeAbilities.combatDamageSteal.descriptionOverride
                } shouldBe 1
            }
            game.resolveAll()
            game.getLifeTotal(2) shouldBe 16
            withClue("One take, one venture: the marker is still on the first room") {
                DungeonService.marker(game.state, game.player1Id)!!.room shouldBe 0
            }
        }

        test("noncombat damage to the holder doesn't steal it") {
            val game = initiativeGame { withCardInHand(1, "Test Initiative Sparker") }
            game.state = game.state.copy(initiative = game.player2Id)
            game.castSpell(1, "Test Initiative Sparker").error shouldBe null
            game.resolveAll()

            game.getLifeTotal(2) shouldBe 19
            game.state.initiative shouldBe game.player2Id
        }

        test("the initiative and the monarch are independent designations") {
            val game = initiativeGame(activePlayer = 2) {
                withCardInHand(2, "Test Initiative Crowning")
            }
            game.state = game.state.copy(initiative = game.player1Id)
            game.castSpell(2, "Test Initiative Crowning").error shouldBe null
            game.resolveAll()

            game.state.monarch shouldBe game.player2Id
            game.state.initiative shouldBe game.player1Id
        }

        test("in another dungeon, the initiative advances it instead of entering Undercity (CR 701.49d)") {
            val game = initiativeGame { withCardInHand(1, "Test Take Initiative") }
            game.state = DungeonService.enter(
                game.state, game.player1Id, com.wingedsheep.sdk.dungeons.Dungeons.DungeonOfTheMadMage, "test"
            ).first
            game.castSpell(1, "Test Take Initiative").error shouldBe null
            game.resolveAll()

            DungeonService.marker(game.state, game.player1Id)!!.let {
                it.dungeon shouldBe "Dungeon of the Mad Mage"
                it.room shouldBe 1
            }
        }

        test("a holder who leaves the game hands it to the active player, who takes it and ventures (CR 726.4)") {
            val registry = CardRegistry().also { it.register(TestCards.all) }
            val boot = GameInitializer(registry).initializeGame(
                GameConfig(
                    players = (1..4).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                    startingPlayerIndex = 0,
                    skipMulligans = true,
                )
            )
            val p = boot.playerIds
            val result = ActionProcessor(registry).process(boot.state.copy(initiative = p[2]), Concede(p[2])).result
            val state = result.newState

            state.gameOver shouldBe false
            state.initiative shouldBe p[0]
            result.events.filterIsInstance<InitiativeTakenEvent>().single().let {
                it.previousHolderId shouldBe p[2]
                it.newHolderId shouldBe p[0]
            }
            withClue("Taking it on the hand-off triggers the venture, which is now on the stack") {
                state.stack.size shouldBe 1
            }
        }
    }

    /**
     * Resolve the whole stack and answer the questions on the way: order simultaneous triggers as
     * offered, keep the order of cards put back on a library, fail to find on searches, take the
     * first card of a selection, and pick [roomChoice] (or the first room) at a fork. Returns every
     * event emitted.
     */
    private fun TestGame.resolveAll(roomChoice: String? = null): List<com.wingedsheep.engine.core.GameEvent> {
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 60) {
            val result = when (val d = state.pendingDecision) {
                null -> passPriority()
                is OrderObjectsDecision -> submitDecision(OrderedResponse(d.id, d.objects))
                is ReorderLibraryDecision -> submitDecision(OrderedResponse(d.id, d.cards))
                is SearchLibraryDecision -> submitDecision(CardsSelectedResponse(d.id, emptyList()))
                is SelectCardsDecision -> submitDecision(CardsSelectedResponse(d.id, d.options.take(d.minSelections)))
                is ChooseOptionDecision -> submitDecision(
                    OptionChosenResponse(d.id, roomChoice?.let { d.options.indexOf(it) }?.takeIf { it >= 0 } ?: 0)
                )
                else -> error("Unexpected decision $d")
            }
            events += result.events
        }
        return events
    }

    private fun initiativeGame(activePlayer: Int = 1, extra: ScenarioBuilder.() -> Unit): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(10) {
            builder.withCardInLibrary(1, "Test Initiative Bear")
            builder.withCardInLibrary(2, "Test Initiative Bear")
        }
        builder.withLandsOnBattlefield(1, "Plains", 2)
        builder.withLandsOnBattlefield(2, "Plains", 2)
        builder.extra()
        return builder.withActivePlayer(activePlayer).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
    }
}
