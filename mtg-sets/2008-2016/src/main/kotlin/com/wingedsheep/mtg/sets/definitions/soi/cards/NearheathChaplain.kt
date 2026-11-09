package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Nearheath Chaplain (Shadows over Innistrad #29)
 * {3}{W}
 * Creature — Human Cleric
 * 3/1
 *
 * Lifelink
 * {2}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens with
 * flying. Activate only as a sorcery.
 */
val NearheathChaplain = card("Nearheath Chaplain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 3
    toughness = 1
    oracleText = "Lifelink\n" +
        "{2}{W}, Exile this card from your graveyard: Create two 1/1 white Spirit creature tokens with " +
        "flying. Activate only as a sorcery."

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.ExileSelf)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            count = 2
        )
        timing = TimingRule.SorcerySpeed
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/362ec364-39c1-4a4b-8dfa-268fad2effdd.jpg"
    }
}
