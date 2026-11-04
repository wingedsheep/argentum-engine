package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val FerventPaincaster = card("Fervent Paincaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    oracleText = "{T}: This creature deals 1 damage to target player or planeswalker.\n" +
        "{T}, Exert this creature: It deals 1 damage to target creature. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 3
    toughness = 1

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.Exert)
        val t = target(TargetFilter.Creature)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Matt Stewart"
        flavorText = "\"Everyone here is a honed fighter. We'll respond the only way we know how.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3c78708-254c-446f-b6eb-8839faf08790.jpg?1783936030"
    }
}
