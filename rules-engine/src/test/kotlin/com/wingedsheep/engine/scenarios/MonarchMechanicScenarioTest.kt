package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.MonarchChangedEvent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for **the monarch** (CR 725, Conspiracy: Take the Crown).
 *
 * | Rule | What it says | Covered by |
 * |---|---|---|
 * | 725.1 | no monarch until an effect makes one | "the game starts with no monarch" |
 * | 725.2 (draw) | at the beginning of the monarch's end step, that player draws | "the monarch draws …", "not on an opponent's end step" |
 * | 725.2 (steal) | a creature dealing combat damage to the monarch crowns its controller | "combat damage steals", "noncombat damage doesn't" |
 * | 608.2h | "its controller" of a creature gone by resolution is its last-known controller | "destroyed in response" |
 * | 725.2 (control) | the inherent abilities are controlled by who was monarch when they triggered | "still draws after losing the crown" (ruling) |
 * | 725.3 | only one monarch; becoming it un-crowns the old one | "only one player can be the monarch" |
 * | ruling | becoming the monarch while already it changes nothing | "becoming the monarch again" |
 *
 * CR 725.4 (the monarch leaving the game) needs a pod that continues — see `MonarchMultiplayerTest`.
 */
class MonarchMechanicScenarioTest : ScenarioTestBase() {

