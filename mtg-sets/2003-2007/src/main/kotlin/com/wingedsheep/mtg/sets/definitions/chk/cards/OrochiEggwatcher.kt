package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity

/**
 * Orochi Eggwatcher // Shidako, Broodmistress (Champions of Kamigawa #233) — a flip card (CR 710).
 *
 * Orochi Eggwatcher {2}{G} — Creature — Snake Shaman 1/1
 * "{2}{G}, {T}: Create a 1/1 green Snake creature token. If you control ten or more creatures,
 * flip this creature."
 *
 * Shidako, Broodmistress — Legendary Creature — Snake Shaman 3/3
 * "{G}, Sacrifice a creature: Target creature gets +3/+3 until end of turn."
 *
 * The creature count is taken after the Snake is created, so the token itself counts toward ten.
 */
private val OrochiEggwatcherUpright = card("Orochi Eggwatcher") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Snake Shaman"
    oracleText = "{2}{G}, {T}: Create a 1/1 green Snake creature token. If you control ten or more " +
        "creatures, flip this creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.Tap)
        effect = Effects.CreateToken(power = 1, toughness = 1, colors = setOf(Color.GREEN),
            creatureTypes = setOf("Snake")) then
            Effects.If(Conditions.ControlCreaturesAtLeast(10), Effects.Flip())
        description = "{2}{G}, {T}: Create a 1/1 green Snake creature token. If you control ten or " +
            "more creatures, flip this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4f4aa3b-c64a-4430-b1a2-a7fca87d0a22.jpg?1783944284"
    }
}

private val ShidakoBroodmistress = card("Shidako, Broodmistress") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Snake Shaman"
    oracleText = "{G}, Sacrifice a creature: Target creature gets +3/+3 until end of turn."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Sacrifice(GameObjectFilter.Creature))
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, creature)
        description = "{G}, Sacrifice a creature: Target creature gets +3/+3 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4f4aa3b-c64a-4430-b1a2-a7fca87d0a22.jpg?1783944284"
    }
}

val OrochiEggwatcher: CardDefinition = CardDefinition.flipCard(
    unflipped = OrochiEggwatcherUpright,
    flipped = ShidakoBroodmistress,
)
