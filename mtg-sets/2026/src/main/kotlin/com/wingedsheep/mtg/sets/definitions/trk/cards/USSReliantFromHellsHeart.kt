package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.station
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * U.S.S. Reliant, From Hell's Heart
 * {2}{B}{B}
 * Legendary Artifact — Spacecraft
 * 4/3
 *
 * Whenever U.S.S. Reliant enters or deals combat damage to a player, return target creature card
 * with mana value 4 or less from your graveyard to the battlefield with a finality counter on it.
 * Station (Tap another creature you control: Put charge counters equal to its power on this
 * Spacecraft. Station only as a sorcery. It's an artifact creature at 10+.)
 * 10+ | Flying
 *
 * The "enters or deals combat damage to a player" trigger is two triggered abilities sharing one
 * effect — the two events can never coincide, so splitting them is rules-identical. The returned
 * creature enters *with* its finality counter (an entry counter, not one added afterwards).
 */
val USSReliantFromHellsHeart = card("U.S.S. Reliant, From Hell's Heart") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Artifact — Spacecraft"
    power = 4
    toughness = 3
    oracleText = "Whenever U.S.S. Reliant enters or deals combat damage to a player, return target " +
        "creature card with mana value 4 or less from your graveyard to the battlefield with a " +
        "finality counter on it.\n" +
        "Station (Tap another creature you control: Put charge counters equal to its power on this " +
        "Spacecraft. Station only as a sorcery. It's an artifact creature at 10+.)\n" +
        "10+ | Flying"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(4))
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            entryCounters = listOf(EntryCounter(CounterType.FINALITY))
        )
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(4))
        effect = Effects.Move(
            creatureCard,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            entryCounters = listOf(EntryCounter(CounterType.FINALITY))
        )
    }

    station()

    val charge10 = Conditions.SourceCounterCountAtLeast(CounterType.CHARGE, 10)

    staticAbility {
        condition = charge10
        ability = GrantCardType("CREATURE", GroupFilter.source())
    }

    staticAbility {
        condition = charge10
        ability = GrantKeyword(Keyword.FLYING, GroupFilter.source())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Dofresh"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4c6fc55-f585-4529-b549-31d4d838c9af.jpg?1792551037"
    }
}
