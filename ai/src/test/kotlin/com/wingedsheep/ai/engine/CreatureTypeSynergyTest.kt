package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.DraftsimSetTables
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.draftsim.withSynergyAxes
import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Creature-type synergy: a card that asks for a creature type (a lord, a tribal trigger) is a
 * payoff, read structurally from the filters in its abilities, and the creatures of that type —
 * changelings and token makers included — are its fuel in drafting and deck building.
 */
class CreatureTypeSynergyTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }
    fun card(name: String): CardDefinition = byName[name] ?: error("'$name' is not in the corpus")
    fun read(name: String) = SynergyAxes.of(card(name))
    val goblin = Subtype("Goblin")

    test("a lord asks for its type; members, token makers and changelings feed it") {
        // "Other Goblins get +1/+1 and have mountainwalk": a static filter naming Goblin.
        read("Goblin King").tribesRewarded shouldContain goblin
        read("Goblin King").tribes shouldContain goblin
        // "Whenever another Elf enters the battlefield": a trigger filter naming Elf.
        read("Elvish Vanguard").tribesRewarded shouldContain Subtype("Elf")
        // A plain Goblin is one, and asks for nothing.
        read("Goblin Piker").tribes shouldContain goblin
        read("Goblin Piker").tribesRewarded.shouldBeEmpty()
        // Two 1/1 Goblin tokens.
        read("Dragon Fodder").tribes shouldContain goblin
        read("Changeling Outcast").everyTribe shouldBe true
        read("Lightning Bolt").tribesRewarded.shouldBeEmpty()
        read("Ironclaw Orcs").tribes shouldContain Subtype("Orc")
    }

    test("a lord is fed by the members beside it, never by itself") {
        fun fed(vararg names: String) = DeckSynergy.fedPayoffs(names.map(::read))
        fed("Goblin King") shouldBe 0.0
        fed("Goblin King", "Goblin Piker", "Goblin Piker") shouldBe (2 / DeckSynergy.TRIBE_MEMBERS_WANTED).plusOrMinus(1e-9)
        fed("Goblin King", "Goblin Piker", "Dragon Fodder", "Changeling Outcast") shouldBe
            (3 / DeckSynergy.TRIBE_MEMBERS_WANTED).plusOrMinus(1e-9)
        fed("Goblin King", "Ironclaw Orcs", "Ironclaw Orcs") shouldBe 0.0
        fed("Goblin King", *Array(5) { "Goblin Piker" }) shouldBe 1.0
    }

    test("beside a lord, the pick scorer takes its type over an equal stranger") {
        val names = listOf("Goblin King", "Goblin Piker", "Ironclaw Orcs")
        val defs = names.map(::card)
        val plain = DraftsimSetTables(defs.associate { DraftsimData.nameKey(it.name) to 2.5 }, emptySet(), emptyMap())
        val pack = listOf(card("Goblin Piker"), card("Ironclaw Orcs")).map { it.toScorerCard() }
        val picks = listOf(card("Goblin King").toScorerCard())
        fun gap(tables: DraftsimSetTables): Double {
            val scores = DraftsimScorer(tables).scoreBoosterAuto(pack, picks)
            return scores.getValue("Goblin Piker").total - scores.getValue("Ironclaw Orcs").total
        }
        val without = gap(plain)
        gap(plain.withSynergyAxes(defs)) shouldBeGreaterThan without
    }

    test("the swap pass trades an equal stranger for a member of the lord's type") {
        var index = 0
        fun slot(name: String) = PoolSlot(index++, card(name))
        val selected = listOf(slot("Goblin King")) + List(4) { slot("Goblin Piker") } + List(2) { slot("Ironclaw Orcs") }
        val bench = listOf(slot("Goblin Piker"))
        val swap = bestSynergySwap(selected, bench, keepPayoffs = true) { 2.5 }
        swap?.first?.card?.name shouldBe "Ironclaw Orcs"
        swap?.second?.card?.name shouldBe "Goblin Piker"
    }
    test("a count of the defending player's permanents of a type is a tribe it fights, not one it asks for") {
        // Siege Dragon: "if defending player controls no Walls" and "destroy all Walls your
        // opponents control" both name the opponent's Walls.
        read("Siege Dragon").tribesRewarded.shouldBeEmpty()
    }
})
