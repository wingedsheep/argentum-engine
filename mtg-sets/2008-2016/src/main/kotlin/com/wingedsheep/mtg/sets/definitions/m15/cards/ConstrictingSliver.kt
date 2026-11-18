package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Constricting Sliver
 * {5}{W}
 * Creature — Sliver
 * 3/3
 * Sliver creatures you control have "When this creature enters, you may exile target creature an
 * opponent controls until this creature leaves the battlefield."
 *
 * "This creature" is the Sliver that has the granted ability, so the granted trigger's source is
 * that Sliver: `MoveUntilSourceLeaves` ties the exile to *its* battlefield visit, returns the card
 * the moment that Sliver leaves (no leaves trigger, so it still returns after Constricting Sliver is
 * gone and the ability with it), and exiles nothing if that Sliver has already left on resolution.
 */
val ConstrictingSliver = card("Constricting Sliver") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Sliver"
    power = 3
    toughness = 3
    oracleText = "Sliver creatures you control have \"When this creature enters, you may exile target " +
        "creature an opponent controls until this creature leaves the battlefield.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.self.enters()
                val creature = target(TargetFilter.CreatureOpponentControls)
                effect = Effects.May(Effects.MoveUntilSourceLeaves(creature, Zone.EXILE))
            },
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.SLIVER).youControl())
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Karl Kopinski"
        flavorText = "Slivers are often seen toying with enemies they capture, not out of cruelty, but " +
            "to fully learn their physical capabilities."
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae9b4111-ca9f-435b-81c4-0abf65d1384b.jpg?1783939204"
        ruling(
            "2014-07-18",
            "When Constricting Sliver enters the battlefield, the ability it gives itself will trigger."
        )
        ruling(
            "2014-07-18",
            "The Sliver that enters the battlefield is the source of the ability that exiles the creature."
        )
        ruling(
            "2014-07-18",
            "If a Sliver leaves the battlefield before its enters-the-battlefield ability resolves, the " +
                "target creature won't be exiled."
        )
        ruling(
            "2014-07-18",
            "If a Sliver you control leaves the battlefield, only the card that particular Sliver exiled " +
                "will return to the battlefield."
        )
        ruling(
            "2014-07-18",
            "The exiled card will return even if the Sliver whose ability exiled it doesn't have that " +
                "ability at the time (perhaps because Constricting Sliver is no longer on the battlefield)."
        )
    }
}