    private val crowning = card("Test Crowning") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.BecomeMonarch() }
    }

    /** Instant "you become the monarch" — lets the non-monarch take the crown mid-end-step. */
    private val usurp = card("Test Usurp") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell { effect = Effects.BecomeMonarch() }
    }

    private val bear = card("Test Monarch Bear") {
        manaCost = "{2}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    /** A creature dealing *noncombat* damage to each opponent as it enters. */
    private val sparker = card("Test Monarch Sparker") {
        manaCost = "{1}"
        typeLine = "Creature — Elemental"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }

    private val slay = card("Test Monarch Slay") {
        manaCost = "{1}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    /** Sorcery: gain 5 life if you're the monarch — reads the condition at resolution. */
    private val royalTithe = card("Test Royal Tithe") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(Conditions.YouAreTheMonarch, Effects.GainLife(5))
        }
    }

    init {
        listOf(crowning, usurp, bear, sparker, slay, royalTithe).forEach { cardRegistry.register(it) }

        test("the game starts with no monarch, and the conditions read false (CR 725.1)") {
            val game = monarchGame {
                withCardInHand(1, "Test Royal Tithe")
            }
            game.state.monarch shouldBe null

            game.castSpell(1, "Test Royal Tithe").error shouldBe null
            game.resolveStack()
            withClue("Nobody is the monarch, so 'if you're the monarch' is false") {
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("an effect makes its controller the monarch and the condition reads it") {
            val game = monarchGame {
                withCardInHand(1, "Test Crowning")
                withCardInHand(1, "Test Royal Tithe")
            }
            game.castSpell(1, "Test Crowning").error shouldBe null
            val results = game.resolveStack()
            game.state.monarch shouldBe game.player1Id
            results.flatMap { it.events }.filterIsInstance<MonarchChangedEvent>().single().let {
                it.previousMonarchId shouldBe null
                it.newMonarchId shouldBe game.player1Id
                it.sourceName shouldBe "Test Crowning"
            }

            game.castSpell(1, "Test Royal Tithe").error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 25
        }

        test("becoming the monarch while already it changes nothing and emits no event") {
            val game = monarchGame {
                withCardsInHand(1, "Test Crowning", 2)
            }
            game.castSpell(1, "Test Crowning")
            game.resolveStack()
            game.castSpell(1, "Test Crowning").error shouldBe null
            val again = game.resolveStack()
            game.state.monarch shouldBe game.player1Id
            again.flatMap { it.events }.filterIsInstance<MonarchChangedEvent>() shouldBe emptyList()
        }

        test("the monarch draws a card at the beginning of their end step (CR 725.2)") {
            val game = monarchGame { withCardInHand(1, "Test Crowning") }
            game.castSpell(1, "Test Crowning")
            game.resolveStack()
            val handBefore = game.handSize(1)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("The inherent end-step trigger draws exactly one card") {
                game.handSize(1) shouldBe handBefore + 1
            }
        }

        test("the monarch doesn't draw at an opponent's end step") {
            val game = monarchGame(activePlayer = 2) {}
            // Player 2 is active; crown player 1 directly so only "the monarch's end step" is tested.
            game.state = game.state.copy(monarch = game.player1Id)
            val p1Hand = game.handSize(1)
            val p2Hand = game.handSize(2)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.handSize(1) shouldBe p1Hand
            withClue("The active player isn't the monarch, so their end step draws nothing") {
                game.handSize(2) shouldBe p2Hand
            }
        }

        test("a creature's combat damage to the monarch makes its controller the monarch (CR 725.2)") {
            val game = monarchGame {
                withCardOnBattlefield(1, "Test Monarch Bear", summoningSickness = false)
            }
            game.state = game.state.copy(monarch = game.player2Id)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Monarch Bear" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            withClue("The steal trigger hands the crown to the attacking creature's controller") {
                game.state.monarch shouldBe game.player1Id
            }
        }

        test("a creature destroyed in response still crowns its last-known controller (CR 608.2h)") {
            val game = monarchGame {
                withCardOnBattlefield(1, "Test Monarch Bear", summoningSickness = false)
                withCardInHand(2, "Test Monarch Slay")
            }
            game.state = game.state.copy(monarch = game.player2Id)

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Test Monarch Bear" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            withClue("The steal trigger is waiting on the stack") {
                game.state.stack.size shouldBe 1
            }
            val bearId = game.findPermanent("Test Monarch Bear")!!
            // The active player passes; the monarch kills the attacker in response.
            game.passPriority()
            game.castSpell(2, "Test Monarch Slay", targetId = bearId).error shouldBe null
            game.resolveStack()

            game.findPermanent("Test Monarch Bear") shouldBe null
            game.state.monarch shouldBe game.player1Id
        }

        test("noncombat damage from a creature doesn't steal the crown") {
            val game = monarchGame {
                withCardInHand(1, "Test Monarch Sparker")
            }
            game.state = game.state.copy(monarch = game.player2Id)

            game.castSpell(1, "Test Monarch Sparker").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.state.monarch shouldBe game.player2Id
        }

        test("only one player can be the monarch (CR 725.3)") {
            val game = monarchGame(activePlayer = 2) {
                withCardInHand(2, "Test Crowning")
            }
            game.state = game.state.copy(monarch = game.player1Id)

            game.castSpell(2, "Test Crowning").error shouldBe null
            game.resolveStack()

            game.state.monarch shouldBe game.player2Id
        }

        test("a monarch who loses the crown with the draw trigger on the stack still draws (ruling)") {
            val game = monarchGame {
                withCardInHand(1, "Test Crowning")
                withCardInHand(2, "Test Usurp")
            }
            game.castSpell(1, "Test Crowning")
            game.resolveStack()
            val p1Hand = game.handSize(1)

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("The end-step draw trigger is waiting on the stack") {
                game.state.stack.size shouldBe 1
            }
            // Player 1 passes; player 2 responds by taking the crown.
            game.passPriority()
            game.castSpell(2, "Test Usurp").error shouldBe null
            game.resolveStack()

            game.state.monarch shouldBe game.player2Id
            withClue("The trigger is controlled by who was the monarch when it triggered (CR 725.2)") {
                game.handSize(1) shouldBe p1Hand + 1
            }
        }
    }

    private fun monarchGame(
        activePlayer: Int = 1,
        extra: ScenarioBuilder.() -> Unit
    ): TestGame {
        val builder = scenario().withPlayers("Player", "Opponent")
        repeat(10) {
            builder.withCardInLibrary(1, "Test Monarch Bear")
            builder.withCardInLibrary(2, "Test Monarch Bear")
        }
        builder.withLandsOnBattlefield(1, "Plains", 2)
        builder.withLandsOnBattlefield(2, "Plains", 2)
        builder.extra()
        val game = builder
            .withActivePlayer(activePlayer)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        return game
    }
}
