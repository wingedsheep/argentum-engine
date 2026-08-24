package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.CantBeBlockedByFewerThan
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedExceptByEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The combat-restriction band — "can't be blocked", in both slots the SDK gives it.
 *
 * The family is a **product**: a subject (the source, the attached permanent, a plural noun phrase)
 * crossed with a restriction (bare, by a class of blocker, except by one, by more than N, except by
 * N or more, can't block, can block only). What is worth asserting is not that each sentence parses
 * but the things a product can get wrong:
 *
 * - each subject lands on the `GroupFilter` it denotes, and **only** that one, so no sentence prints
 *   a lord's line as an aura's or the source's;
 * - a subject that the printed phrase does not fully describe — an `excludeSelf`, a soulbond pair —
 *   **refuses to print** rather than dropping the field;
 * - the source's bare "can't be blocked" stays the `AbilityFlag`, which is the one hole in the
 *   product and the one that would otherwise be ambiguous;
 * - the durational table is the same five sentences one slot over, and its plural rows exist.
 */
class CombatRestrictionsTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun statics(line: String) = fragment(line).script.staticAbilities

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    /**
     * An alternate spelling: it parses to the same model and prints as [canonical].
     *
     * Every bare tribal noun in this file is one. "Walls" reads as `Permanent.withSubtype(Wall)` —
     * a bare tribal noun means *permanents*, the migration `Filters` records — and the canonical
     * spelling of that value is "Wall permanents", so the printed forms move even though the reading
     * is exactly right.
     */
    fun variantOf(line: String, canonical: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe canonical
        fragment(line) shouldBe fragment(canonical)
    }

    val walls = GameObjectFilter.Permanent.withSubtype(Subtype.WALL)
    val flyers = GameObjectFilter.Creature.withKeyword(Keyword.FLYING)

    // ---------------------------------------------------------------------------------------
    // The subject axis — three spellings, three disjoint GroupFilters
    // ---------------------------------------------------------------------------------------

    "each subject lands on the group filter it denotes" {
        statics("~ can't be blocked by Walls.") shouldBe
            listOf(CantBeBlockedBy(walls, GroupFilter.source()))
        statics("Enchanted creature can't be blocked by Walls.") shouldBe
            listOf(CantBeBlockedBy(walls, GroupFilter.attachedCreature()))
        statics("Juggernauts you control can't be blocked by Walls.") shouldBe
            listOf(
                CantBeBlockedBy(
                    walls,
                    GroupFilter(GameObjectFilter.Permanent.withSubtype("Juggernaut").youControl()),
                )
            )
        variantOf("~ can't be blocked by Walls.", "~ can't be blocked by Wall permanents.")
        variantOf(
            "Enchanted creature can't be blocked by Walls.",
            "Enchanted creature can't be blocked by Wall permanents.",
        )
        variantOf(
            "Juggernauts you control can't be blocked by Walls.",
            "Juggernaut permanents you control can't be blocked by Wall permanents.",
        )
    }

    // Cloak of Mists and Hot Soup were carrying `flags(AbilityFlag.CANT_BE_BLOCKED)`, which lands the
    // evasion on the Aura or Equipment itself — a permanent that never blocks or is blocked. The
    // attached subject is what the printed line says, and there is no flag that can express it.
    "the attached subject is the only spelling of an aura's evasion" {
        statics("Enchanted creature can't be blocked.") shouldBe
            listOf(CantBeBlocked(GroupFilter.attachedCreature()))
        roundTrips("Enchanted creature can't be blocked.")
        roundTrips("Enchanted creature can't block.")
        roundTrips("Enchanted creature can block only creatures with flying.")
        roundTrips("Enchanted creature can't be blocked by more than one creature.")
    }

    // The one hole in the product. 19 hand-written cards spell the unconditional source form as the
    // flag against 6 that write the static, so a source-scoped bare row here would be a second rule
    // for one text. `Grammar.flagLine` owns it and the fragment holds it outside the script.
    "the source's bare form stays the flag, not a static" {
        fragment("~ can't be blocked.") shouldBe CardFragment(flags = setOf(AbilityFlag.CANT_BE_BLOCKED))
        roundTrips("~ can't be blocked.")
    }

    // The fail-closed half. `GroupFilter` carries four fields the printed noun phrase says nothing
    // about; the subject's round-trip check is what makes every one of them refuse rather than one
    // that somebody remembered to test.
    "a group filter the printed subject cannot describe refuses to print" {
        val other = CardFragment(
            script = CardScript(
                staticAbilities = listOf(
                    CantBeBlockedBy(
                        walls,
                        GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
                    )
                )
            )
        )
        Grammar.abilityLine.printLine(other) shouldBe null
    }

    // ---------------------------------------------------------------------------------------
    // The restriction axis — one clause, one SDK type
    // ---------------------------------------------------------------------------------------

    "each restriction lands on its own SDK type" {
        statics("~ can't be blocked except by creatures with flying.") shouldBe
            listOf(CantBeBlockedExceptBy(flyers, GroupFilter.source()))
        statics("~ can't be blocked except by three or more creatures.") shouldBe
            listOf(CantBeBlockedByFewerThan(3, GroupFilter.source()))
        statics("~ can't be blocked by more than one creature.") shouldBe
            listOf(CantBeBlockedByMoreThan(1, GroupFilter.source()))
        statics("~ can't be blocked by more than two creatures.") shouldBe
            listOf(CantBeBlockedByMoreThan(2, GroupFilter.source()))
        statics("~ can't block.") shouldBe listOf(CantBlock(GroupFilter.source()))
        statics("~ can block only creatures with flying.") shouldBe
            listOf(CanOnlyBlockCreaturesWith(flyers, GroupFilter.source()))
    }

    // Menace *is* `CantBeBlockedByFewerThan(2)` and it is spelled by `Keywords`, so this row starts
    // at three. A rule that also printed two would be a second spelling of the keyword.
    "the generalized menace starts above the keyword it generalizes" {
        roundTrips("~ can't be blocked except by three or more creatures.")
        roundTrips("~ can't be blocked except by six or more creatures.")
        val menace = CardFragment(
            script = CardScript(staticAbilities = listOf(CantBeBlockedByFewerThan(2)))
        )
        Grammar.abilityLine.printLine(menace) shouldBe null
    }

    // The count rows split on the noun's number, because "more than one creature" and "more than two
    // creatures" differ in both halves of the phrase.
    "the counted rows print the number as a word with its noun" {
        variantOf(
            "Boars you control can't be blocked by more than one creature.",
            "Boar permanents you control can't be blocked by more than one creature.",
        )
        roundTrips("Enchanted creature can't be blocked by more than two creatures.")
    }

    // ---------------------------------------------------------------------------------------
    // The durational table — the same sentences one slot over
    // ---------------------------------------------------------------------------------------

    "the durational evasion is the grant family's effect over the flag" {
        fragment("Target creature can't be blocked this turn.").script.spellEffect shouldBe
            GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, Targets.bound())
        roundTrips("Target creature can't be blocked this turn.")
        roundTrips("{1}{U}: ~ can't be blocked this turn.")
        roundTrips("Target creature can't be blocked this turn except by creatures with flying.")
    }

    "the durational restrictions are each their own effect" {
        fragment("Target creature can't block this turn.").script.spellEffect
            .shouldBeInstanceOf<CantBlockEffect>()
        fragment("Target creature can't be blocked this turn except by Walls.").script.spellEffect
            .shouldBeInstanceOf<GrantCantBeBlockedExceptByEffect>()
        roundTrips("Target creature can't block this turn.")
        roundTrips("Target creature can't attack this turn.")
        roundTrips("Target creature can't attack or block this turn.")
    }

    // The frozen quantifier: the singular row was the only one written, and the plural rows print.
    "the durational table takes every quantifier English prints before \"target\"" {
        roundTrips("Up to two target creatures can't block this turn.")
        roundTrips("Up to three target creatures can't block this turn.")
        roundTrips("Up to one target creature can't be blocked this turn.")
        roundTrips("Target creature with power 2 or less can't be blocked this turn.")
    }

    // "That creature" is the target an earlier clause chose; the pronoun is deliberately absent, so
    // a later-clause "it" declines rather than being read as the wrong creature.
    "the anaphor reads the target and the pronoun declines" {
        roundTrips("Untap target creature. That creature can't be blocked this turn.")
        declines("Untap target creature. It can't be blocked this turn.")
    }

    // Every rule in the family can print what it parses — the meta-test each family gets, because a
    // `match` half that quietly matches nothing compiles, parses, and surfaces as a print mismatch
    // far from its cause.
    "every combat-restriction rule prints what it parses" {
        listOf(
            "~ can't be blocked.",
            "~ can't be blocked by Wall permanents.",
            "~ can't be blocked except by creatures with flying.",
            "~ can't be blocked except by three or more creatures.",
            "~ can't be blocked by more than one creature.",
            "~ can't be blocked by more than two creatures.",
            "~ can't block.",
            "~ can block only creatures with flying.",
            "Enchanted creature can't be blocked.",
            "Enchanted creature can't be blocked by Wall permanents.",
            "Enchanted creature can't be blocked except by Wall permanents.",
            "Enchanted creature can't be blocked by more than one creature.",
            "Enchanted creature can't block.",
            "Enchanted creature can block only creatures with flying.",
            "Sliver permanents can't be blocked except by Sliver permanents.",
            "Boar permanents you control can't be blocked by more than one creature.",
            "Creatures with flying can block only creatures with flying.",
            "Target creature can't be blocked this turn.",
            "Target creature can't be blocked this turn except by creatures with flying.",
            "Target creature can't block this turn.",
            "Target creature can't attack this turn.",
            "Target creature can't attack or block this turn.",
            "Up to two target creatures can't block this turn.",
            "{1}{U}: ~ can't be blocked this turn.",
        ).forEach { line -> Grammar.abilityLine.printLine(fragment(line)) shouldBe line }
    }
})
