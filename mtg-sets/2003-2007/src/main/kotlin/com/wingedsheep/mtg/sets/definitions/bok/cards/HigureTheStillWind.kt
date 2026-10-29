package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Higure, the Still Wind — Betrayers of Kamigawa #37 (canonical printing)
 * {3}{U}{U} · Legendary Creature — Human Ninja · 3/4
 *
 * Ninjutsu {2}{U}{U}
 * Whenever Higure deals combat damage to a player, you may search your library for a Ninja card,
 * reveal it, put it into your hand, then shuffle.
 * {2}: Target Ninja creature can't be blocked this turn.
 *
 * The tutor is Faerie Harbinger's optional [Patterns.Library.searchLibrary] (any card type with the
 * Ninja subtype); the evasion grant is Trailblazer's `CANT_BE_BLOCKED` flag, until end of turn.
 */
val HigureTheStillWind = card("Higure, the Still Wind") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Ninja"
    power = 3
    toughness = 4
    oracleText = "Ninjutsu {2}{U}{U} ({2}{U}{U}, Return an unblocked attacker you control to hand: Put " +
        "this card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever Higure deals combat damage to a player, you may search your library for a Ninja " +
        "card, reveal it, put it into your hand, then shuffle.\n" +
        "{2}: Target Ninja creature can't be blocked this turn."

    ninjutsu("{2}{U}{U}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.NINJA),
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        )
        description = "you may search your library for a Ninja card, reveal it, put it into your " +
            "hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        val t = target(TargetFilter.Creature.withSubtype(Subtype.NINJA))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, t)
        description = "{2}: Target Ninja creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "37"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/743f3cf5-f8aa-49d4-947d-76b91799547a.jpg?1783944207"
    }
}
