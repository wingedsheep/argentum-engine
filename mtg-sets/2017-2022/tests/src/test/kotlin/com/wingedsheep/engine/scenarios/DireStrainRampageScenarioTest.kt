package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.DireStrainRampage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Dire-Strain Rampage (MID #219) — {1}{R}{G} Sorcery.
 *
 *   "Destroy target artifact, enchantment, or land. If a land was destroyed this way, its controller
 *    may search their library for up to two basic land cards, put them onto the battlefield tapped,
 *    then shuffle. Otherwise, its controller may search their library for a basic land card, put it
 *    onto the battlefield tapped, then shuffle."
 *
 * The search size depends on whether the destroyed permanent was a land, and the search belongs to
 * the destroyed permanent's controller, not the caster.
 */
class DireStrainRampageScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + DireStrainRampage)
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun plainsControlledBy(d: GameTestDriver, playerId: EntityId): List<EntityId> =
        d.state.getBattlefield().filter { id ->
            d.state.getEntity(id)?.get<CardComponent>()?.name == "Plains" &&
                d.state.projectedState.getController(id) == playerId
        }

    /** Resolves the stack, accepting every may and taking as many cards as offered; returns who was asked. */
    fun resolve(d: GameTestDriver): Set<EntityId> {
        val asked = mutableSetOf<EntityId>()
        var safety = 0
        while (safety++ < 40) {
            when (val pending = d.state.pendingDecision) {
                is YesNoDecision -> {
                    asked += pending.playerId
                    d.submitYesNo(pending.playerId, true)
                }
                is SelectCardsDecision -> {
                    asked += pending.playerId
                    d.submitCardSelection(pending.playerId, pending.options.take(pending.maxSelections))
                }
                else -> if (d.state.stack.isNotEmpty()) d.bothPass() else break
            }
        }
        return asked
    }

    fun cast(d: GameTestDriver, me: EntityId, target: EntityId) {
        val rampage = d.putCardInHand(me, "Dire-Strain Rampage")
        d.giveMana(me, Color.RED, 1)
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, rampage, targets = listOf(target)).error shouldBe null
    }

    test("destroying a land lets its controller fetch two tapped basic lands") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val land = d.putLandOnBattlefield(opp, "Plains")

        cast(d, me, land)
        val asked = resolve(d)

        asked shouldBe setOf(opp)
        d.state.getZone(opp, Zone.GRAVEYARD) shouldContain land
        val fetched = plainsControlledBy(d, opp)
        fetched.size shouldBe 2
        fetched.all { d.isTapped(it) } shouldBe true
        plainsControlledBy(d, me).size shouldBe 0
    }

    test("destroying an artifact lets its controller fetch only one tapped basic land") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val artifact = d.putCreatureOnBattlefield(opp, "Artifact Creature")

        cast(d, me, artifact)
        val asked = resolve(d)

        asked shouldBe setOf(opp)
        d.state.getZone(opp, Zone.GRAVEYARD) shouldContain artifact
        val fetched = plainsControlledBy(d, opp)
        fetched.size shouldBe 1
        d.isTapped(fetched.single()) shouldBe true
    }
})
