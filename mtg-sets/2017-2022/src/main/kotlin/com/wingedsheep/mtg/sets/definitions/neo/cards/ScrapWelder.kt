package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scrap Welder — Kamigawa: Neon Dynasty #159 (canonical printing)
 * {2}{R} · Creature — Goblin Artificer · 3/3
 *
 * {T}, Sacrifice an artifact with mana value X: Return target artifact card with mana value less
 * than X from your graveyard to the battlefield. It gains haste until end of turn.
 *
 * The sacrifice is a single-permanent variable cost (`maxCount = 1`, measured by mana value): which
 * artifact is picked announces X (CR 601.2b), and the target's "less than X" is measured against
 * it. A card with mana value equal to X is not a legal target (the card's ruling).
 */
val ScrapWelder = card("Scrap Welder") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Artificer"
    power = 3
    toughness = 3
    oracleText = "{T}, Sacrifice an artifact with mana value X: Return target artifact card with mana " +
        "value less than X from your graveyard to the battlefield. It gains haste until end of turn."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificePermanents(
                GameObjectFilter.Artifact,
                maxCount = 1,
                xMeasure = VariableCostMeasure.TOTAL_MANA_VALUE,
            ),
        )
        val artifact = target(
            TargetFilter(
                baseFilter = GameObjectFilter.Artifact.ownedByYou().manaValueLessThanX(),
                zone = Zone.GRAVEYARD,
            ),
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(artifact) then
            Effects.GrantKeyword(Keyword.HASTE, artifact)
        description = "{T}, Sacrifice an artifact with mana value X: Return target artifact card with " +
            "mana value less than X from your graveyard to the battlefield. It gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Simon Dominic"
        flavorText = "\"This big metal guy will make some amazing wind chimes!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14057067-2325-4229-8580-d9a6397ae343.jpg?1783923860"
    }
}
