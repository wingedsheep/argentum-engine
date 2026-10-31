package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

private const val MU_YANLING_EMBLEM = "Islands you control have '{T}: Draw a card.'"

/**
 * Mu Yanling, Sky Dancer
 * {1}{U}{U}
 * Legendary Planeswalker — Yanling
 * Loyalty: 2
 *
 * +2: Until your next turn, up to one target creature gets -2/-0 and loses flying.
 * −3: Create a 4/4 blue Elemental Bird creature token with flying.
 * −8: You get an emblem with "Islands you control have '{T}: Draw a card.'"
 *
 * The emblem's grant is read dynamically off the group filter (Liliana of the Dark Realms' shape),
 * so Islands that enter later gain the ability too.
 */
val MuYanlingSkyDancer = card("Mu Yanling, Sky Dancer") {
    val islandDraw = grantedActivatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1)
        description = "{T}: Draw a card."
    }

    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Yanling"
    startingLoyalty = 2
    oracleText = "+2: Until your next turn, up to one target creature gets -2/-0 and loses flying.\n" +
        "−3: Create a 4/4 blue Elemental Bird creature token with flying.\n" +
        "−8: You get an emblem with \"$MU_YANLING_EMBLEM\""

    loyaltyAbility(+2) {
        val creature = target(TargetFilter.Creature, optional = true)
        effect = Effects.ModifyStats(-2, 0, creature, Duration.UntilYourNextTurn) then
            Effects.RemoveKeyword(Keyword.FLYING, creature, Duration.UntilYourNextTurn)
        description = "Until your next turn, up to one target creature gets -2/-0 and loses flying."
    }

    loyaltyAbility(-3) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Elemental", "Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/5/a5582cd9-97e7-426f-a084-c05e2113ad13.jpg?1783932855",
        )
        description = "Create a 4/4 blue Elemental Bird creature token with flying."
    }

    loyaltyAbility(-8) {
        effect = Effects.CreatePermanentEmblem(
            groupFilter = GroupFilter(GameObjectFilter.Land.withSubtype(Subtype.ISLAND).youControl()),
            grantedActivatedAbilities = listOf(islandDraw),
            emblemDescription = MU_YANLING_EMBLEM,
        )
        description = "You get an emblem with \"$MU_YANLING_EMBLEM\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "G-host Lee"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/0240f9eb-e1b2-4b41-94b0-b00ad2850825.jpg?1783933006"
        ruling("2019-07-12", "Mu Yanling's first ability can target a creature that doesn't have flying. The creature just gets -2/-0.")
        ruling("2019-07-12", "After Mu Yanling's first ability has resolved, the target creature will have flying again if another effect grants it flying.")
        ruling("2019-07-12", "An Island is a land with the subtype Island, not any land with a mana ability that produces blue mana.")
        ruling("2019-07-12", "Tapping an Island for mana is an activated ability. You can't activate that ability and also activate the ability it gains from Mu Yanling's emblem.")
        ruling("2019-07-12", "In a multiplayer game, if you leave the game after Mu Yanling's first ability resolves but before your next turn begins, its effect lasts until your next turn would have begun. It neither expires immediately nor lasts indefinitely.")
    }
}
