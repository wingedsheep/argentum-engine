package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.untapLimitChoices
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.EmblemStaticAbilityComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * [UntapLimitPerStep]'s `players` axis and its emblem-held form: a cap binds only the untap steps
 * of the players it names, relative to its source's controller, and functions from the command
 * zone on an emblem (CR 114.4) — Dovin Baan's "Your opponents can't untap more than two permanents
 * during their untap steps."
 */
class UntapLimitPlayerScopeTest : FunSpec({
    val opponentsCap = card("Opponents Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Land, 1, players = Player.EachOpponent) }
    }
    val ownCap = card("Own Player Cap") {
        manaCost = "{1}"; typeLine = "Artifact"
        staticAbility { ability = UntapLimitPerStep(GameObjectFilter.Land, 1, players = Player.You) }
    }
    val emblemCap = card("Emblem Cap") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell {
            effect = Effects.CreatePermanentEmblem(
                ownedStaticAbilities = listOf(UntapLimitPerStep(GameObjectFilter.Permanent, 2, players = Player.EachOpponent)),
                emblemDescription = "Your opponents can't untap more than two permanents during their untap steps."
            )
        }
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(opponentsCap, ownCap, emblemCap))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun tappedLands(d: GameTestDriver, player: EntityId, n: Int = 3) =
        List(n) { d.putPermanentOnBattlefield(player, "Forest").also(d::tapPermanent) }
    fun limits(d: GameTestDriver, ids: List<EntityId>) =
        untapLimitChoices(d.state, d.cardRegistry, d.services.predicateEvaluator, ids)
    fun castEmblem(d: GameTestDriver, player: EntityId) {
        val spell = d.putCardInHand(player, emblemCap.name)
        d.castSpell(player, spell).error shouldBe null
        d.bothPass()
        d.state.entities.values.count { it.has<EmblemStaticAbilityComponent>() } shouldBe 1
    }

    test("an opponents-only cap leaves its controller's untap uncapped") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mine = tappedLands(d, me); val theirs = tappedLands(d, opp)
        d.putPermanentOnBattlefield(me, opponentsCap.name)
        limits(d, mine).shouldBeEmpty()
        val choice = limits(d, theirs).single()
        choice.matchingPermanents.toSet() shouldBe theirs.toSet()
        choice.max shouldBe 1
    }

    test("the scope follows the source's controller, not its owner") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mine = tappedLands(d, me); val theirs = tappedLands(d, opp)
        d.putPermanentOnBattlefield(opp, opponentsCap.name)
        limits(d, theirs).shouldBeEmpty()
        limits(d, mine).single().matchingPermanents.toSet() shouldBe mine.toSet()
    }

    test("a you-scoped cap binds only its controller") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mine = tappedLands(d, me); val theirs = tappedLands(d, opp)
        d.putPermanentOnBattlefield(me, ownCap.name)
        limits(d, theirs).shouldBeEmpty()
        limits(d, mine).single().max shouldBe 1
    }

    test("a cap held on an emblem functions from the command zone") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        castEmblem(d, me)
        val mine = tappedLands(d, me); val theirs = tappedLands(d, opp)
        limits(d, mine).shouldBeEmpty()
        limits(d, theirs).single().max shouldBe 2
    }

    test("an emblem cap prompts the opponent at their untap step and untaps exactly two") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        castEmblem(d, me)
        val theirs = tappedLands(d, opp, 4)
        d.passPriorityUntil(Step.UNTAP)
        d.activePlayer shouldBe opp
        val decision = d.pendingDecision as SelectCardsDecision
        decision.playerId shouldBe opp
        decision.minSelections shouldBe 2
        d.submitCardSelection(opp, theirs.take(2))
        theirs.count { d.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe 2
    }

    test("the emblem's owner untaps everything on their own turn") {
        val d = driver(); val me = d.activePlayer!!
        castEmblem(d, me)
        val mine = tappedLands(d, me, 4)
        d.passPriorityUntil(Step.UPKEEP)        // opponent's turn
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)        // back to my turn
        d.activePlayer shouldBe me
        mine.none { d.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
    }
})
