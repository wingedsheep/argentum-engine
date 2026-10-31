package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bog Naughty
 * {3}{B}{B}
 * Creature — Faerie
 * 3/3
 * Flying
 * {2}{B}, Sacrifice a Food: Target creature gets -3/-3 until end of turn.
 *
 * "Sacrifice a Food" matches any Food artifact, not just tokens (2024-11-08 ruling), so the cost
 * filters on the Food subtype as Greta, Sweettooth Scourge does.
 */
val BogNaughty = card("Bog Naughty") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie"
    power = 3
    toughness = 3
    oracleText = "Flying\n{2}{B}, Sacrifice a Food: Target creature gets -3/-3 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food")),
        )
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-3, -3, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cf282fe-83fd-4208-b46f-dae76e3a7f62.jpg?1783932643"
        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact token. " +
                "For example, you can sacrifice Tough Cookie (an Artifact Creature — Food Golem) to activate " +
                "Maraleaf Rider's ability (an ability with \"Sacrifice a Food\" in its cost)."
        )
    }
}
