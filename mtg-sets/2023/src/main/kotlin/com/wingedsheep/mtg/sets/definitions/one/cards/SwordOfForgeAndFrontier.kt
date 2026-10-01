package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Sword of Forge and Frontier — Phyrexia: All Will Be One #244
 * {3} · Artifact — Equipment · Mythic
 *
 * Equipped creature gets +2/+2 and has protection from red and from green.
 * Whenever equipped creature deals combat damage to a player, exile the top two cards of your
 * library. You may play those cards this turn. You may play an additional land this turn.
 * Equip {2}
 *
 * The trigger is an impulse draw of two ([Patterns.Exile.impulse], may-play expiring at end of
 * turn) followed by one additional land drop for the turn ([Effects.PlayAdditionalLands]).
 */
val SwordOfForgeAndFrontier = card("Sword of Forge and Frontier") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has protection from red and from green.\n" +
        "Whenever equipped creature deals combat damage to a player, exile the top two cards of " +
        "your library. You may play those cards this turn. You may play an additional land this turn.\n" +
        "Equip {2}"

    // Equipped creature gets +2/+2 ...
    staticAbility {
        ability = ModifyStats(+2, +2, Filters.EquippedCreature)
    }
    // ... and has protection from red and from green.
    staticAbility {
        ability = GrantProtection(Color.RED, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantProtection(Color.GREEN, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Exile.impulse(count = 2) then Effects.PlayAdditionalLands(1)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "244"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2daa3621-8a2c-4b4b-87ac-f981192a0567.jpg?1783917985"

        ruling(
            "2023-02-04",
            "You must follow all normal timing rules for a card you play using Sword of Forge and " +
                "Frontier's second ability and, if it's a spell, you must pay its costs to cast it."
        )
        ruling(
            "2023-02-04",
            "The effect that allows you to play an additional land that turn is cumulative with other " +
                "effects that do so, including itself. If the second ability triggers multiple times in " +
                "the same turn, perhaps because there were multiple combats, you will be able to play " +
                "that many additional lands."
        )
        ruling(
            "2023-02-04",
            "If the equipped creature somehow deals combat damage to a player on an opponent's turn, " +
                "the second ability will trigger. You'll be able to cast cards you exile if timing rules " +
                "allow (so, instants, mostly), but you won't be able to play a land that turn."
        )
    }
}
