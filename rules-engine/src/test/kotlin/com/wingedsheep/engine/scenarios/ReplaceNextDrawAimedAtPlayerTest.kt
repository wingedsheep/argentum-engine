package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `Effects.ReplaceNextDraw(effect, player)` aimed at a player other than its controller — the
 * "the next time *they* would draw a card this turn, instead …" shape (Urabrask, Heretic Praetor).
 *
 * Pins: only the named player's draw is replaced (the controller's own draws aren't); the shield is
 * spent by that one draw ("the next time"); the
 * replacement stays its controller's ("you" is still the caster); the replacement effect reads the
 * player whose draw it replaced as `Player.TriggeringPlayer`; and the shield ends with the turn.
 */
class ReplaceNextDrawAimedAtPlayerTest : FunSpec({

    val youGainForTheirDraw = card("You Gain For Their Draw") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val p = target(Targets.Player)
            effect = Effects.ReplaceNextDraw(Effects.GainLife(3), player = p)
        }
    }
    val theyLoseForTheirDraw = card("They Lose For Their Draw") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            val p = target(Targets.Player)
            effect = Effects.ReplaceNextDraw(
                Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
                player = p
            )
        }
    }
    val eachPlayerDraws = card("Each Player Draws One") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.Each)) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(youGainForTheirDraw, theyLoseForTheirDraw, eachPlayerDraws))
        initMirrorMatch(Deck.of("Forest" to 40)); passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.cast(p: EntityId, name: String, targets: List<EntityId> = emptyList()) {
        castSpell(p, putCardInHand(p, name), targets).error shouldBe null
        bothPass()
    }

    test("only the named player's next draw is replaced, once; the caster stays \"you\"") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.cast(me, youGainForTheirDraw.name, listOf(opp))
        val (myHand, oppHand) = d.getHandSize(me) to d.getHandSize(opp)
        val (myLife, oppLife) = d.getLifeTotal(me) to d.getLifeTotal(opp)

        d.cast(me, eachPlayerDraws.name)
        withClue("my draw happens; the opponent's is replaced") {
            d.getHandSize(me) shouldBe myHand + 1
            d.getHandSize(opp) shouldBe oppHand
        }
        withClue("the replacement is the caster's: the caster gains the life") {
            d.getLifeTotal(me) shouldBe myLife + 3
            d.getLifeTotal(opp) shouldBe oppLife
        }

        d.cast(me, eachPlayerDraws.name)
        withClue("the shield was spent: the opponent's second draw happens") {
            d.getHandSize(opp) shouldBe oppHand + 1
            d.getLifeTotal(me) shouldBe myLife + 3
        }
    }

    test("the replacement effect names the player whose draw it replaced as TriggeringPlayer") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.cast(me, theyLoseForTheirDraw.name, listOf(opp))
        val (myLife, oppLife) = d.getLifeTotal(me) to d.getLifeTotal(opp)
        val oppHand = d.getHandSize(opp)

        d.cast(me, eachPlayerDraws.name)
        d.getHandSize(opp) shouldBe oppHand
        d.getLifeTotal(opp) shouldBe oppLife - 2
        d.getLifeTotal(me) shouldBe myLife
    }

    test("aimed at the caster, it replaces the caster's own draw as before") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.cast(me, theyLoseForTheirDraw.name, listOf(me))
        val (myHand, oppHand) = d.getHandSize(me) to d.getHandSize(opp)
        val myLife = d.getLifeTotal(me)

        d.cast(me, eachPlayerDraws.name)
        d.getHandSize(me) shouldBe myHand
        d.getHandSize(opp) shouldBe oppHand + 1
        d.getLifeTotal(me) shouldBe myLife - 2
    }

    test("an unused shield ends with the turn: the opponent's next-turn draw step draw is unreplaced") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.cast(me, youGainForTheirDraw.name, listOf(opp))
        val oppHand = d.getHandSize(opp)
        val myLife = d.getLifeTotal(me)

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent drew for their turn and nobody gained life") {
            d.getHandSize(opp) shouldBe oppHand + 1
            d.getLifeTotal(me) shouldBe myLife
        }
    }
})
