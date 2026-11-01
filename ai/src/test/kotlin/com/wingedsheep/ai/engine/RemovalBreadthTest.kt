package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.deck.CommanderDeckRoles
import com.wingedsheep.ai.engine.deck.DeckRole
import com.wingedsheep.ai.engine.knowledge.RemovalBreadth
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * Narrow hosers ([RemovalBreadth]): removal that can only aim at one colour or a land answers too
 * little of an unknown opponent's deck to rate as removal or fill an interaction slot.
 */
class RemovalBreadthTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }
    fun card(name: String): CardDefinition = byName[name] ?: error("'$name' is not in the corpus")

    test("colour hosers and land destruction are narrow; real removal is not") {
        RemovalBreadth.isNarrow(card("Execute")) shouldBe true
        RemovalBreadth.isNarrow(card("Slay")) shouldBe true
        RemovalBreadth.isNarrow(card("Choking Sands")) shouldBe true
        RemovalBreadth.isNarrow(card("Murder")) shouldBe false
        RemovalBreadth.isNarrow(card("Doom Blade")) shouldBe false
        // Artifact and enchantment removal answers the noncreature threats.
        RemovalBreadth.isNarrow(card("Naturalize")) shouldBe false
        // Not removal at all.
        RemovalBreadth.of(card("Raging Goblin")) shouldBe 1.0
    }

    test("a hoser fills no interaction slot") {
        CommanderDeckRoles.rolesOf(card("Execute")) shouldNotContain DeckRole.REMOVAL
        CommanderDeckRoles.rolesOf(card("Choking Sands")) shouldNotContain DeckRole.REMOVAL
        CommanderDeckRoles.rolesOf(card("Murder")) shouldContain DeckRole.REMOVAL
    }

    test("the effect heuristic rates a hoser below the same spell aimed at any creature") {
        // Execute is Murder's shape with a colour restriction and a cantrip on top; neither has a
        // limited rating to lean on in the heuristic, so compare the heuristic's own breakdowns.
        val execute = LimitedCardRater.explain(card("Execute")).heuristicRating
        val murder = LimitedCardRater.explain(card("Murder")).heuristicRating
        execute shouldBeLessThan murder
    }
})
