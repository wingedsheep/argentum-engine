package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * A group move to the battlefield that holds an Aura pauses to ask what the Aura enchants
 * (CR 303.4f) and resumes in `MoveCollectionAuraTargetContinuation`. The move's placement and the
 * rest of the batch must survive that pause:
 *  - "return them to the battlefield tapped" puts the Auras onto the battlefield tapped too;
 *  - the cards that entered before the pause still report their entry, so their ETB triggers fire;
 *  - an Aura with nothing legal to enchant stays in its zone (CR 303.4g) even when an earlier Aura
 *    of the same batch was just attached — it never follows that Aura onto its host.
 */
class TappedGroupMoveAuraTest : FunSpec({
    fun returner(name: String, placement: ZonePlacement) = card(name) {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            targets(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD), count = 3, optional = true)
            effect = Effects.Pipeline {
                val returning = gather(CardSource.ChosenTargets)
                move(returning, CardDestination.ToZone(Zone.BATTLEFIELD, placement = placement))
            }
        }
    }
    val returnTapped = returner("Test Return Tapped", ZonePlacement.Tapped)
    val returnUntapped = returner("Test Return Untapped", ZonePlacement.Default)
    val creatureAura = card("Test Creature Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }
    val artifactAura = card("Test Artifact Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Artifact)
    }
    val lifeOnEntry = card("Test Life On Entry") {
        manaCost = "{W}"
        typeLine = "Creature — Cleric"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(3)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(returnTapped, returnUntapped, creatureAura, artifactAura, lifeOnEntry))
        it.initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Cast [spell] at [cards] in the caster's graveyard; answer each Aura prompt via [host]. */
    fun GameTestDriver.cast(
        spell: String,
        cards: List<EntityId>,
        host: (ChooseTargetsDecision) -> EntityId = { it.legalTargets[0]!!.first() },
        offered: MutableList<List<EntityId>> = mutableListOf(),
    ) {
        val me = player1
        val targets = cards.map { ChosenTarget.Card(it, me, Zone.GRAVEYARD) }
        castSpellWithTargets(me, putCardInHand(me, spell), targets).error shouldBe null
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 20) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> {
                    offered += decision.legalTargets[0]!!
                    submitTargetSelection(decision.playerId, listOf(host(decision))).error shouldBe null
                }
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
    }

    test("a tapped group move puts the Aura onto the battlefield tapped, beside the tapped creature") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val aura = d.putCardInGraveyard(d.player1, "Test Creature Aura")
        val creature = d.putCardInGraveyard(d.player1, "Test Life On Entry")
        val offered = mutableListOf<List<EntityId>>()

        d.cast("Test Return Tapped", listOf(aura, creature), offered = offered)

        withClue("the creature entering with the Aura can't be its host (CR 303.4f)") {
            offered shouldContainExactly listOf(listOf(host))
        }
        d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe host
        withClue("both returned cards entered tapped") {
            d.isTapped(aura) shouldBe true
            d.isTapped(creature) shouldBe true
        }
        withClue("the host itself is untouched") { d.isTapped(host) shouldBe false }
    }

    test("a creature returned beside an Aura still triggers on entering") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val aura = d.putCardInGraveyard(d.player1, "Test Creature Aura")
        val creature = d.putCardInGraveyard(d.player1, "Test Life On Entry")
        val life = d.getLifeTotal(d.player1)

        d.cast("Test Return Tapped", listOf(aura, creature))

        d.state.getBattlefield().contains(creature) shouldBe true
        d.getLifeTotal(d.player1) shouldBe life + 3
    }

    test("an Aura with no legal host stays in the graveyard instead of following the previous Aura") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val creatureAuraId = d.putCardInGraveyard(d.player1, "Test Creature Aura")
        val artifactAuraId = d.putCardInGraveyard(d.player1, "Test Artifact Aura")

        d.cast("Test Return Tapped", listOf(creatureAuraId, artifactAuraId))

        d.state.getEntity(creatureAuraId)?.get<AttachedToComponent>()?.targetId shouldBe host
        withClue("no artifact exists, so the artifact Aura stays put (CR 303.4g)") {
            d.state.getBattlefield().contains(artifactAuraId) shouldBe false
            d.state.getGraveyard(d.player1).contains(artifactAuraId) shouldBe true
        }
    }

    test("each Aura of a batch is asked about in turn and both enter tapped") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val auraA = d.putCardInGraveyard(d.player1, "Test Creature Aura")
        val auraB = d.putCardInGraveyard(d.player1, "Test Creature Aura")
        val offered = mutableListOf<List<EntityId>>()

        d.cast("Test Return Tapped", listOf(auraA, auraB), offered = offered)

        offered.size shouldBe 2
        listOf(auraA, auraB).forEach { aura ->
            d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe bears
            d.isTapped(aura) shouldBe true
        }
    }

    test("an untapped group move still puts its Aura onto the battlefield untapped") {
        val d = driver()
        val host = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val aura = d.putCardInGraveyard(d.player1, "Test Creature Aura")

        d.cast("Test Return Untapped", listOf(aura))

        d.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId shouldBe host
        d.isTapped(aura) shouldBe false
    }
})
