package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Assimilate (Star Trek): "Put it onto the battlefield under your control with a +1/+1 counter. It's
 * a Borg artifact creature and loses all other creature types." — `Effects.Assimilate`, i.e. an
 * entry under your control with a +1/+1 counter and [EntersWithCharacteristics.assimilated], whose
 * `creatureSubtypes` axis *replaces* the creature types rather than adding to them.
 */
class AssimilateTest : FunSpec({
    val goblin = card("Test Assimilate Goblin") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin Warrior"
        power = 2
        toughness = 2
    }
    val shapeshifter = card("Test Assimilate Shapeshifter") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Shapeshifter"
        power = 1
        toughness = 1
        keywords(Keyword.CHANGELING)
    }
    val assimilate = card("Test Assimilate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.Assimilate(t, fromZone = Zone.GRAVEYARD)
        }
    }
    val reanimate = card("Test Assimilate Plain Reanimate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter.CreatureInGraveyard)
            effect = Effects.PutOntoBattlefieldFromGraveyard(t, underYourControl = true)
        }
    }
    val artifactWatcher = card("Test Assimilate Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.ArtifactCreature).enters()
            effect = Effects.GainLife(1)
        }
    }
    val kill = card("Test Assimilate Kill") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Creature)
            effect = Effects.Destroy(t)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(goblin, shapeshifter, assimilate, reanimate, artifactWatcher, kill))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castAtGraveyardCard(name: String, cardId: EntityId, owner: EntityId) {
        val p = activePlayer!!
        castSpellWithTargets(p, putCardInHand(p, name), listOf(ChosenTarget.Card(cardId, owner, Zone.GRAVEYARD)))
            .error shouldBe null
        repeat(6) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    fun GameTestDriver.plusOneCounters(id: EntityId) =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("an opponent's creature card enters under your control as a Borg artifact creature with a +1/+1 counter") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val goblinId = d.putCardInGraveyard(opp, goblin.name)

        d.castAtGraveyardCard(assimilate.name, goblinId, opp)

        (goblinId in d.state.getBattlefield()) shouldBe true
        val projected = d.state.projectedState
        projected.getController(goblinId) shouldBe me
        projected.getTypes(goblinId) shouldContainAll setOf("ARTIFACT", "CREATURE")
        projected.getSubtypes(goblinId) shouldContain "Borg"
        projected.getSubtypes(goblinId) shouldNotContain "Goblin"
        projected.getSubtypes(goblinId) shouldNotContain "Warrior"
        d.plusOneCounters(goblinId) shouldBe 1
        projected.getPower(goblinId) shouldBe 3
        projected.getToughness(goblinId) shouldBe 3
    }

    test("it is an artifact creature as it enters, so 'whenever an artifact creature enters' sees it") {
        val d = driver()
        val me = d.activePlayer!!
        d.putCreatureOnBattlefield(me, artifactWatcher.name)
        val goblinId = d.putCardInGraveyard(me, goblin.name)
        val life = d.getLifeTotal(me)

        d.castAtGraveyardCard(assimilate.name, goblinId, me)

        d.getLifeTotal(me) shouldBe life + 1
    }

    test("a changeling loses every creature type but Borg") {
        val d = driver()
        val me = d.activePlayer!!
        val shifterId = d.putCardInGraveyard(me, shapeshifter.name)

        d.castAtGraveyardCard(assimilate.name, shifterId, me)

        val subtypes = d.state.projectedState.getSubtypes(shifterId)
        subtypes shouldContain "Borg"
        subtypes shouldNotContain "Shapeshifter"
        subtypes shouldNotContain "Elf"
    }

    test("the rider belongs to that object — back from the graveyard without it, it is a Goblin again") {
        val d = driver()
        val me = d.activePlayer!!
        val goblinId = d.putCardInGraveyard(me, goblin.name)
        d.castAtGraveyardCard(assimilate.name, goblinId, me)

        val p = d.activePlayer!!
        d.castSpellWithTargets(p, d.putCardInHand(p, kill.name), listOf(ChosenTarget.Permanent(goblinId))).error shouldBe null
        repeat(6) { if (d.state.pendingDecision == null && d.state.stack.isNotEmpty()) d.bothPass() }
        d.getGraveyard(me) shouldContain goblinId

        d.castAtGraveyardCard(reanimate.name, goblinId, me)
        val projected = d.state.projectedState
        projected.getSubtypes(goblinId) shouldContain "Goblin"
        projected.getSubtypes(goblinId) shouldNotContain "Borg"
        projected.getTypes(goblinId) shouldNotContain "ARTIFACT"
        d.plusOneCounters(goblinId) shouldBe 0
    }
})
