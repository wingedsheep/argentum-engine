package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test for the `else -> true` fall-through in
 * [GrantedKeywordResolver.matchesSpellFilter]. A spell filter that uses a predicate the
 * resolver doesn't yet know how to evaluate (e.g. `NameEquals`, `ManaValueAtMost`,
 * `HasKeyword`) used to fall through to `true`, silently granting the keyword to every
 * spell the caster played. The fix is fail-closed: an unhandled predicate means
 * "we can't tell — don't grant".
 *
 * This test pins the conservative behavior so a future refactor can't accidentally
 * regress to the silent-match default.
 */
class GrantedKeywordFailClosedTest : FunSpec({

    // Granter with a spellFilter that the resolver does NOT have explicit handling for.
    // `NameEquals` is a stable choice: it could be added in the future, but right now
    // (and at the time of the regression) it falls into the `else` branch.
    val NameMatchGranter = CardDefinition.creature(
        name = "Name Match Granter",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Elemental")),
        power = 2,
        toughness = 2,
        oracleText = "Spells named \"Foo\" you cast have convoke.",
        script = CardScript(
            staticAbilities = listOf(
                GrantKeywordToOwnSpells(
                    keyword = Keyword.CONVOKE,
                    spellFilter = GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.NameEquals("Foo"))
                    )
                )
            )
        )
    )

    // Two creature spells the caster might cast. Their names are irrelevant to the test
    // outcome — the point is that an unhandled `NameEquals` predicate must not silently
    // approve either of them.
    val AnyCreatureA = CardDefinition.creature(
        name = "Any Creature A",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )
    val AnyCreatureB = CardDefinition.creature(
        name = "Any Creature B",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(NameMatchGranter, AnyCreatureA, AnyCreatureB)
        )
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("unhandled spell-filter predicate fails closed — keyword is not granted") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(caster, "Name Match Granter")

        val resolver = GrantedKeywordResolver(driver.cardRegistry)
        val creatureA = driver.cardRegistry.requireCard("Any Creature A")
        val creatureB = driver.cardRegistry.requireCard("Any Creature B")

        // Both must return false. With the previous `else -> true` fall-through, the
        // resolver would silently grant CONVOKE to every spell, which is the regression
        // we're guarding against.
        resolver.hasKeyword(driver.state, caster, creatureA, Keyword.CONVOKE) shouldBe false
        resolver.hasKeyword(driver.state, caster, creatureB, Keyword.CONVOKE) shouldBe false
    }
})
