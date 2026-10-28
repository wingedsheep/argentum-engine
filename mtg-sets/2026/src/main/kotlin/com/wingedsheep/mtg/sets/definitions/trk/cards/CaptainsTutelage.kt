package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Captain's Tutelage
 * {1}{R}
 * Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * When this Aura enters, enchanted creature gains first strike until end of turn.
 * Enchanted creature gets +2/+0.
 */
val CaptainsTutelage = card("Captain's Tutelage") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    colorIdentity = "R"
    oracleText = "Flash\nEnchant creature\nWhen this Aura enters, enchanted creature gains first strike until end of turn.\nEnchanted creature gets +2/+0."
    keywords(Keyword.FLASH)
    auraTarget = TargetObject(filter = TargetFilter.Creature, id = "target")
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.EnchantedCreature)
    }
    staticAbility { ability = ModifyStats(2, 0) }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Mark Spears"
        flavorText = "Starfleet tactical training demands cadets learn how to handle any situation—even failure."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c340eba-645d-416a-9ed4-f2303aa8c919.jpg?1785981528"
    }
}
