package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The "can't attack or block" band — one sentence, two statics.
 *
 * What is worth asserting is the shape a joint sentence can get wrong: that it lands on *both*
 * halves over the *same* group, in printed order; that a card carrying only one half, the halves
 * reversed, or the halves over different groups refuses to print; and that the "unless" form shares
 * one condition between its halves and is source-only.
 */
class CantAttackOrBlockTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun statics(line: String) = fragment(line).script.staticAbilities

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    fun prints(vararg abilities: com.wingedsheep.sdk.scripting.StaticAbility): String? =
        Grammar.abilityLine.printLine(CardFragment(script = CardScript(staticAbilities = abilities.toList())))

    val attached = GroupFilter.attachedCreature()

    "each subject lands both halves on the group it denotes" {
        statics("Enchanted creature can't attack or block.") shouldBe
            listOf(CantAttack(attached), CantBlock(attached))
        statics("~ can't attack or block.") shouldBe
            listOf(CantAttack(GroupFilter.source()), CantBlock(GroupFilter.source()))
        val walls = GroupFilter(GameObjectFilter.Creature.withSubtype("Wall"))
        statics("Wall creatures can't attack or block.") shouldBe listOf(CantAttack(walls), CantBlock(walls))
        roundTrips("Enchanted creature can't attack or block.")
        roundTrips("~ can't attack or block.")
        roundTrips("Wall creatures can't attack or block.")
    }

    // The pair is one sentence only when it is exactly this pair: a lone half is the single-ability
    // rule's business, and a reversed or split pair is some other card's model.
    "a pair the sentence does not say refuses to print" {
        prints(CantBlock(attached), CantAttack(attached)) shouldBe null
        prints(CantAttack(attached), CantBlock(GroupFilter.source())) shouldBe null
        prints(CantAttack(attached.copy(excludeSelf = true)), CantBlock(attached.copy(excludeSelf = true))) shouldBe null
    }

    "the unless form shares one condition between its halves" {
        val abilities = statics("~ can't attack or block unless you control an artifact.")
        val attack = abilities[0].shouldBeInstanceOf<CantAttackUnless>()
        abilities shouldBe listOf(attack, CantBlockUnless(attack.condition))
        attack.filter shouldBe GroupFilter.source()
        roundTrips("~ can't attack or block unless you control an artifact.")
        prints(CantAttackUnless(attack.condition), CantBlockUnless(attack.condition, attached)) shouldBe null
    }

    // The condition's "it" is the source; under "enchanted creature" it would be the host, which the
    // condition vocabulary cannot say — so the attached unless-form is not a rule at all.
    "the unless form is source-only" {
        declines("Enchanted creature can't attack or block unless you control an artifact.")
    }
})
