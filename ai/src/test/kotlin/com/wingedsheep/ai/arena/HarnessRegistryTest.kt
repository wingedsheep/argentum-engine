package com.wingedsheep.ai.arena

import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Guards the arena / game-log harnesses against playing games in which predefined tokens can't be
 * created: a registry missing them makes `CreatePredefinedTokenExecutor` fail without a trace.
 */
class HarnessRegistryTest : FunSpec({

    val set = MtgSetCatalog.requireByCode("POR")
    val registry = harnessRegistry(set)

    test("a harness registry resolves every predefined token") {
        PredefinedTokens.allTokens.map { it.name }.filter { registry.getCard(it) == null }.shouldBeEmpty()
        listOf("Food", "Treasure", "Clue").forEach { registry.getCard(it).shouldNotBeNull() }
    }

    test("set cards and basics still resolve alongside the tokens") {
        registry.getCard(set.cards.first().name).shouldNotBeNull()
        registry.getCard(set.basicLands.first().name).shouldNotBeNull()
    }
})
