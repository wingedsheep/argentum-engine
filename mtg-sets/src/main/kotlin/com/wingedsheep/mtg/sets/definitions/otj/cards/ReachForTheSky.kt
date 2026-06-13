package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Reach for the Sky
 * {3}{G}
 * Enchantment — Aura
 * Flash
 * Enchant creature
 * Enchanted creature gets +3/+2 and has reach.
 * When this Aura is put into a graveyard from the battlefield, draw a card.
 *
 * Armadillo Cloak's aura shape (static [ModifyStats] + static [GrantKeyword]) with a flash keyword
 * and a self [Triggers.PutIntoGraveyardFromBattlefield] draw trigger (same event as Nutrient Block's
 * "put into a graveyard from the battlefield" replacement of a dies trigger for a non-creature).
 */
val ReachForTheSky = card("Reach for the Sky") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant creature\n" +
        "Enchanted creature gets +3/+2 and has reach.\n" +
        "When this Aura is put into a graveyard from the battlefield, draw a card."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(3, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.REACH)
    }

    triggeredAbility {
        trigger = Triggers.PutIntoGraveyardFromBattlefield
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Villarrte"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb871985-a11b-4dfe-b0e3-898888c86277.jpg?1712355983"
    }
}
