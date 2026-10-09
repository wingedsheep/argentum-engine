package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val SilverbackElder = card("Silverback Elder") {
    manaCost = "{2}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ape Shaman"
    power = 5
    toughness = 7
    oracleText = "Whenever you cast a creature spell, choose one —\n" +
        "• Destroy target artifact or enchantment.\n" +
        "• Look at the top five cards of your library. You may put a land card from among them onto the battlefield tapped. Put the rest on the bottom of your library in a random order.\n" +
        "• You gain 4 life."

    triggeredAbility {
        trigger = Triggers.you.casts(Filters.Creature)
        effect = Effects.Modal(modes = listOf(
            mode("Destroy target artifact or enchantment") {
                effect = Effects.Destroy(target(TargetFilter.ArtifactOrEnchantment))
            },
            mode("Look at the top five cards and put up to one land onto the battlefield tapped") {
                effect = Patterns.Library.lookAtTopAndTakeMatching(
                    count = DynamicAmounts.fixed(5),
                    filter = Filters.Land,
                    prompt = "You may put a land card from among them onto the battlefield tapped",
                    selection = SelectionMode.ChooseUpTo(DynamicAmounts.fixed(1)),
                    keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                    restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    restOrder = CardOrder.Random
                )
            },
            mode("You gain 4 life") { effect = Effects.GainLife(4) }
        ))
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "177"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b987664f-0b74-4c0a-b306-14767a55559a.jpg?1783921295"
        ruling("2022-09-09", "Silverback Elder’s triggered ability resolves before the spell that caused it to trigger. The ability will resolve even if that spell is countered.")
    }
}
