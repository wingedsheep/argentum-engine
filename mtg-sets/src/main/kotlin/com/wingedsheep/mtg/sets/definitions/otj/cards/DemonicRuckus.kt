package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Demonic Ruckus
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+1 and has menace and trample.
 * When this Aura is put into a graveyard from the battlefield, draw a card.
 * Plot {R}
 *
 * Reach for the Sky's aura shape (static [ModifyStats] + per-keyword static [GrantKeyword]) with a
 * self [Triggers.PutIntoGraveyardFromBattlefield] draw trigger. Plot is the standard
 * [KeywordAbility.plot] exile-and-cast-later mechanic.
 */
val DemonicRuckus = card("Demonic Ruckus") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+1 and has menace and trample.\n" +
        "When this Aura is put into a graveyard from the battlefield, draw a card.\n" +
        "Plot {R} (You may pay {R} and exile this card from your hand. Cast it as a sorcery on a " +
        "later turn without paying its mana cost. Plot only as a sorcery.)"

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Effects.DrawCards(1)
    }

    keywordAbility(KeywordAbility.plot("{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b491d11-d00a-4541-8389-2785a455eeee.jpg?1712355738"
    }
}
