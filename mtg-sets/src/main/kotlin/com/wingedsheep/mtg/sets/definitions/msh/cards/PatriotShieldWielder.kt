package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Patriot, Shield Wielder
 * {1}{W}
 * Legendary Creature — Human Hero
 * 2/2
 * {2}, {T}: Another target creature you control gets +2/+0 and gains hexproof until end of turn.
 *
 * Implementation note: "Another target creature you control" is `TargetFilter.Creature.youControl().other()`
 * — `.other()` sets `excludeSelf`, so Patriot can't target itself.
 */
val PatriotShieldWielder = card("Patriot, Shield Wielder") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Hero"
    oracleText = "{2}, {T}: Another target creature you control gets +2/+0 and gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)"
    power = 2
    toughness = 2
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.youControl().other()))
        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, t),
            Effects.GrantKeyword(Keyword.HEXPROOF, t),
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Vlad Petruchik"
        flavorText = "\"They say true heroes always put their teammates before themselves. I often find it's the other way around.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1467d7c9-00d8-43f1-b4e7-a279d2b10503.jpg?1783902968"
    }
}
