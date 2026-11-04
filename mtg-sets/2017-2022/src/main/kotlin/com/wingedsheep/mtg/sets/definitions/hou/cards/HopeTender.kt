package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val HopeTender = card("Hope Tender") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    oracleText = "{1}, {T}: Untap target land.\n" +
        "{1}, {T}, Exert this creature: Untap two target lands. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val land = target(TargetFilter.Land)
        effect = Effects.Untap(land)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.Exert)
        targets(TargetFilter.Land, count = 2)
        effect = Effects.UntapEachTarget()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Magali Villeneuve"
        flavorText = "Some refused to leave their shattered homes, vowing to rebuild at any cost."
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9ffb676d-54b7-4d30-9c84-ff9b48030705.jpg?1783936018"
    }
}
