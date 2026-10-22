package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CardBuilder
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceLifeReductionToZero
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

/**
 * [ReplaceLifeReductionToZero]: every reduction of a protected life total that would leave it at 0 or
 * less — damage, "loses N life", a life payment, an effect that sets the total — is replaced whole by
 * the ability's `instead`, which runs before state-based actions.
 */
class ReplaceLifeReductionToZeroTest : ScenarioTestBase() {
    private fun permanent(name: String, block: CardBuilder.() -> Unit) = card(name) {
        manaCost = "{0}"; typeLine = "Artifact"; block()
    }
    private fun instant(name: String, effect: com.wingedsheep.sdk.scripting.effects.Effect) = card(name) {
        manaCost = "{0}"; typeLine = "Instant"
        spell { this.effect = effect }
    }

    /** "If your life total would be reduced to 0 or less, instead your life total becomes 3 and you draw a card." */
    private val phoenix = permanent("Phoenix Probe") {
        replacementEffect(ReplaceLifeReductionToZero(instead = Effects.SetLifeTotal(3) then Effects.DrawCards(1)))
    }

    /** Enduring Angel's replacement on a permanent that can't transform (CR 701.27c). */
    private val singleFacedAngel = permanent("Single-Faced Angel Probe") {
        replacementEffect(
            ReplaceLifeReductionToZero(
                instead = Effects.IfYouDo(
                    action = Effects.Transform(EffectTarget.Self),
                    then = Effects.SetLifeTotal(3),
                    otherwise = Effects.SetLifeTotal(3) then Effects.LoseGame(EffectTarget.Controller),
                    successCriterion = SuccessCriterion.Transformed
                )
            )
        )
    }

    /** Protects every player, not just its controller; "that player" is the rider's triggering player. */
    private val guardian = permanent("Guardian Probe") {
        replacementEffect(
            ReplaceLifeReductionToZero(
                instead = Effects.SetLifeTotal(3, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                appliesTo = EventPattern.LifeLossEvent(Player.Each)
            )
        )
    }

    private val payer = permanent("Life Payer Probe") {
        activatedAbility {
            cost = Costs.PayLife(2)
            effect = Effects.DrawCards(1)
        }
    }

    private val loseFive = instant("Lose Five Probe", Effects.LoseLife(5, EffectTarget.Controller))
    private val loseTwo = instant("Lose Two Probe", Effects.LoseLife(2, EffectTarget.Controller))
    private val burnSelf = instant("Burn Self Probe", Effects.DealDamage(5, EffectTarget.Controller))
    private val setZero = instant("Set Zero Probe", Effects.SetLifeTotal(0))
    private val opponentLosesFive = instant("Opponent Loses Five Probe",
        Effects.LoseLife(5, EffectTarget.PlayerRef(Player.EachOpponent)))

    private fun game(life: Int, vararg onBattlefield: String, inHand: String? = null): TestGame {
        val b = scenario().withPlayers()
            .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .withLifeTotal(1, life)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        onBattlefield.forEach { b.withCardOnBattlefield(1, it) }
        inHand?.let { b.withCardInHand(1, it) }
        return b.build()
    }

    private fun TestGame.cast(name: String) {
        castSpell(1, name).error shouldBe null
        resolveStack()
    }

    init {
        listOf(phoenix, singleFacedAngel, guardian, payer, loseFive, loseTwo, burnSelf, setZero, opponentLosesFive)
            .forEach { cardRegistry.register(it) }

        test("lethal life loss is replaced whole; the rest of the replacement runs") {
            val g = game(2, phoenix.name, inHand = loseFive.name)
            val hand = g.handSize(1)
            g.cast(loseFive.name)
            g.state.gameOver shouldBe false
            g.getLifeTotal(1) shouldBe 3
            g.handSize(1) shouldBe hand
        }

        test("a reduction to exactly 0 is replaced") {
            val g = game(2, phoenix.name, inHand = loseTwo.name)
            g.cast(loseTwo.name)
            g.getLifeTotal(1) shouldBe 3
        }

        test("a reduction that leaves life above 0 isn't replaced") {
            val g = game(10, phoenix.name, inHand = loseTwo.name)
            g.cast(loseTwo.name)
            g.getLifeTotal(1) shouldBe 8
        }

        test("lethal noncombat damage is replaced") {
            val g = game(4, phoenix.name, inHand = burnSelf.name)
            g.cast(burnSelf.name)
            g.state.gameOver shouldBe false
            g.getLifeTotal(1) shouldBe 3
        }

        test("an effect setting the life total to 0 is a reduction (CR 119.5) and is replaced") {
            val g = game(10, phoenix.name, inHand = setZero.name)
            g.cast(setZero.name)
            g.getLifeTotal(1) shouldBe 3
        }

        test("paying life down to 0 is replaced; the cost is still paid") {
            val g = game(2, phoenix.name, payer.name)
            val hand = g.handSize(1)
            val payerId = g.findPermanent(payer.name)!!
            val abilityId = cardRegistry.getCard(payer.name)!!.script.activatedAbilities.single().id
            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = payerId, abilityId = abilityId)).error shouldBe null
            g.resolveStack()
            g.state.gameOver shouldBe false
            g.getLifeTotal(1) shouldBe 3
            // One draw from the replacement, one from the ability.
            g.handSize(1) shouldBe hand + 2
        }

        test("'you' is the source's controller — an opponent's lethal loss isn't replaced") {
            val g = scenario().withPlayers()
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withCardOnBattlefield(1, phoenix.name).withCardInHand(1, opponentLosesFive.name)
                .withLifeTotal(2, 5)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            g.cast(opponentLosesFive.name)
            g.state.gameOver shouldBe true
            g.state.winnerId shouldBe g.player1Id
        }

        test("appliesTo can protect every player") {
            val g = scenario().withPlayers()
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .withCardOnBattlefield(1, guardian.name).withCardInHand(1, opponentLosesFive.name)
                .withLifeTotal(2, 5)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            g.cast(opponentLosesFive.name)
            g.state.gameOver shouldBe false
            g.getLifeTotal(2) shouldBe 3
            g.getLifeTotal(1) shouldBe 20
        }

        test("a permanent that can't transform: life becomes 3, then its controller loses the game") {
            val g = game(2, singleFacedAngel.name, inHand = loseFive.name)
            g.cast(loseFive.name)
            g.state.gameOver shouldBe true
            g.state.winnerId shouldBe g.player2Id
        }
    }
}
