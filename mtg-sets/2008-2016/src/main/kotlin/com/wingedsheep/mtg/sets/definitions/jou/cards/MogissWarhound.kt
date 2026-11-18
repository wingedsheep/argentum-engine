package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mogis's Warhound — "attacks each combat if able" twice: on its own body while it's a creature, and
 * on the creature it enchants once bestowed (read off the Aura through `GroupFilter.attachedCreature()`).
 */
val MogissWarhound = card("Mogis's Warhound") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "Bestow {2}{R} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature attacks each combat if able.\n" +
        "Enchanted creature gets +2/+2 and attacks each combat if able."

    keywordAbility(KeywordAbility.bestow("{2}{R}"))

    staticAbility { ability = MustAttack() }
    staticAbility { ability = ModifyStats(2, 2) }
    staticAbility { ability = MustAttack(GroupFilter.attachedCreature()) }

    metadata {
        ruling("2014-04-26", "The controller of Mogis's Warhound or the creature it's enchanting chooses which player or planeswalker to attack.")
        ruling("2014-04-26", "If, during your declare attackers step, Mogis's Warhound (or the creature it enchants) is tapped, is affected by a spell or ability that says it can't attack, or hasn't been under its controller's control continuously since the turn began (and doesn't have haste), then it doesn't attack. If there's a cost associated with having a creature attack, its controller isn't forced to pay that cost, so it doesn't have to attack in that case either.")
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2014-04-26", "You still control the Aura, even if it's enchanting a creature controlled by another player.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2e73e06-d708-41dc-b20c-1f71651230e1.jpg?1783939422"
    }
}
