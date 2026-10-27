package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Malakir Bloodwitch
 * {3}{B}{B}
 * Creature — Vampire Shaman
 * 4/4
 * Flying, protection from white
 * When this creature enters, each opponent loses life equal to the number of Vampires you control.
 * You gain life equal to the life lost this way.
 *
 * [Effects.DrainLife] is the "each opponent loses X, you gain the life lost this way" shape; X is
 * counted as the trigger resolves (the Bloodwitch counts itself if it's still on the battlefield).
 */
val MalakirBloodwitch = card("Malakir Bloodwitch") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Shaman"
    oracleText = "Flying, protection from white\nWhen this creature enters, each opponent loses life " +
        "equal to the number of Vampires you control. You gain life equal to the life lost this way."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.VAMPIRE)
            ).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "100"
        artist = "Shelly Wan"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/865ac36f-4c61-468b-a2ed-ed2d1bbbf4e3.jpg?1783942152"
    }
}
