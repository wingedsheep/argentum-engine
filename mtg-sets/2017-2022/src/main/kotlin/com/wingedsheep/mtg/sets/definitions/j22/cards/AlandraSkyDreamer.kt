package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Alandra, Sky Dreamer
 * {2}{U}{U}
 * Legendary Creature — Merfolk Wizard
 * 2/4
 *
 * Whenever you draw your second card each turn, create a 2/2 blue Drake creature token with flying.
 * Whenever you draw your fifth card each turn, Alandra and Drakes you control each get +X/+X until
 * end of turn, where X is the number of cards in your hand.
 *
 * - Both triggers are `Triggers.you.drawsNth(n)`, which fires once per turn on exactly the Nth draw.
 * - "Alandra and Drakes you control" is Alandra itself plus every *other* Drake you control, so an
 *   Alandra that is somehow also a Drake is pumped once, not twice. X (cards in your hand) is read
 *   as the ability resolves and is fixed from then on (per the 2022-12-02 ruling).
 */
val AlandraSkyDreamer = card("Alandra, Sky Dreamer") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 2
    toughness = 4
    oracleText = "Whenever you draw your second card each turn, create a 2/2 blue Drake creature token with flying.\n" +
        "Whenever you draw your fifth card each turn, Alandra and Drakes you control each get +X/+X until end of turn, where X is the number of cards in your hand."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Drake"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/f/4/f4a73034-e20f-4e7e-ac15-3460b1e9c69b.jpg?1783908592"
        )
        description = "Whenever you draw your second card each turn, create a 2/2 blue Drake creature token with flying."
    }

    triggeredAbility {
        trigger = Triggers.you.drawsNth(5)
        effect = Effects.ModifyStats(
            DynamicAmounts.cardsInYourHand(),
            DynamicAmounts.cardsInYourHand(),
            EffectTarget.Self
        ) then Patterns.Group.modifyStatsForAll(
            DynamicAmounts.cardsInYourHand(),
            DynamicAmounts.cardsInYourHand(),
            GroupFilter(GameObjectFilter.Creature.withSubtype("Drake").youControl()).other()
        )
        description = "Whenever you draw your fifth card each turn, Alandra and Drakes you control each get +X/+X until end of turn, where X is the number of cards in your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Caroline Gariba"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/61561585-f4fb-4e47-a65a-0c43a855ebcf.jpg?1783919193"
        ruling("2022-12-02", "The first triggered ability can trigger only once each turn. It doesn't matter whether Alandra, Sky Dreamer was on the battlefield when the first card was drawn. If it's not on the battlefield when the second card is drawn, the ability can't trigger at all that turn. It won't trigger when the third or fourth card is drawn.")
        ruling("2022-12-02", "The same is true for the last ability and the fifth card drawn.")
        ruling("2022-12-02", "The value of X is locked in as the last ability resolves. The bonus it grants won't change after that point, even if the number of cards in your hand does.")
    }
}
