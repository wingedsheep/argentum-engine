package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cateran Overlord
 * {4}{B}{B}{B}
 * Creature — Horror Mercenary
 * 7 / 5
 */
val CateranOverlord = card("Cateran Overlord") {
    manaCost = "{4}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror Mercenary"
    oracleText = "Sacrifice a creature: Regenerate this creature.\n" +
        "{6}, {T}: Search your library for a Mercenary permanent card with mana value 6 or less, put it onto the battlefield, then shuffle."
    power = 7
    toughness = 5

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Creature)
        effect = RegenerateEffect(EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{6}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Permanent.withSubtype("Mercenary").manaValueAtMost(6),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Michael Sutfin"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8a1ffcb-40a7-423f-b28a-b5b4c1c9ffd0.jpg"
    }
}
