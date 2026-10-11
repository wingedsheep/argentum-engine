package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

private const val LILIANA_EMBLEM = "Swamps you control have '{T}: Add {B}{B}{B}{B}.'"

/**
 * Liliana of the Dark Realms - {2}{B}{B}
 * Legendary Planeswalker — Liliana
 * Starting Loyalty: 3
 *
 * +1: Search your library for a Swamp card, reveal it, put it into your hand, then shuffle.
 * −3: Target creature gets +X/+X or -X/-X until end of turn, where X is the number of Swamps you control.
 * −6: You get an emblem with "Swamps you control have '{T}: Add {B}{B}{B}{B}.'"
 *
 * The −3's sign is chosen as the ability resolves (2012-07-01 ruling), so it is a resolution-time
 * [Effects.ChooseAction] between the two pumps, each counting Swamps then. The emblem's grant is a
 * mana ability on each Swamp, read by the manual-tap enumerator and the auto-payer alike.
 */
val LilianaOfTheDarkRealms = card("Liliana of the Dark Realms") {
    val swampMana = grantedActivatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 4)
        manaAbility = true
        description = "{T}: Add {B}{B}{B}{B}."
    }

    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Liliana"
    startingLoyalty = 3
    oracleText = "+1: Search your library for a Swamp card, reveal it, put it into your hand, then shuffle.\n" +
        "−3: Target creature gets +X/+X or -X/-X until end of turn, where X is the number of Swamps you control.\n" +
        "−6: You get an emblem with \"$LILIANA_EMBLEM\""

    loyaltyAbility(+1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype("Swamp"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        val swamps = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Land.withSubtype("Swamp")).count()
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice("+X/+X", Effects.ModifyStats(swamps, swamps, creature)),
                EffectChoice("-X/-X", Effects.ModifyStats(-swamps, -swamps, creature)),
            )
        )
    }

    loyaltyAbility(-6) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter(GameObjectFilter.Land.withSubtype("Swamp").youControl()),
            grantedActivatedAbilities = listOf(swampMana),
            emblemDescription = LILIANA_EMBLEM,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "97"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cd2d81e-1388-4f34-9917-2289971cf8da.jpg?1783940493"
        ruling(
            "2012-07-01",
            "You choose whether the creature gets +X/+X or -X/-X as the second ability resolves.",
        )
        ruling(
            "2012-07-01",
            "The number of Swamps you control is counted when the second ability resolves.",
        )
    }
}
