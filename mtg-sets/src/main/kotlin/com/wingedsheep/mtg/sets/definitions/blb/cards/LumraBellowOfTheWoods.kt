package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

// Lumra, Bellow of the Woods - {4}{G}{G}
// Legendary Creature — Elemental Bear - star/star
// Vigilance, reach
// P/T equal to number of lands you control.
// When Lumra enters, mill four cards. Then return all land cards from your graveyard
// to the battlefield tapped.
val LumraBellowOfTheWoods = card("Lumra, Bellow of the Woods") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental Bear"
    oracleText = "Vigilance, reach\nLumra, Bellow of the Woods's power and toughness are each equal to the number of lands you control.\nWhen Lumra, Bellow of the Woods enters, mill four cards. Then return all land cards from your graveyard to the battlefield tapped."

    dynamicStats(DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land))

    keywords(Keyword.VIGILANCE, Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.mill(4)
            .then(
                Effects.Pipeline {
                    val graveyardLands = gather(
                        CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
                        name = "graveyard_lands"
                    )
                    move(
                        graveyardLands,
                        CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
                    )
                }
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae4f3aaf-3960-48cd-b34b-32e4ae5ae088.jpg?1721426865"
        ruling("2024-07-26", "The ability that defines Lumra's power and toughness functions in all zones, not just the battlefield.")
    }
}
