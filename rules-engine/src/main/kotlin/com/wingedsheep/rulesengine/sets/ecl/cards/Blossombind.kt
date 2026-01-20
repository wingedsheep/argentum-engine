package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.OnEnterBattlefield
import com.wingedsheep.rulesengine.ability.TapUntapEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Blossombind
 *
 * {1}{U} Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature can't become untapped and can't have counters put on it.
 */
object Blossombind {
    val definition = CardDefinition.enchantment(
        name = "Blossombind",
        manaCost = ManaCost.parse("{1}{U}"),
        subtypes = setOf(Subtype.AURA),
        oracleText = "Enchant creature\nWhen this Aura enters, tap enchanted creature.\n" +
                "Enchanted creature can't become untapped and can't have counters put on it.",
        metadata = ScryfallMetadata(
            collectorNumber = "45",
            rarity = Rarity.COMMON,
            artist = "Drew Tucker",
            imageUri = "https://cards.scryfall.io/normal/front/e/e/ee9e9e9e-9e9e-9e9e-9e9e-9e9e9e9e9e9e.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Blossombind") {
        // Enchant target creature
        targets(TargetCreature())

        // ETB: Tap enchanted creature
        triggered(
            trigger = OnEnterBattlefield(),
            effect = TapUntapEffect(
                target = EffectTarget.EnchantedCreature,
                tap = true
            )
        )

        // Static: Enchanted creature can't untap
        // TODO: CantReceiveCountersEffect needs additional infrastructure
        grantKeyword(Keyword.CANT_UNTAP)
    }
}
