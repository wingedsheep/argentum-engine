package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Acquisition Octopus — Kamigawa: Neon Dynasty #44 (canonical printing)
 * {2}{U} · Artifact Creature — Equipment Octopus · 2/2
 *
 * Whenever this creature or equipped creature deals combat damage to a player, draw a card.
 * Reconfigure {2}
 *
 * The or-joined trigger is two triggered abilities (the house shape): one SELF-bound for the
 * unattached Octopus, one ATTACHED-bound for the equipped creature. They can never both fire off
 * the same combat damage step: an Equipment with reconfigure that is somehow still a creature
 * while attached immediately becomes unattached (2022-02-18 ruling).
 */
val AcquisitionOctopus = card("Acquisition Octopus") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Equipment Octopus"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature or equipped creature deals combat damage to a player, draw a card.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    reconfigure("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Francisco Miyara"
        flavorText = "You can never have too many arms."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e971a72-31db-4b7e-b9b9-9f45e8c3f87e.jpg?1783923908"
    }
}
