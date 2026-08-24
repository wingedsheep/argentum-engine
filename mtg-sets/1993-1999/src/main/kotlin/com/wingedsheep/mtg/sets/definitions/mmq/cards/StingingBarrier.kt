package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Stinging Barrier
 * {2}{U}{U}
 * Creature — Wall
 * 0 / 4
 *
 * "This creature deals" is the ability's own source, which is the [Effects.DealDamage]
 * default — no `damageSource` rider.
 */
val StingingBarrier = card("Stinging Barrier") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    oracleText = "Defender (This creature can't attack.)\n" +
        "{U}, {T}: This creature deals 1 damage to any target."
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        val t = target("target", Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Pat Lewis"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca7f7cd5-4e91-474a-9f60-a66f3f462b1c.jpg"
    }
}
