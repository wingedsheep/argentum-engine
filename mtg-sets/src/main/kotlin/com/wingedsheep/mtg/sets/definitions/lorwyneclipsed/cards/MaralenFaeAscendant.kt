package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Maralen, Fae Ascendant
 * {2}{B}{G}{U}
 * Legendary Creature — Elf Faerie Noble
 * 4/5
 *
 * Flying
 * Whenever Maralen or another Elf or Faerie you control enters, exile the top two
 * cards of target opponent's library.
 * Once each turn, you may cast a spell with mana value less than or equal to the
 * number of Elves and Faeries you control from among cards exiled with Maralen
 * this turn without paying its mana cost.
 */
val MaralenFaeAscendant = card("Maralen, Fae Ascendant") {
    manaCost = "{2}{B}{G}{U}"
    typeLine = "Legendary Creature — Elf Faerie Noble"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever Maralen or another Elf or Faerie you control enters, exile the top two " +
        "cards of target opponent's library.\n" +
        "Once each turn, you may cast a spell with mana value less than or equal to the " +
        "number of Elves and Faeries you control from among cards exiled with Maralen this " +
        "turn without paying its mana cost."

    keywords(Keyword.FLYING)

    val elfOrFaerieYouControl = GameObjectFilter.Creature
        .youControl()
        .withAnyOfSubtypes(listOf(Subtype("Elf"), Subtype("Faerie")))

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = elfOrFaerieYouControl,
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        val opponent = target("opponent", TargetOpponent())
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2), Player.ContextPlayer(0)),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE),
                    linkToSource = true
                )
            )
        )
    }

    staticAbility {
        ability = GrantMayCastFromLinkedExile(
            filter = GameObjectFilter.Nonland,
            withoutPayingManaCost = true,
            oncePerTurn = true,
            exiledThisTurnOnly = true,
            maxManaValue = DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = elfOrFaerieYouControl,
                aggregation = Aggregation.COUNT
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c50f5408-5b5c-41dc-807e-136233403a09.jpg?1767862685"
        ruling("2025-11-17", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2025-11-17", "If Maralen enters at the same time as one or more other Elves or Faeries you control, its first ability will trigger for each of those other Elves and Faeries as well as itself.")
        ruling("2025-11-17", "If an exiled card has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
        ruling("2025-11-17", "Since you are using an alternative cost to cast the spell, you can't pay any other alternative costs. You can, however, pay additional costs, such as kicker costs. If the card has any mandatory additional costs, you must pay those.")
    }
}
