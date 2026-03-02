package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Skirk Drill Sergeant
 * {1}{R}
 * Creature — Goblin
 * 2/1
 * Whenever Skirk Drill Sergeant or another Goblin dies, you may pay {2}{R}.
 * If you do, reveal the top card of your library. If it's a Goblin permanent card,
 * put it onto the battlefield. Otherwise, put it into your graveyard.
 */
val SkirkDrillSergeant = card("Skirk Drill Sergeant") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Goblin"
    power = 2
    toughness = 1
    oracleText = "Whenever Skirk Drill Sergeant or another Goblin dies, you may pay {2}{R}. If you do, reveal the top card of your library. If it's a Goblin permanent card, put it onto the battlefield. Otherwise, put it into your graveyard."

    val revealAndPlace = CompositeEffect(
        listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "revealed",
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "revealed",
                selection = SelectionMode.All,
                filter = GameObjectFilter.Permanent.withSubtype(Subtype.GOBLIN),
                storeSelected = "goblin",
                storeRemainder = "nonGoblin"
            ),
            MoveCollectionEffect(
                from = "goblin",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            MoveCollectionEffect(
                from = "nonGoblin",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            )
        )
    )

    val mayPayEffect = MayPayManaEffect(
        cost = ManaCost.parse("{2}{R}"),
        effect = revealAndPlace
    )

    // "Whenever Skirk Drill Sergeant ... dies" (self death trigger)
    triggeredAbility {
        trigger = Triggers.Dies
        effect = mayPayEffect
    }

    // "... or another Goblin dies" (other Goblin death trigger)
    triggeredAbility {
        trigger = Triggers.OtherCreatureWithSubtypeDies(Subtype.GOBLIN)
        effect = mayPayEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"I order you to volunteer.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/359b2d1a-4027-46d9-b780-bcac8d60ecdb.jpg?1562905832"
    }
}
