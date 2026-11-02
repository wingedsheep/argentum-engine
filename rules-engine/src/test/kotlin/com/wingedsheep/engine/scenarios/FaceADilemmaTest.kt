package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.BudgetModalResponse
import com.wingedsheep.engine.core.CancelDecisionResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DilemmaFacedEvent
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.DilemmasFacedThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.matchers.shouldBe

/**
 * "Face a dilemma" (Star Trek): "You face a dilemma as you choose one or more modes for a spell or
 * ability." One dilemma per mode choice — casting a modal spell, activating a modal ability, putting
 * a modal trigger on the stack, a mode choice made while an ability resolves — however many modes
 * it took; a random mode, a gift-style yes/no and a non-modal spell are not dilemmas. The per-turn
 * count ([TurnTracker.DILEMMAS_FACED]) resets each turn, and "whenever you face a dilemma" watches
 * only its controller's choices.
 */
class FaceADilemmaTest : ScenarioTestBase() {

    private fun gain(n: Int) = Mode.noTarget(Effects.GainLife(n), "Gain $n life")
    private val draw = Mode.noTarget(Effects.DrawCards(1), "Draw a card")

    private val watcher = card("Test Dilemma Watcher") {
        manaCost = "{0}"
        typeLine = "Creature — Advisor"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.you.facesADilemma()
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        }
    }
    private val chooseOne = card("Test Dilemma Charm") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = ModalEffect.chooseOne(gain(1), draw) }
    }
    private val chooseTwo = card("Test Dilemma Command") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = ModalEffect(modes = listOf(gain(1), draw, gain(2)), chooseCount = 2) }
    }
    private val plain = card("Test Plain Draw") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }
    private val giftLike = card("Test Gift Choice") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = ModalEffect.chooseOne(gain(1), draw).copy(countsAsModalSpell = false) }
    }
    private val nested = card("Test Nested Choice") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = CompositeEffect(listOf(Effects.GainLife(1), ModalEffect.chooseOne(gain(1), draw))) }
    }
    private val nestedTargeted = card("Test Nested Targeted Choice") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = CompositeEffect(listOf(Effects.GainLife(1), ModalEffect.chooseOne(
                mode("Tap target creature") {
                    val creature = target(TargetFilter.Creature)
                    effect = Effects.Tap(creature)
                },
                draw
            )))
        }
    }
    private val budget = card("Test Budget Modal") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = Effects.BudgetModal(budget = 2, modes = listOf(
                BudgetMode(cost = 1, effect = Effects.GainLife(1), description = "Gain 1 life")
            ))
        }
    }
    private val modalArtifact = card("Test Dilemma Artifact") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = ModalEffect.chooseOne(gain(1), draw)
        }
    }
    private val modalEtb = card("Test Dilemma Arrival") {
        manaCost = "{0}"; typeLine = "Artifact Creature — Construct"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = ModalEffect.chooseOne(gain(1), draw)
        }
    }
    private val randomEtb = card("Test Random Arrival") {
        manaCost = "{0}"; typeLine = "Artifact Creature — Construct"
        power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = ModalEffect.chooseOneAtRandom(gain(1), gain(2))
        }
    }
    private val counter = card("Test Dilemma Ledger") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(DynamicAmount.TurnTracking(Player.You, TurnTracker.DILEMMAS_FACED))
        }
    }
    private val gate = card("Test Dilemma Gate") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.If(Conditions.YouFacedADilemmaThisTurn(), Effects.DrawCards(1))
        }
    }

    init {
        cardRegistry.register(listOf(watcher, chooseOne, chooseTwo, plain, giftLike, nested, nestedTargeted, budget, modalArtifact,
            modalEtb, randomEtb, counter, gate))

        fun game(active: Int = 1, vararg hand: Pair<Int, String>, battlefield: List<Pair<Int, String>> = emptyList()): TestGame {
            val b = scenario().withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, watcher.name)
                .withActivePlayer(active)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(5) { b.withCardInLibrary(1, "Forest"); b.withCardInLibrary(2, "Forest") }
            hand.forEach { (p, n) -> b.withCardInHand(p, n) }
            battlefield.forEach { (p, n) -> b.withCardOnBattlefield(p, n) }
            return b.build()
        }
        fun TestGame.dilemmas(player: Int): Int =
            state.getEntity(if (player == 1) player1Id else player2Id)!!.get<DilemmasFacedThisTurnComponent>()?.count ?: 0
        fun TestGame.cardInHand(player: Int, name: String): EntityId = findCardsInHand(player, name).first()
        fun TestGame.cast(player: Int, name: String, modes: List<Int>) = execute(
            CastSpell(if (player == 1) player1Id else player2Id, cardInHand(player, name),
                chosenModes = modes, modeTargetsOrdered = modes.map { emptyList() })
        )
        fun TestGame.activate(name: String, modes: List<Int>? = null) = execute(
            ActivateAbility(player1Id, findPermanent(name)!!, cardRegistry.getCard(name)!!.activatedAbilities.single().id,
                chosenModes = modes, modeTargetsOrdered = modes?.map { emptyList() } ?: emptyList())
        )

        test("casting a choose-one spell faces one dilemma and fires the watcher once") {
            val g = game(1, 1 to chooseOne.name)
            val result = g.cast(1, chooseOne.name, listOf(0))
            result.error shouldBe null
            result.events.filterIsInstance<DilemmaFacedEvent>().map { it.playerId } shouldBe listOf(g.player1Id)
            g.dilemmas(1) shouldBe 1
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 19
            g.getLifeTotal(1) shouldBe 21
        }

        test("choosing two modes is still one dilemma") {
            val g = game(1, 1 to chooseTwo.name)
            g.cast(1, chooseTwo.name, listOf(0, 2)).error shouldBe null
            g.dilemmas(1) shouldBe 1
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 19
        }

        test("a non-modal spell and a gift-style choice face no dilemma") {
            val g = game(1, 1 to plain.name, 1 to giftLike.name)
            g.cast(1, plain.name, emptyList()).error shouldBe null
            g.resolveStack()
            g.cast(1, giftLike.name, listOf(1)).error shouldBe null
            g.resolveStack()
            g.dilemmas(1) shouldBe 0
            g.getLifeTotal(2) shouldBe 20
        }

        test("activating a modal ability faces a dilemma") {
            val g = game(1, battlefield = listOf(1 to modalArtifact.name))
            g.activate(modalArtifact.name, listOf(1)).error shouldBe null
            g.dilemmas(1) shouldBe 1
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 19
        }

        test("putting a modal trigger on the stack faces a dilemma; a random mode does not") {
            val g = game(1, 1 to modalEtb.name, 1 to randomEtb.name)
            g.cast(1, modalEtb.name, emptyList()).error shouldBe null
            repeat(2) { g.passPriority() }
            val pick = g.getPendingDecision() as ChooseOptionDecision
            g.submitDecision(OptionChosenResponse(pick.id, 0)).error shouldBe null
            g.dilemmas(1) shouldBe 1
            g.resolveStack()
            g.cast(1, randomEtb.name, emptyList()).error shouldBe null
            g.resolveStack()
            g.dilemmas(1) shouldBe 1
            g.getLifeTotal(2) shouldBe 19
        }

        test("a mode chosen while an ability resolves faces a dilemma") {
            val g = game(1, 1 to nested.name)
            g.cast(1, nested.name, emptyList()).error shouldBe null
            g.dilemmas(1) shouldBe 0
            repeat(2) { g.passPriority() }
            val pick = g.getPendingDecision() as ChooseOptionDecision
            g.submitDecision(OptionChosenResponse(pick.id, 1)).error shouldBe null
            g.dilemmas(1) shouldBe 1
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 19
        }

        test("cancelling back out of a mode's target prompt and re-picking is still one dilemma") {
            val g = game(1, 1 to nestedTargeted.name)
            g.cast(1, nestedTargeted.name, emptyList()).error shouldBe null
            repeat(2) { g.passPriority() }
            val first = g.getPendingDecision() as ChooseOptionDecision
            g.submitDecision(OptionChosenResponse(first.id, 0)).error shouldBe null
            val targets = g.getPendingDecision() as ChooseTargetsDecision
            g.submitDecision(CancelDecisionResponse(targets.id)).error shouldBe null
            val again = g.getPendingDecision() as ChooseOptionDecision
            g.submitDecision(OptionChosenResponse(again.id, 1)).error shouldBe null
            g.resolveStack()
            g.dilemmas(1) shouldBe 1
            g.getLifeTotal(2) shouldBe 19
        }

        test("picking a budget modal's modes faces one dilemma") {
            val g = game(1, 1 to budget.name)
            g.cast(1, budget.name, emptyList()).error shouldBe null
            repeat(2) { g.passPriority() }
            val pick = g.getPendingDecision()!!
            g.submitDecision(BudgetModalResponse(pick.id, listOf(0, 0))).error shouldBe null
            g.resolveStack()
            g.dilemmas(1) shouldBe 1
            g.getLifeTotal(2) shouldBe 19
        }

        test("an opponent's dilemma counts for them and doesn't fire your watcher") {
            val g = game(2, 2 to chooseOne.name)
            g.cast(2, chooseOne.name, listOf(0)).error shouldBe null
            g.resolveStack()
            g.dilemmas(2) shouldBe 1
            g.dilemmas(1) shouldBe 0
            g.getLifeTotal(2) shouldBe 21
        }

        test("the count feeds amounts and conditions, and resets next turn") {
            val g = game(1, 1 to chooseOne.name, 1 to chooseTwo.name,
                battlefield = listOf(1 to counter.name, 1 to gate.name))
            g.cast(1, chooseOne.name, listOf(1)).error shouldBe null
            g.resolveStack()
            g.cast(1, chooseTwo.name, listOf(1, 0)).error shouldBe null
            g.resolveStack()
            g.dilemmas(1) shouldBe 2
            val life = g.getLifeTotal(1)
            g.activate(counter.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe life + 2
            val hand = g.handSize(1)
            g.activate(gate.name).error shouldBe null
            g.resolveStack()
            g.handSize(1) shouldBe hand + 1
            g.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            g.dilemmas(1) shouldBe 0
        }
    }
}
