package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Nyxborn Triton — a 2/3 on its own body; bestowed, the Aura's static hands the host +2/+3.
 */
val NyxbornTriton = card("Nyxborn Triton") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Merfolk"
    power = 2
    toughness = 3
    oracleText = "Bestow {4}{U} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Enchanted creature gets +2/+3."

    keywordAbility(KeywordAbility.bestow("{4}{U}"))

    staticAbility { ability = ModifyStats(2, 3) }

    metadata {
        ruling("2013-09-15", "On the stack, a spell with bestow is either a creature spell or an Aura spell. It's never both, although it's an enchantment spell in either case.")
        ruling("2013-09-15", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2013-09-15", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2013-09-15", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2013-09-15", "Auras attached to a creature don't become tapped when the creature becomes tapped. Except in some rare cases, an Aura with bestow remains untapped when it becomes unattached and becomes a creature.")
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65371de6-13a2-401f-9334-ae9e19619487.jpg?1783939566"
    }
}
