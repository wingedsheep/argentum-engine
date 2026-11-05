package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Djeru, With Eyes Open
 * {3}{W}{W}
 * Legendary Creature — Human Warrior
 * 4/3
 * Vigilance
 * When Djeru enters, you may search your library for a planeswalker card, reveal it, put it into
 * your hand, then shuffle.
 * If a source would deal damage to a planeswalker you control, prevent 1 of that damage.
 *
 * The prevention is a continuous [PreventDamage] replacement (the Daunting Defender shape) over
 * planeswalkers you control; it applies once per source per planeswalker, so several attackers
 * hitting one planeswalker each lose 1 damage (the 2017-07-14 ruling).
 */
val DjeruWithEyesOpen = card("Djeru, With Eyes Open") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "Vigilance\n" +
        "When Djeru enters, you may search your library for a planeswalker card, reveal it, put it into your hand, then shuffle.\n" +
        "If a source would deal damage to a planeswalker you control, prevent 1 of that damage."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Planeswalker,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    replacementEffect(
        PreventDamage(
            amount = 1,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Object(GameObjectFilter.Planeswalker.youControl())
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2234df89-0835-4a17-b5d8-2334ee8f692d.jpg?1783936062"
        ruling(
            "2017-07-14",
            "If multiple sources would deal damage to one or more planeswalkers you control at once (for example, several attacking creatures), 1 damage from each of those sources to each of those planeswalkers is prevented."
        )
    }
}
