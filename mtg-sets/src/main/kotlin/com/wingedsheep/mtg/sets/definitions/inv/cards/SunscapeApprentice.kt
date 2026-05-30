package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunscape Apprentice
 * {W}
 * Creature — Human Wizard
 * 1/1
 * {G}, {T}: Target creature gets +1/+1 until end of turn.
 * {U}, {T}: Put target creature you control on top of its owner's library.
 */
val SunscapeApprentice = card("Sunscape Apprentice") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{G}, {T}: Target creature gets +1/+1 until end of turn.\n" +
        "{U}, {T}: Put target creature you control on top of its owner's library."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{U}"), Costs.Tap)
        val t = target("target", Targets.CreatureYouControl)
        effect = Effects.PutOnTopOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "41"
        artist = "Stephanie Law"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9d6bd19-77c9-4a1a-a2d5-6f9737693fea.jpg?1562929204"
    }
}
