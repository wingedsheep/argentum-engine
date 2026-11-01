package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.deck.CommanderSynergy
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe

/**
 * Synergy weighed by how much a payoff gives back ([com.wingedsheep.ai.engine.knowledge.SynergyStrength]):
 * a strong lord pulls harder on its tribe than a weak one, a strong engine is worth more fed than a
 * weak one, and a card that shrinks a creature type counts that type's creatures against the deck.
 */
class SynergyStrengthTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }
    fun real(name: String): CardDefinition = byName[name] ?: error("'$name' is not in the corpus")

    fun lord(name: String, bonusPower: Int, bonusToughness: Int) = card(name) {
        manaCost = "{2}{R}"
        typeLine = "Creature — Goblin"
        power = 2
        toughness = 2
        staticAbility {
            ability = ModifyStats(
                powerBonus = bonusPower,
                toughnessBonus = bonusToughness,
                filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN), excludeSelf = true),
            )
        }
    }

    val weakLord = lord("Test Goblin Captain", 1, 0)
    val strongLord = lord("Test Goblin Warlord", 2, 2)
    val goblinBane = card("Test Goblin Bane") {
        manaCost = "{1}{W}"
        typeLine = "Enchantment"
        staticAbility {
            ability = ModifyStats(
                powerBonus = -1,
                toughnessBonus = -1,
                filter = GroupFilter(GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN)),
            )
        }
    }
    val goblin = real("Raging Goblin")

    test("a lord's strength follows what it gives each body") {
        val weak = SynergyAxes.of(weakLord)
        val strong = SynergyAxes.of(strongLord)
        weak.tribesRewarded shouldContain Subtype.GOBLIN
        strong.tribesRewarded shouldContain Subtype.GOBLIN
        strong.strength shouldBeGreaterThan weak.strength
        // Goblin King's +1/+1 and mountainwalk is the reference payoff.
        SynergyAxes.of(real("Goblin King")).strength shouldBe 1.0
    }

    test("a feeder gains more beside a strong payoff than beside a weak one") {
        val goblinProfile = SynergyAxes.of(goblin)
        val besideWeak = DeckSynergy.feedGain(goblinProfile, listOf(SynergyAxes.of(weakLord)))
        val besideStrong = DeckSynergy.feedGain(goblinProfile, listOf(SynergyAxes.of(strongLord)))
        besideWeak shouldBeGreaterThan 0.0
        besideStrong shouldBeGreaterThan besideWeak
    }

    test("a strong engine is worth more fed than a weak one") {
        val fuel = List(5) { SynergyAxes.of(goblin) }
        DeckSynergy.fedPayoffs(fuel + SynergyAxes.of(strongLord)) shouldBeGreaterThan
            DeckSynergy.fedPayoffs(fuel + SynergyAxes.of(weakLord))
    }

    test("a card that shrinks a creature type doesn't ask for it, and its members count against it") {
        val bane = SynergyAxes.of(goblinBane)
        bane.tribesRewarded shouldNotContain Subtype.GOBLIN
        bane.punishes shouldContainKey Subtype.GOBLIN

        DeckSynergy.feedGain(SynergyAxes.of(goblin), listOf(bane)) shouldBeLessThan 0.0
        val goblins = List(5) { SynergyAxes.of(goblin) }
        DeckSynergy.fedPayoffs(goblins + bane) shouldBeLessThan 0.0
        DeckSynergy.shortfall(goblins + bane) shouldBeGreaterThan DeckSynergy.shortfall(goblins)
    }

    test("a commander that shrinks a tribe scores its members below zero") {
        val synergy = CommanderSynergy(goblinBane)
        synergy.namedTribes shouldNotContain Subtype.GOBLIN
        synergy.score(goblin) shouldBeLessThan 0.0
        // A strong lord commander pulls its tribe harder than a weak one.
        CommanderSynergy(strongLord).score(goblin) shouldBeGreaterThan CommanderSynergy(weakLord).score(goblin)
    }
})
