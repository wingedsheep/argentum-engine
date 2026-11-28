package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDamageWithMill
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undead Alchemist (ISD 84)
 * {3}{U}
 * Creature — Zombie
 * 4/2
 *
 * If a Zombie you control would deal combat damage to a player, instead that player mills that
 * many cards.
 * Whenever a creature card is put into an opponent's graveyard from their library, exile that card
 * and create a 2/2 black Zombie creature token.
 *
 * The first ability is an "instead" replacement, not prevention ([ReplaceDamageWithMill] with
 * `prevents = false`), so it replaces combat damage that can't be prevented too; the damaged
 * player is the miller (`Player.TriggeringPlayer`). Several Alchemists replace the damage only
 * once (ruling). Per the second ruling, every Alchemist's trigger creates a token even when an
 * earlier one already exiled the card.
 */
val UndeadAlchemist = card("Undead Alchemist") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 2
    oracleText = "If a Zombie you control would deal combat damage to a player, instead that player " +
        "mills that many cards.\n" +
        "Whenever a creature card is put into an opponent's graveyard from their library, exile " +
        "that card and create a 2/2 black Zombie creature token."

    replacementEffect(
        ReplaceDamageWithMill(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyPlayer,
                source = GameObjectFilter.Creature.withSubtype("Zombie").youControl(),
                damageType = DamageType.Combat,
            ),
            miller = Player.TriggeringPlayer,
            prevents = false,
        )
    )

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByOpponent())
            .changesZone(from = Zone.LIBRARY, to = Zone.GRAVEYARD)
        effect = Effects.Exile(EffectTarget.TriggeringEntity, fromZone = Zone.GRAVEYARD) then
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Michael C. Hayes"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/717f4592-6c81-43ac-8975-f6d5d6710310.jpg?1783940962"
    }
}
