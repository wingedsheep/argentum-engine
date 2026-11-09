package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Geralf's Masterpiece (Shadows over Innistrad #65)
 * {3}{U}{U}
 * Creature — Zombie Horror
 * 7/7
 *
 * Flying
 * This creature gets -1/-1 for each card in your hand.
 * {3}{U}, Discard three cards: Return this card from your graveyard to the battlefield tapped.
 *
 * The shrink is Grim Strider's self-scoped [GrantDynamicStats] over your hand size; the return is
 * Stitchwing Skaab's graveyard-only activated ability with a discard cost.
 */
val GeralfsMasterpiece = card("Geralf's Masterpiece") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "This creature gets -1/-1 for each card in your hand.\n" +
        "{3}{U}, Discard three cards: Return this card from your graveyard to the battlefield tapped."

    keywords(Keyword.FLYING)

    staticAbility {
        val negHand = -DynamicAmounts.cardsInYourHand()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = negHand,
            toughnessBonus = negHand
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}"), Costs.Discard(count = 3))
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "65"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f00d28b8-2ed8-4266-a870-d308f9ebd5da.jpg?1783937797"
        ruling("2016-04-08", "Geralf's Masterpiece's last ability can only be activated while it's in your graveyard.")
        ruling("2016-04-08", "If a spell or ability causes you to draw cards and then discard cards, Geralf's Masterpiece may have a toughness of 0 while you choose which cards to discard. As long as it has toughness 1 or greater again after the spell or ability resolves, it will survive.")
    }
}
