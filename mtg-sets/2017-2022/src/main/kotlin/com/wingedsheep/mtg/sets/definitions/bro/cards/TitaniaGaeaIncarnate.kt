package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Titania, Gaea Incarnate
 * Legendary Creature — Elemental Avatar (green color indicator)
 * * / *
 * Reach, vigilance, trample, haste
 * Titania's power and toughness are each equal to the number of lands you control.
 * When Titania enters, return all land cards from your graveyard to the battlefield tapped.
 * {3}{G}: Put four +1/+1 counters on target land you control. It becomes a 0/0 Elemental creature
 * with haste. It's still a land.
 *
 * The meld result of Titania, Voice of Gaea + Argoth, Sanctum of Nature (`meldOf`); it only ever
 * exists by melding the pair (Titania's upkeep trigger). The land animation has no duration, so it
 * lasts indefinitely (2022-10-14 ruling).
 */
val TitaniaGaeaIncarnate = card("Titania, Gaea Incarnate") {
    manaCost = ""
    meldOf("Titania, Voice of Gaea", "Argoth, Sanctum of Nature")
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elemental Avatar"
    oracleText = "Reach, vigilance, trample, haste\n" +
        "Titania's power and toughness are each equal to the number of lands you control.\n" +
        "When Titania enters, return all land cards from your graveyard to the battlefield tapped.\n" +
        "{3}{G}: Put four +1/+1 counters on target land you control. It becomes a 0/0 Elemental " +
        "creature with haste. It's still a land."

    // Characteristic-defining ability (CR 604.3).
    dynamicStats(DynamicAmounts.landsYouControl())

    keywords(Keyword.REACH, Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val graveyardLands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land))
            move(graveyardLands, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
        }
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        val land = target(TargetFilter.Land.youControl())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 4, land) then
            Effects.BecomeCreature(
                target = land,
                power = 0,
                toughness = 0,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.Permanent
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "256b"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/414b9230-9d25-4bdf-8b1e-b4fa2035b6a4.jpg?1783920008"
        ruling(
            "2022-10-14",
            "Titania, Gaea Incarnate's last ability doesn't have a duration, which means that the land remains a creature indefinitely."
        )
    }
}
