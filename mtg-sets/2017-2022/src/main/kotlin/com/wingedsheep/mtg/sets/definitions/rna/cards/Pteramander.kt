package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Pteramander {U}
 * Creature — Salamander Drake
 * 1/1
 * Flying
 * {7}{U}: Adapt 4. This ability costs {1} less to activate for each instant and sorcery card in
 * your graveyard.
 *
 * Same shape as Etherium Pteramander: `genericCostReduction` only trims generic mana, so {U}
 * always remains.
 */
val Pteramander = card("Pteramander") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Salamander Drake"
    oracleText = "Flying\n" +
        "{7}{U}: Adapt 4. This ability costs {1} less to activate for each instant and sorcery card in your graveyard. " +
        "(If this creature has no +1/+1 counters on it, put four +1/+1 counters on it.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{7}{U}")
        genericCostReduction = DynamicAmounts.count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery)
        effect = Effects.Adapt(4)
        description = "{7}{U}: Adapt 4. This ability costs {1} less to activate for each instant and sorcery card in your graveyard."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "47"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/280f2a85-1900-460b-a768-164fc2dea636.jpg?1783933707"
        ruling("2019-01-25", "Pteramander's activated ability can't reduce itself to less than {U} to activate.")
        ruling(
            "2019-01-25",
            "You can always activate an ability that will cause a creature to adapt. As that ability resolves, " +
                "if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.",
        )
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
