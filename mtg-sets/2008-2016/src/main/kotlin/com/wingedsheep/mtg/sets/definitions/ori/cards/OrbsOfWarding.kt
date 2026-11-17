package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofToController
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Orbs of Warding — Magic Origins #234
 * {5} · Artifact · Rare
 *
 * You have hexproof.
 * If a creature would deal damage to you, prevent 1 of that damage.
 *
 * "You have hexproof" is the [GrantHexproofToController] static (Leyline of Sanctity). The second
 * line is Sphere of Purity's continuous [PreventDamage] replacement with the source filter moved
 * from artifacts to creatures: `amount = 1` takes one point off each damage event from each
 * creature source, combat and noncombat alike, and [Recipient.You] limits it to damage dealt to
 * the controller — never their creatures or planeswalkers. Multiple Orbs are cumulative, as the
 * ruling says: each is its own replacement effect.
 */
val OrbsOfWarding = card("Orbs of Warding") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "You have hexproof. (You can't be the target of spells or abilities your opponents control.)\n" +
        "If a creature would deal damage to you, prevent 1 of that damage."

    staticAbility {
        ability = GrantHexproofToController
    }

    replacementEffect(
        PreventDamage(
            amount = DynamicAmounts.fixed(1),
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.You,
                source = GameObjectFilter.Creature
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "234"
        artist = "Johann Bodin"
        flavorText = "One orb to guard the body, one to protect the mind, and one to shield the soul."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c33ba4e5-217f-47e3-a6ce-f935a70c7566.jpg?1783938309"
        ruling("2015-06-22", "The damage prevention effects of multiple Orbs of Warding are cumulative.")
    }
}
