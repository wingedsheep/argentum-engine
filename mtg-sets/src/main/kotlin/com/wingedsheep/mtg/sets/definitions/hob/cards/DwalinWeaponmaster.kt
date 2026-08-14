package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dwalin, Weaponmaster
 * {1}{R/W}
 * Legendary Creature — Dwarf Warrior
 * 2/1
 *
 * First strike
 * Whenever Dwalin enters or attacks, put a hone counter on each Equipment you control.
 *
 * Dwalin is the card that forces hone counters to be a property of the *counter* rather than an
 * ability printed on the Equipment: he hands a counter to **every** Equipment you control, most of
 * which have never heard of hone, and CR 122.1j still gives each of their equipped creatures +1/+0.
 * See [Counters.HONE] — the bonus is synthesized in `StateProjector`, so nothing is needed here
 * beyond placing the counters.
 *
 * "Enters or attacks" is one printed ability but two engine triggers; the project models that split
 * the way Sentinel of the Nameless City does, since there is no combined enters-or-attacks trigger.
 * Note the counters land on Equipment whether or not they're attached to anything — an unattached
 * honed Equipment simply banks the bonus until it's equipped later.
 */
val DwalinWeaponmaster = card("Dwalin, Weaponmaster") {
    manaCost = "{1}{R/W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Dwarf Warrior"
    oracleText = "First strike\n" +
        "Whenever Dwalin enters or attacks, put a hone counter on each Equipment you control. " +
        "(Each hone counter on an Equipment grants +1/+0 to equipped creature.)"
    power = 2
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)

    val honeEachEquipment = Effects.ForEachInGroup(
        GroupFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()),
        AddCountersEffect(Counters.HONE, 1, EffectTarget.Self),
    )

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = honeEachEquipment
        description = "Whenever Dwalin enters, put a hone counter on each Equipment you control."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = honeEachEquipment
        description = "Whenever Dwalin attacks, put a hone counter on each Equipment you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Marco Teixeira"
        flavorText = "The Company had lost a great deal, but they had killed the Great Goblin and " +
            "many others besides. All in all, they had the best of it."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/196d9287-a37d-4b27-a83b-a5489a54f081.jpg?1785496508"
    }
}
