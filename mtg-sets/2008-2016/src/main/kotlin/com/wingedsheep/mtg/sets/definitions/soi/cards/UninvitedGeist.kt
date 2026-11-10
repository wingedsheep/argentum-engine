package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Uninvited Geist // Unimpeded Trespasser (Shadows over Innistrad #94)
 * {2}{U}
 * Creature — Spirit 2/2 // Creature — Spirit 3/3
 *
 * Front — "Skulk / When this creature deals combat damage to a player, transform it."
 * Back  — "This creature can't be blocked."
 *
 * The back face has no mana cost, so its blue comes from a color indicator (CR 204).
 */

private val UninvitedGeistFront = card("Uninvited Geist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Skulk (This creature can't be blocked by creatures with greater power.)\n" +
        "When this creature deals combat damage to a player, transform it."

    keywords(Keyword.SKULK)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Transform(EffectTarget.Self)
        description = "When this creature deals combat damage to a player, transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Dan Murayama Scott"
        flavorText = "A slow knock at midnight is best left unanswered."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dea7c9f-ce66-447b-9f1b-d9bca5806153.jpg?1783937790"
        ruling(
            "2016-04-08",
            "Skulk matters only as blockers are chosen. Modifying either creature's power after " +
                "blockers are chosen won't cause the attacking creature to become unblocked."
        )
        ruling(
            "2016-04-08",
            "If you cause a creature to have 0 power or less, use the actual value (which may be " +
                "negative) to determine whether it can block or be blocked. A creature with skulk and 0 " +
                "or less power most likely won't be blocked, but it won't deal combat damage and won't " +
                "trigger any abilities that trigger when combat damage is dealt."
        )
    }
}

private val UnimpededTrespasser = card("Unimpeded Trespasser") {
    manaCost = ""
    colorIdentity = "U"
    colorIndicator = "U" // Transformed back face, no mana cost (CR 204).
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked."

    staticAbility {
        ability = CantBeBlocked()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Dan Murayama Scott"
        flavorText = "Knocking was simply a formality."
        imageUri = "https://cards.scryfall.io/normal/back/4/d/4dea7c9f-ce66-447b-9f1b-d9bca5806153.jpg?1783937790"
    }
}

val UninvitedGeist: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = UninvitedGeistFront,
    backFace = UnimpededTrespasser,
)
