package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elspeth, Sun's Nemesis
 * {2}{W}{W}
 * Legendary Planeswalker — Elspeth
 * Loyalty 5
 * −1: Up to two target creatures you control each get +2/+1 until end of turn.
 * −2: Create two 1/1 white Human Soldier creature tokens.
 * −3: You gain 5 life.
 * Escape—{4}{W}{W}, Exile four other cards from your graveyard.
 *
 * The −1 is an up-to-two multi-slot target read through [Effects.ForEachTarget]. Escape is the
 * same [KeywordAbility.escape] every THB escape card uses (Chainweb Aracnir); an escaped Elspeth
 * is a new permanent, so her loyalty abilities can be activated again that turn (ruling).
 */
val ElspethSunsNemesis = card("Elspeth, Sun's Nemesis") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 5
    oracleText = "−1: Up to two target creatures you control each get +2/+1 until end of turn.\n" +
        "−2: Create two 1/1 white Human Soldier creature tokens.\n" +
        "−3: You gain 5 life.\n" +
        "Escape—{4}{W}{W}, Exile four other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    loyaltyAbility(-1) {
        targets(TargetFilter.CreatureYouControl, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.ModifyStats(2, 1, EffectTarget.ContextTarget(0)))
        description = "Up to two target creatures you control each get +2/+1 until end of turn."
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Soldier"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e76f0e3-9411-401d-ab38-9c3c64769483.jpg?1783931455",
        )
        description = "Create two 1/1 white Human Soldier creature tokens."
    }

    loyaltyAbility(-3) {
        effect = Effects.GainLife(5)
        description = "You gain 5 life."
    }

    keywordAbility(KeywordAbility.escape("{4}{W}{W}", Costs.additional.ExileOtherCards(4)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "14"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81aec42f-abb7-42f9-96f5-f1ee2f94db52.jpg?1783931598"
        ruling(
            "2020-01-24",
            "If you activate a loyalty ability of Elspeth, she's put into your graveyard, and she escapes all " +
                "in the same turn, she'll be a new permanent. You can activate one of her loyalty abilities even " +
                "though she's represented by the same card."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later."
        )
    }
}
