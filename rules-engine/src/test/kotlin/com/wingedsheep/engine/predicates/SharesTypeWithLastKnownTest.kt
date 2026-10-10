package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * "Shares a card type / creature type with it", where *it* has left the battlefield, reads *it* as it
 * last existed there (CR 608.2h) — the snapshot its sacrifice captured — not the card's printed type
 * line in the graveyard. A reference still on the battlefield is read live.
 */
class SharesTypeWithLastKnownTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.matches(entity: EntityId, filter: GameObjectFilter, sacrificed: EntitySnapshot) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, filter,
            PredicateContext.fromEffectContext(
                EffectContext(sourceId = null, controllerId = activePlayer!!, sacrificedPermanents = listOf(sacrificed))
            )
        )

    val sharesCardType = GameObjectFilter.Any.sharingCardTypeWith(EffectTarget.SacrificedAsCost())
    val sharesCreatureType = GameObjectFilter.Any.sharingCreatureTypeWith(EffectTarget.SacrificedAsCost())

    test("a sacrifice snapshot freezes the projected type line — an animated land is an artifact creature land") {
        val d = driver(); val me = d.activePlayer!!
        val factory = d.putLandOnBattlefield(me, "Mishra's Factory")
        d.giveColorlessMana(me, 1)
        d.submit(ActivateAbility(me, factory, TestCards.all.first { it.name == "Mishra's Factory" }.activatedAbilities[1].id))
        d.bothPass()

        val snapshot = captureEntitySnapshots(listOf(factory), d.state.projectedState, d.state).single()
        snapshot.typeLine!!.cardTypes shouldContainExactlyInAnyOrder
            listOf(CardType.ARTIFACT, CardType.CREATURE, CardType.LAND)
    }

    test("off the battlefield the snapshot's card types win over the printed ones") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        // A creature card in the graveyard that was, as it last existed, an artifact (only).
        val gone = d.putCardInGraveyard(me, "Grizzly Bears")
        val lastKnown = EntitySnapshot(entityId = gone, typeLine = TypeLine.parse("Artifact"))
        val artifact = d.putPermanentOnBattlefield(opp, "Ornithopter")
        val creature = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.matches(artifact, sharesCardType, lastKnown) shouldBe true
        d.matches(creature, sharesCardType, lastKnown) shouldBe false
    }

    test("off the battlefield the snapshot's creature types win over the printed ones") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val gone = d.putCardInGraveyard(me, "Grizzly Bears") // printed Bear
        val lastKnown = EntitySnapshot(entityId = gone, subtypes = setOf("Goblin"), typeLine = TypeLine.parse("Creature — Goblin"))
        val goblin = d.putCreatureOnBattlefield(opp, "Goblin Guide")
        val bear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.matches(goblin, sharesCreatureType, lastKnown) shouldBe true
        d.matches(bear, sharesCreatureType, lastKnown) shouldBe false
    }

    test("a reference still on the battlefield is read live, not from a stale snapshot") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val stillHere = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val stale = EntitySnapshot(entityId = stillHere, typeLine = TypeLine.parse("Artifact"))
        val creature = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.matches(creature, sharesCardType, stale) shouldBe true
    }
})
