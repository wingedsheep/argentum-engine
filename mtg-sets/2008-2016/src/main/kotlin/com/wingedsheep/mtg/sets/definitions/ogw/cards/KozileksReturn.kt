package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kozilek's Return
 * {2}{R}
 * Instant
 * Devoid (This card has no color.)
 * Kozilek's Return deals 2 damage to each creature.
 * Whenever you cast an Eldrazi creature spell with mana value 7 or greater, you may exile this
 * card from your graveyard. If you do, this card deals 5 damage to each creature.
 *
 * The recursion ability functions only from the graveyard (`triggerZone = GRAVEYARD`), and it
 * resolves before the Eldrazi spell that triggered it, so that creature isn't on the battlefield
 * to be dealt damage. The exile reads `fromZone = GRAVEYARD`: if the card has already left the
 * graveyard when the trigger resolves, nothing is exiled and the "if you do" damage doesn't
 * happen. The damage keeps the card itself as its source.
 */
val KozileksReturn = card("Kozilek's Return") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Kozilek's Return deals 2 damage to each creature.\n" +
        "Whenever you cast an Eldrazi creature spell with mana value 7 or greater, you may exile " +
        "this card from your graveyard. If you do, this card deals 5 damage to each creature."

    keywords(Keyword.DEVOID)

    spell {
        effect = Patterns.Group.dealDamageToAll(2, GroupFilter.AllCreatures)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withSubtype("Eldrazi").manaValueAtLeast(7))
        triggerZone = Zone.GRAVEYARD
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Move(EffectTarget.Self, Zone.EXILE, fromZone = Zone.GRAVEYARD),
                then = Patterns.Group.dealDamageToAll(5, GroupFilter.AllCreatures)
            )
        )
        description = "Whenever you cast an Eldrazi creature spell with mana value 7 or greater, you may " +
            "exile this card from your graveyard. If you do, this card deals 5 damage to each creature."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "98"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72765559-0a78-4aa3-827e-cb4612720991.jpg?1783937909"

        ruling(
            "2016-01-22",
            "The last ability triggers only if Kozilek's Return is in your graveyard as you cast the " +
                "Eldrazi creature spell. That ability will resolve before the Eldrazi creature spell does. " +
                "In other words, that Eldrazi won't be on the battlefield yet and won't be dealt damage by " +
                "Kozilek's Return."
        )
        ruling(
            "2016-01-22",
            "Exiling Kozilek's Return due to the last ability isn't the same as casting it as a spell. " +
                "Cards that say \"Counter target spell\" won't work."
        )
    }
}
