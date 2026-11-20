package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Boon Satyr — a 4/2 flash creature on its own; bestowed (also at instant speed, since flash
 * applies to the whole card), the Aura's static hands the host +4/+2.
 */
val BoonSatyr = card("Boon Satyr") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Satyr"
    power = 4
    toughness = 2
    oracleText = "Flash\n" +
        "Bestow {3}{G}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Enchanted creature gets +4/+2."

    keywords(Keyword.FLASH)
    keywordAbility(KeywordAbility.bestow("{3}{G}{G}"))

    staticAbility { ability = ModifyStats(4, 2) }

    metadata {
        ruling("2018-07-13", "Bestow is an alternative cost to cast the spell with bestow. It can't be combined with other alternative costs, such as casting a spell “without paying its mana cost.”")
        ruling("2018-07-13", "To determine the total cost of a spell, start with the mana cost or alternative cost (such as a bestow cost) you're paying, add any cost increases, then apply any cost reductions. The mana value of the spell remains unchanged, no matter what the total cost to cast it was.")
        ruling("2018-07-13", "A spell with bestow is either a creature spell or an Aura spell. It's never both. Similarly, a permanent with bestow is either a creature or an Aura, but not both.")
        ruling("2018-07-13", "Unlike other Aura spells, an Aura spell with bestow still resolves if its target is illegal. Instead of entering the battlefield as an Aura, the effect making it an Aura spell ends, it loses enchant creature, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2018-07-13", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if the enchanted creature leaves the battlefield or becomes an illegal creature for the Aura to enchant. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2018-07-13", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2018-07-13", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Wesley Burt"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f50c12c4-5565-449a-b30e-7cd896da7735.jpg?1783939748"
    }
}
