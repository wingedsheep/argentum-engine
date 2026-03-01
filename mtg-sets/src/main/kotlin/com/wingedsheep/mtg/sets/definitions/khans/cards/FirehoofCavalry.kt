package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Firehoof Cavalry
 * {W}
 * Creature — Human Berserker
 * 1/1
 * {3}{R}: Firehoof Cavalry gets +2/+0 and gains trample until end of turn.
 */
val FirehoofCavalry = card("Firehoof Cavalry") {
    manaCost = "{W}"
    typeLine = "Creature — Human Berserker"
    power = 1
    toughness = 1
    oracleText = "{3}{R}: This creature gets +2/+0 and gains trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{3}{R}")
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "YW Tang"
        flavorText = "\"What warrior worth the name fears to leave a trail? If my enemies seek me, let them follow the ashes in my wake.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edb2b284-f79c-41eb-a25f-4710d4a5228f.jpg?1562795647"
    }
}
