package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Obyra's Attendants // Desperate Parry
 * {4}{U}
 * Creature — Faerie Wizard
 * 3/4
 *
 * Flying
 *
 * Adventure: Desperate Parry — {1}{U}, Instant — Adventure
 * Target creature gets -4/-0 until end of turn.
 *
 * The Adventure is a pure combat trick: [Effects.ModifyStats] with a negative power delta and no
 * toughness change, so a blocked attacker deals (almost) nothing but still survives. Toughness is
 * untouched — `-4/-0` never kills on its own. (CR 715: Adventure cards. Casting the Adventure
 * exiles the card on resolution and lets the caster cast it as the creature spell while it remains
 * in exile.)
 */
val ObyrasAttendants = card("Obyra's Attendants") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Wizard"
    oracleText = "Flying"
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    adventure("Desperate Parry") {
        manaCost = "{1}{U}"
        typeLine = "Instant — Adventure"
        oracleText = "Target creature gets -4/-0 until end of turn. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target", Targets.Creature)
            effect = Effects.ModifyStats(-4, 0, t)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Andreas Zafiratos"
        flavorText = "Obyra's devoted servants shrieked as their sleeping mistress slashed at them, unseeing."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0001e77a-7fff-49d2-a55c-42f6fdf6db08.jpg?1783915116"
    }
}
