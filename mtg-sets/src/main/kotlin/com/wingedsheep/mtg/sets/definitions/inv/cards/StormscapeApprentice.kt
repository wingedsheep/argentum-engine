package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Stormscape Apprentice
 * {U}
 * Creature — Human Wizard
 * 1/1
 * {W}, {T}: Tap target creature.
 * {B}, {T}: Target player loses 1 life.
 */
val StormscapeApprentice = card("Stormscape Apprentice") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{W}, {T}: Tap target creature.\n" +
        "{B}, {T}: Target player loses 1 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = TapUntapEffect(target = t, tap = true)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val t = target("target", TargetPlayer())
        effect = Effects.LoseLife(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1eb42f39-9187-44e4-aa34-14ab31977199.jpg?1562901059"
    }
}
