package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Supplant Form
 * {4}{U}{U}
 * Instant
 * Return target creature to its owner's hand. You create a token that's a copy of that creature.
 *
 * "That creature" is the permanent that was just returned, so the token copies its copiable values
 * as it last existed on the battlefield (CR 608.2h, CR 707.2): `CreateTokenCopyOfTarget` reads the
 * departed target's battlefield-exit snapshot rather than the card now in hand. A creature that was
 * copying something is copied as what it was copying, a face-down creature yields a face-up
 * nameless 2/2 with no abilities, and counters, Auras and pumps are not copied. If the target is
 * illegal on resolution the whole spell does nothing, so no token is created.
 */
val SupplantForm = card("Supplant Form") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. You create a token that's a copy of that creature."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ReturnToHand(creature) then Effects.CreateTokenCopyOfTarget(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Adam Paquette"
        flavorText = "\"Ice can be shaped to any form, even the whisper of a memory.\"\n—Mytha, Temur shaman"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3df927c0-9aa9-450b-ab2a-ae12c5489d57.jpg?1783938701"
        ruling(
            "2014-11-24",
            "If you return a face-down creature to its owner's hand, the token will be a face-up colorless " +
                "2/2 creature with no name, no abilities, and no creature types."
        )
        ruling(
            "2014-11-24",
            "If the copied creature was copying something else, the token enters the battlefield as whatever " +
                "that creature was copying."
        )
        ruling(
            "2014-11-24",
            "The token copies exactly what was printed on the creature and nothing else. It doesn't copy " +
                "whether that creature was tapped or untapped, whether it had any counters on it or Auras " +
                "and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, " +
                "types, color, and so on."
        )
    }
}
