package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.targeting.CreatureTargetFilter
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Aquitect's Defenses
 *
 * {1}{U} Enchantment — Aura
 * Flash
 * Enchant creature you control
 * When this Aura enters, enchanted creature gains hexproof until end of turn.
 * Enchanted creature gets +1/+2.
 */
object AquitectsDefenses {
    val definition = CardDefinition.enchantment(
        name = "Aquitect's Defenses",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.AURA),
        oracleText = "Flash\nEnchant creature you control\nWhen this Aura enters, enchanted creature gains " +
                "hexproof until end of turn.\nEnchanted creature gets +1/+2.",
        metadata = ScryfallMetadata(
            collectorNumber = "44",
            rarity = Rarity.COMMON,
            artist = "Ioannis Fiore",
            imageUri = "https://cards.scryfall.io/normal/front/d/d/dd8d8d8d-8d8d-8d8d-8d8d-8d8d8d8d8d8d.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Aquitect's Defenses") {
        keywords(Keyword.FLASH)

        // Enchant target creature you control
        targets(TargetCreature(filter = CreatureTargetFilter.YouControl))

        // Aura grants +1/+2 to enchanted creature (static ability)
        modifyStats(power = 1, toughness = 2)

        // ETB: Grant hexproof until end of turn
        triggered(
            trigger = OnEnterBattlefield(),
            effect = GrantKeywordUntilEndOfTurnEffect(
                keyword = Keyword.HEXPROOF,
                target = EffectTarget.EnchantedCreature
            )
        )
    }
}
