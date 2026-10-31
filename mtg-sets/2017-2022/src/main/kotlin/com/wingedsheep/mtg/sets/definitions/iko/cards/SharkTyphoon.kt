package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

private const val SHARK_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/3/0/30d12226-6417-4b98-beb5-89a36c774141.jpg?1783930944"

/**
 * Shark Typhoon — Ikoria: Lair of Behemoths #67
 * {5}{U} · Enchantment
 *
 * Whenever you cast a noncreature spell, create an X/X blue Shark creature token with flying,
 * where X is that spell's mana value.
 * Cycling {X}{1}{U}
 * When you cycle this card, create an X/X blue Shark creature token with flying.
 *
 * The cast trigger reads [DynamicAmounts.triggeringSpellManaValue], captured on the cast event —
 * so a spell with {X} counts its chosen X, and the Shark still arrives if the spell is countered.
 * Cycling is an activated ability (CR 702.29a), so its X is announced as it's activated; the
 * engine carries that X on the cycle event into the trigger, where [DynamicAmounts.xValue] reads
 * it (Valor's Flagship, Webstrike Elite). The draw belongs to the cycling ability itself.
 */
val SharkTyphoon = card("Shark Typhoon") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a noncreature spell, create an X/X blue Shark creature token " +
        "with flying, where X is that spell's mana value.\n" +
        "Cycling {X}{1}{U} ({X}{1}{U}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, create an X/X blue Shark creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.triggeringSpellManaValue(),
            dynamicToughness = DynamicAmounts.triggeringSpellManaValue(),
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Shark"),
            keywords = setOf(Keyword.FLYING),
            imageUri = SHARK_TOKEN_IMAGE,
        )
        description = "Whenever you cast a noncreature spell, create an X/X blue Shark creature " +
            "token with flying, where X is that spell's mana value."
    }

    keywordAbility(KeywordAbility.cycling("{X}{1}{U}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.xValue(),
            dynamicToughness = DynamicAmounts.xValue(),
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Shark"),
            keywords = setOf(Keyword.FLYING),
            imageUri = SHARK_TOKEN_IMAGE,
        )
        description = "When you cycle this card, create an X/X blue Shark creature token with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1da4d4f3-b3cb-4b61-81b8-06ae441c41bf.jpg?1783931069"
        ruling(
            "2020-04-17",
            "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value."
        )
        ruling(
            "2020-04-17",
            "You can choose 0 as the value of X in Shark Typhoon's cycling cost. The last ability will trigger, and " +
                "you'll create a 0/0 blue Shark creature token with flying. Unless something else is raising its " +
                "toughness, the Shark will then immediately die."
        )
    }
}
