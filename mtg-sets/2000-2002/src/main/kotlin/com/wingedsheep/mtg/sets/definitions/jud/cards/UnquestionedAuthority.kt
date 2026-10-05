package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantProtectionFromCardType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unquestioned Authority
 * {2}{W}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, draw a card.
 * Enchanted creature has protection from creatures.
 *
 * "Protection from creatures" is protection from the creature card type: the projected
 * `PROTECTION_FROM_CARDTYPE_CREATURE` keyword is read by blocking, damage and targeting alike.
 */
val UnquestionedAuthority = card("Unquestioned Authority") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted creature has protection from creatures."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = GrantProtectionFromCardType(CardType.CREATURE, Filters.EnchantedCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"Only the Ancestor should be revered.\"\n—Mystic heretic"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a015205e-5895-4038-9c2f-ed4766c498ff.jpg?1783945132"
    }
}
