package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.TrickstersTalisman
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Trickster's Talisman (AFR #79) — Equipped creature gets +1/+1 and has "Whenever this creature
 * deals combat damage to a player, you may sacrifice Trickster's Talisman. If you do, create a
 * token that's a copy of this creature."
 *
 * Focus: the granted trigger lives on the creature, so the sacrifice is scoped to the *granting*
 * Equipment (a second Talisman is never offered), the payoff is gated on the sacrifice actually
 * happening, and the token copies the creature — not the Equipment, and without the +1/+1.
 */
class TrickstersTalismanScenarioTest : FunSpec({

    val equipAbilityId = TrickstersTalisman.activatedAbilities.single { it.isEquipAbility }.id

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20, skipMulligans = true)
    }

    data class Board(
        val d: GameTestDriver,
        val courser: EntityId,
        val talisman: EntityId,
        val otherTalisman: EntityId,
    )

    fun equippedAttack(): Board {
        val d = setup()
        val p1 = d.activePlayer!!
        val opponent = d.getOpponent(p1)

        val courser = d.putCreatureOnBattlefield(p1, "Centaur Courser") // 3/3
        d.removeSummoningSickness(courser)
        val talisman = d.putPermanentOnBattlefield(p1, "Trickster's Talisman")
        // An unattached second Talisman that must never be a legal sacrifice.
        val otherTalisman = d.putPermanentOnBattlefield(p1, "Trickster's Talisman")

        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveColorlessMana(p1, 2)
        d.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = talisman,
                abilityId = equipAbilityId,
                targets = listOf(ChosenTarget.Permanent(courser))
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()
        d.state.getEntity(talisman)?.get<AttachedToComponent>()?.targetId shouldBe courser
        d.state.projectedState.getPower(courser) shouldBe 4
        d.state.projectedState.getToughness(courser) shouldBe 4

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(courser), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareNoBlockers(opponent)
        return Board(d, courser, talisman, otherTalisman)
    }

    /** Drives the trigger by decision shape; returns whether the other Talisman was ever offered. */
    fun Board.resolveTrigger(accept: Boolean): Boolean {
        val p1 = d.activePlayer!!
        var otherOffered = false
        var sawPrompt = false
        var guard = 0
        while (guard++ < 30) {
            while (d.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass()
            when (val decision = d.pendingDecision) {
                is YesNoDecision -> {
                    sawPrompt = true
                    d.submitYesNo(p1, accept).error shouldBe null
                }
                is ChooseTargetsDecision -> {
                    if (otherTalisman in decision.legalTargets[0].orEmpty()) otherOffered = true
                    d.submitTargetSelection(p1, listOf(talisman)).error shouldBe null
                }
                is SelectCardsDecision -> {
                    if (otherTalisman in decision.options) otherOffered = true
                    d.submitCardSelection(p1, listOf(talisman))
                }
                null -> if (sawPrompt) break else d.bothPass()
                else -> break
            }
        }
        sawPrompt shouldBe true
        return otherOffered
    }

    fun Board.coursers(): List<EntityId> =
        d.getCreatures(d.activePlayer!!).filter {
            d.state.getEntity(it)?.get<CardComponent>()?.name == "Centaur Courser"
        }

    test("combat damage to a player: sacrificing the granting Talisman creates a token copy of the creature") {
        val board = equippedAttack()
        val d = board.d
        val opponent = d.getOpponent(d.activePlayer!!)

        val otherOffered = board.resolveTrigger(accept = true)

        d.getLifeTotal(opponent) shouldBe 16
        otherOffered shouldBe false
        d.getGraveyard(d.activePlayer!!) shouldContain board.talisman
        d.findPermanent(d.activePlayer!!, "Trickster's Talisman") shouldBe board.otherTalisman

        val coursers = board.coursers()
        coursers shouldHaveSize 2
        val token = coursers.single { it != board.courser }
        d.state.getEntity(token)?.get<TokenComponent>() shouldNotBe null
        // A copy of the creature's copiable values — the Talisman's +1/+1 isn't copied.
        d.state.projectedState.getPower(token) shouldBe 3
        d.state.projectedState.getToughness(token) shouldBe 3
    }

    test("declining keeps the Talisman and creates no token") {
        val board = equippedAttack()
        val d = board.d

        board.resolveTrigger(accept = false)

        d.state.getEntity(board.talisman)?.get<AttachedToComponent>()?.targetId shouldBe board.courser
        board.coursers() shouldHaveSize 1
    }
})
