package com.wingedsheep.gameserver.config

import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Guards the regression where an all-reprint set (Eighth Edition) was missing from the selectable
 * set list. `boosterGenerator` filtered out any set with an empty `cards` list, but 8ED authors no
 * own `CardDefinition`s — every card is a reprint `Printing` whose canonical lives in an earlier
 * set — so it was silently dropped. The pool is now resolved from those printings.
 */
class GameBeansConfigBoosterPoolTest : FunSpec({

    val config = GameBeansConfig(GameProperties())
    val cardRegistry = config.cardRegistry()
    val boosterGenerator = config.boosterGenerator(cardRegistry)

    test("Eighth Edition appears as a selectable set") {
        boosterGenerator.availableSets shouldContainKey "8ED"
    }

    test("Eighth Edition's booster pool is resolved from its reprints") {
        val ed8 = boosterGenerator.availableSets["8ED"].shouldNotBeNull()
        // 8ED is an all-reprint set: its ~187 cards are resolved from printings, not own definitions.
        ed8.cards.size shouldBeGreaterThan 150
        ed8.distinctCardCount shouldBeGreaterThan 150
        // Every resolved card is stamped as the 8ED printing.
        ed8.cards.all { it.setCode == "8ED" } shouldBe true
    }

    test("resolved reprints span multiple rarities so booster slots can be filled") {
        val ed8 = boosterGenerator.availableSets["8ED"].shouldNotBeNull()
        val rarities = ed8.cards.map { it.metadata.rarity }.toSet()
        rarities shouldContain Rarity.COMMON
        rarities shouldContain Rarity.RARE
    }
})
