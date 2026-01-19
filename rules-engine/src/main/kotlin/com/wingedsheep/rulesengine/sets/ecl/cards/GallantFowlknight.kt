package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Gallant Fowlknight
 *
 * {3}{W} Creature — Kithkin Knight 3/4
 * When this creature enters, creatures you control get +1/+0 until end of turn.
 * Kithkin creatures you control also gain first strike until end of turn.
 */
object GallantFowlknight {
    val definition = CardDefinition.creature(
        name = "Gallant Fowlknight",
        manaCost = ManaCost.parse("{3}{W}"),
        subtypes = setOf(Subtype.KITHKIN, Subtype.KNIGHT),
        power = 3,
        toughness = 4,
        oracleText = "When this creature enters, creatures you control get +1/+0 until end of turn. " +
                "Kithkin creatures you control also gain first strike until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "17",
            rarity = Rarity.COMMON,
            artist = "Edgar Sánchez Hidalgo",
            flavorText = "The thoughtweft amplifies any small bud of courage, turning fear to resolve and daring to heroics.",
            imageUri = "https://cards.scryfall.io/normal/front/f/b/fb6096ba-8083-4207-9a3f-c1e4ff095204.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Gallant Fowlknight") {
        // ETB: All creatures you control get +1/+0 until end of turn
        triggered(
            trigger = OnEnterBattlefield(),
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 0,
                target = EffectTarget.AllControlledCreatures,
                untilEndOfTurn = true
            )
        )

        // ETB: Kithkin creatures you control gain first strike until end of turn
        // Note: This is part of the same trigger but affects a subset of creatures
        // Full implementation would need a filtered target like "AllControlledKithkin"
        triggered(
            trigger = OnEnterBattlefield(),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.FIRST_STRIKE,
                target = EffectTarget.AllControlledCreatures  // Ideally: AllControlledKithkin
            )
        )
    }
}
