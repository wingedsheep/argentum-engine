package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.MustBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spirespine — "blocks each combat if able" twice: on its own body while it's a creature, and on
 * the creature it enchants once bestowed (read off the Aura through `GroupFilter.attachedCreature()`).
 */
val Spirespine = card("Spirespine") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Beast"
    power = 4
    toughness = 1
    oracleText = "Bestow {4}{G} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature blocks each combat if able.\n" +
        "Enchanted creature gets +4/+1 and blocks each combat if able."

    keywordAbility(KeywordAbility.bestow("{4}{G}"))

    staticAbility { ability = MustBlock() }
    staticAbility { ability = ModifyStats(4, 1) }
    staticAbility { ability = MustBlock(GroupFilter.attachedCreature()) }

    metadata {
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2014-04-26", "You still control the Aura, even if it's enchanting a creature controlled by another player.")
        ruling("2014-04-26", "If the enchanted creature leaves the battlefield, the Aura stops being an Aura and remains on the battlefield. Control of that permanent doesn't change; you'll control the resulting enchantment creature.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "142"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac71491f-3027-4257-a18f-ba4de6041feb.jpg?1783939407"
    }
}
