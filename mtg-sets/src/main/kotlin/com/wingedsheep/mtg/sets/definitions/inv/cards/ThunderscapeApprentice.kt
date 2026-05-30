package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Thunderscape Apprentice
 * {R}
 * Creature — Human Wizard
 * 1/1
 * {B}, {T}: Target player loses 1 life.
 * {G}, {T}: Target creature gets +1/+1 until end of turn.
 */
val ThunderscapeApprentice = card("Thunderscape Apprentice") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "{B}, {T}: Target player loses 1 life.\n" +
        "{G}, {T}: Target creature gets +1/+1 until end of turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        val t = target("target", TargetPlayer())
        effect = Effects.LoseLife(1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val t = target("target", Targets.Creature)
        effect = Effects.ModifyStats(power = 1, toughness = 1, target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a0b075-5414-48d3-a2b1-47dc20213e96.jpg?1562918595"
    }
}
