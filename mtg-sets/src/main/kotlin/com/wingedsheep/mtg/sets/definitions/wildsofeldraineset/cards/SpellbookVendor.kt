package com.wingedsheep.mtg.sets.definitions.wildsofeldraineset.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spellbook Vendor
 * {1}{W}
 * Creature — Human Advisor
 * 1/3
 *
 * When Spellbook Vendor enters the battlefield, create a Sorcerer Role token attached to
 * target creature you control. (The token is an enchantment with "Enchanted creature gets
 * +1/+1" and "Whenever enchanted creature attacks, scry 1.")
 */
val SpellbookVendor = card("Spellbook Vendor") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 3
    oracleText = "When Spellbook Vendor enters the battlefield, create a Sorcerer Role token attached to target creature you control. (The token is an enchantment with \"Enchanted creature gets +1/+1\" and \"Whenever enchanted creature attacks, scry 1.\")"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val targetCreature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateRoleToken("Sorcerer Role", targetCreature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Paolo Parente"
        flavorText = "\"Don't let the size of the shop fool you. I stock everything.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33a34446-dd04-4e32-b4de-c1c5c0a9408f.jpg?1696288191"
    }
}
