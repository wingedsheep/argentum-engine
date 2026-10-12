package com.wingedsheep.gameserver.handler

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * The Commander Draft / Sealed builder crowns pool cards from the same two facts the constructed
 * catalog ships: whether a card can lead alone, and its partner abilities (CR 702.124).
 */
class SealedCardInfoCommanderFieldsTest : FunSpec({

    fun legend(name: String, partner: KeywordAbility?) =
        CardDefinition.creature(
            name = name,
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Human")),
            power = 2,
            toughness = 2,
            supertypes = setOf(Supertype.LEGENDARY),
        ).copy(keywordAbilities = listOfNotNull(partner))

    test("a partner legend can lead and lists its partner ability") {
        val info = ConnectionHandler.cardToSealedCardInfo(legend("Test Partner", KeywordAbility.partner()))
        info.canBeCommander shouldBe true
        info.partnerAbilities shouldBe listOf("Partner")
    }

    test("a Background can't lead alone and has no partner ability of its own") {
        val background = CardDefinition.enchantment(
            name = "Test Background",
            manaCost = ManaCost.parse("{1}{R}"),
            subtypes = setOf(Subtype.BACKGROUND),
            supertypes = setOf(Supertype.LEGENDARY),
        )
        val info = ConnectionHandler.cardToSealedCardInfo(background)
        info.canBeCommander shouldBe false
        info.partnerAbilities.shouldBeEmpty()
    }

    test("a plain creature is no commander") {
        val bear = CardDefinition.creature(
            name = "Test Bear",
            manaCost = ManaCost.parse("{1}{G}"),
            subtypes = setOf(Subtype("Bear")),
            power = 2,
            toughness = 2,
        )
        ConnectionHandler.cardToSealedCardInfo(bear).canBeCommander shouldBe false
    }
})
