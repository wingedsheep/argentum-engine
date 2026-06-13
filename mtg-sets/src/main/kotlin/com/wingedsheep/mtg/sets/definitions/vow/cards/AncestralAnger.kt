package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ancestral Anger
 * {R}
 * Sorcery
 * Target creature gains trample and gets +X/+0 until end of turn, where X is 1 plus the
 * number of cards named Ancestral Anger in your graveyard.
 * Draw a card.
 *
 * X resolves at resolution: `1 + (cards named "Ancestral Anger" in your graveyard)`. The
 * graveyard count is `DynamicAmount.Count` over `CardPredicate.NameEquals`, offset by one via
 * `DynamicAmount.Add`. Each copy that has been cast and gone to the graveyard pumps the next.
 */
val AncestralAnger = card("Ancestral Anger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target creature gains trample and gets +X/+0 until end of turn, where X is 1 " +
        "plus the number of cards named Ancestral Anger in your graveyard.\nDraw a card."

    spell {
        val t = target("target", Targets.Creature)
        val pump = DynamicAmount.Add(
            DynamicAmount.Fixed(1),
            DynamicAmount.Count(
                player = Player.You,
                zone = Zone.GRAVEYARD,
                filter = GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.NameEquals("Ancestral Anger")),
                ),
            ),
        )
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.TRAMPLE, t),
            Effects.ModifyStats(pump, DynamicAmount.Fixed(0), t),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Randy Vargas"
        flavorText = "\"One vampire for every family member I've lost.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/d/5dee47ab-d603-4346-97f4-a25dc3f47765.jpg?1643590713"
    }
}
