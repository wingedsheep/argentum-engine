package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe

/**
 * [EntersWithCharacteristics]: a card an effect puts onto the battlefield already has the added
 * characteristics as it enters — "those permanents are 4/4 creatures in addition to their other
 * types" (Dance of the Manse), "they're 5/5 Elemental creatures" (Relive the Past). Per the cards'
 * rulings they "don't enter as noncreature permanents and then become creatures", so entry
 * replacements and "whenever a creature enters" triggers see a creature, and a later effect that
 * sets P/T still wins by timestamp.
 */
class EntersWithCharacteristicsTest : FunSpec({
    val elemental = EntersWithCharacteristics.creature(5, 5, "Elemental")

    val relic = card("Test Entry Relic") {
        manaCost = "{3}"
        typeLine = "Artifact"
    }
    val idol = card("Test Entry Idol") {
        manaCost = "{2}"
        typeLine = "Enchantment"
    }
    val reanimate = card("Test Entry Reanimate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefieldFromGraveyard(t, entersWith = elemental)
        }
    }
    val massReturn = card("Test Entry Mass Return") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Pipeline {
                move(
                    gather(CardSource.FromZone(Zone.GRAVEYARD, filter = GameObjectFilter.Artifact or GameObjectFilter.Enchantment)),
                    CardDestination.ToZone(Zone.BATTLEFIELD),
                    entersWith = elemental,
                )
            }
        }
    }
    val watcher = card("Test Entry Watcher") {
        manaCost = "{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature).enters()
            effect = Effects.GainLife(1)
        }
    }
    val creaturesEnterTapped = card("Test Entry Tapper") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        replacementEffect(
            PermanentsEnterTapped(
                appliesTo = EventPattern.ZoneChangeEvent(filter = GameObjectFilter.Creature, to = Zone.BATTLEFIELD)
            )
        )
    }
    val shrink = card("Test Entry Shrink") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.BecomeCreature(t, power = 1, toughness = 1, duration = Duration.EndOfTurn)
        }
    }

    val priest = card("Test Entry Priest") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Cleric"
        power = 2
        toughness = 2
        replacementEffect(
            RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature.nontoken().notCast(),
                    to = Zone.BATTLEFIELD,
                ),
            )
        )
    }
    val plainReanimate = card("Test Entry Plain Reanimate") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
            effect = Effects.PutOntoBattlefieldFromGraveyard(t)
        }
    }
    val shatter = card("Test Entry Shatter") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.Destroy(t)
        }
    }
    val pump = card("Test Entry Pump") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val t = target(TargetFilter.Permanent)
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(relic, idol, reanimate, massReturn, watcher, creaturesEnterTapped, shrink, plainReanimate, shatter, pump, priest))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.cast(name: String, targets: List<EntityId> = emptyList()) {
        val p = activePlayer!!
        castSpellWithTargets(p, putCardInHand(p, name), targets.map { ChosenTarget.Card(it, p, Zone.GRAVEYARD) })
            .error shouldBe null
        repeat(6) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    fun GameTestDriver.castAtPermanent(name: String, target: EntityId) {
        val p = activePlayer!!
        castSpellWithTargets(p, putCardInHand(p, name), listOf(ChosenTarget.Permanent(target))).error shouldBe null
        repeat(6) { if (state.pendingDecision == null && state.stack.isNotEmpty()) bothPass() }
    }

    test("a returned artifact is a 5/5 Elemental artifact creature, and creature-enters observers see it") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, watcher.name)
        val relicId = d.putCardInGraveyard(p, relic.name)
        val life = d.getLifeTotal(p)

        d.cast(reanimate.name, listOf(relicId))

        val projected = d.state.projectedState
        (relicId in d.state.getBattlefield()) shouldBe true
        projected.isCreature(relicId) shouldBe true
        projected.getTypes(relicId) shouldContainAll setOf("ARTIFACT", "CREATURE")
        projected.getSubtypes(relicId) shouldContain "Elemental"
        projected.getPower(relicId) shouldBe 5
        projected.getToughness(relicId) shouldBe 5
        d.getLifeTotal(p) shouldBe life + 1
    }

    test("a 'creatures enter tapped' replacement applies, because it is a creature as it enters") {
        val d = driver()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, creaturesEnterTapped.name)
        val relicId = d.putCardInGraveyard(p, relic.name)

        d.cast(reanimate.name, listOf(relicId))

        d.isTapped(relicId) shouldBe true
    }

    test("a collection enters together as creatures; counters and a later P/T-setting effect apply on top") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, watcher.name)
        val relicId = d.putCardInGraveyard(p, relic.name)
        val idolId = d.putCardInGraveyard(p, idol.name)
        val life = d.getLifeTotal(p)

        d.cast(massReturn.name)

        val projected = d.state.projectedState
        listOf(relicId, idolId).forEach {
            projected.isCreature(it) shouldBe true
            projected.getPower(it) shouldBe 5
        }
        projected.getTypes(idolId) shouldContain "ENCHANTMENT"
        d.getLifeTotal(p) shouldBe life + 2

        d.castAtPermanent(pump.name, relicId)
        d.state.projectedState.getPower(relicId) shouldBe 6

        // A later effect that sets base P/T as it makes the permanent a creature wins by timestamp.
        d.castAtPermanent(shrink.name, idolId)
        d.state.projectedState.getPower(idolId) shouldBe 1
    }

    test("the characteristics belong to that object only — returned again without the rider, it is no creature") {
        val d = driver()
        val p = d.activePlayer!!
        val relicId = d.putCardInGraveyard(p, relic.name)
        d.cast(reanimate.name, listOf(relicId))
        d.state.projectedState.isCreature(relicId) shouldBe true

        d.castAtPermanent(shatter.name, relicId)
        d.getGraveyard(p) shouldContain relicId
        d.cast(plainReanimate.name, listOf(relicId))
        (relicId in d.state.getBattlefield()) shouldBe true
        d.state.projectedState.isCreature(relicId) shouldBe false
    }

    test("a 'nontoken creature would enter' redirect judges it as the creature it enters as (Containment Priest)") {
        val d = driver()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, priest.name)
        val relicId = d.putCardInGraveyard(p, relic.name)
        val idolId = d.putCardInGraveyard(p, idol.name)

        d.cast(reanimate.name, listOf(relicId))
        d.getExile(p) shouldContain relicId

        d.cast(massReturn.name)
        d.getExile(p) shouldContain idolId
        (idolId in d.state.getBattlefield()) shouldBe false
    }
})
