package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveCardType
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Karametra, God of Harvests — Born of the Gods #148
 * {3}{G}{W} · Legendary Enchantment Creature — God · 6/7
 *
 * Indestructible
 * As long as your devotion to green and white is less than seven, Karametra isn't a creature.
 * Whenever you cast a creature spell, you may search your library for a Forest or Plains card,
 * put it onto the battlefield tapped, then shuffle.
 *
 * The devotion clause is the two-colour God template (Xenagos, Athreos). The cast trigger is
 * `Triggers.you.casts(Creature)` (a bestowed Aura spell isn't a creature spell — ruling) with
 * Grasslands' "Forest or Plains card" search (any land with either subtype — ruling), optional
 * as a whole.
 */
val KarametraGodOfHarvests = card("Karametra, God of Harvests") {
    manaCost = "{3}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Enchantment Creature — God"
    power = 6
    toughness = 7
    oracleText = "Indestructible\n" +
        "As long as your devotion to green and white is less than seven, Karametra isn't a creature.\n" +
        "Whenever you cast a creature spell, you may search your library for a Forest or Plains card, " +
        "put it onto the battlefield tapped, then shuffle."

    keywords(Keyword.INDESTRUCTIBLE)

    staticAbility {
        condition = Conditions.CompareAmounts(
            DynamicAmounts.devotionTo(Color.GREEN, Color.WHITE),
            ComparisonOperator.LT,
            7
        )
        ability = RemoveCardType("CREATURE", GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withAnySubtype("Forest", "Plains"),
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "Whenever you cast a creature spell, you may search your library for a Forest or " +
            "Plains card, put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "148"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74050cb3-ba99-475d-9124-726e498fb68e.jpg?1783939526"
        ruling(
            "2014-02-01",
            "You can use the last ability to put any land card with the subtype Forest or Plains onto the " +
                "battlefield, not just ones named Forest or Plains."
        )
        ruling(
            "2014-02-01",
            "If you cast a creature card with bestow for its bestow cost, it becomes an Aura spell and not a " +
                "creature spell. Karametra's last ability won't trigger."
        )
        ruling(
            "2020-11-10",
            "Your devotion to two colors is the number of mana symbols among mana costs of permanents you control " +
                "that are the first color, the second, or both. If an effect counts your devotion to two colors, a " +
                "hybrid symbol that is both of those colors is counted just once."
        )
        ruling(
            "2020-11-10",
            "If Karametra stops being a creature, it loses the type creature and the creature type God. It " +
                "continues to be a legendary enchantment."
        )
        ruling(
            "2020-11-10",
            "The type-changing ability that can make Karametra not be a creature functions only on the " +
                "battlefield. It's always a creature card in other zones, regardless of your devotion to its color. " +
                "It's always a creature spell while it's on the stack."
        )
    }
}
