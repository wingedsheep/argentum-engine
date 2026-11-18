package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gnarled Scarhide — "can't block" twice: on its own body while it's a creature, and on the creature
 * it enchants once bestowed (read off the Aura through `GroupFilter.attachedCreature()`).
 */
val GnarledScarhide = card("Gnarled Scarhide") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Minotaur"
    power = 2
    toughness = 1
    oracleText = "Bestow {3}{B} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "This creature can't block.\n" +
        "Enchanted creature gets +2/+1 and can't block."

    keywordAbility(KeywordAbility.bestow("{3}{B}"))

    staticAbility { ability = CantBlock() }
    staticAbility { ability = ModifyStats(2, 1) }
    staticAbility { ability = CantBlock(filter = GroupFilter.attachedCreature()) }

    metadata {
        ruling("2014-04-26", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature.")
        ruling("2014-04-26", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        ruling("2014-04-26", "You still control the Aura, even if it's enchanting a creature controlled by another player.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/040af9b9-1e94-4dd1-b347-b54b6b9e0275.jpg?1783939434"
    }
}
