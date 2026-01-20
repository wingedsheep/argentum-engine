package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModifyStatsEffect
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.targeting.CreatureTargetFilter
import com.wingedsheep.rulesengine.targeting.TargetCreature

/**
 * Safewright Cavalry
 *
 * {3}{G} Creature — Elf Warrior 4/4
 * This creature can't be blocked by more than one creature.
 * {5}: Target Elf you control gets +2/+2 until end of turn.
 */
object SafewrightCavalry {
    val definition = CardDefinition.creature(
        name = "Safewright Cavalry",
        manaCost = ManaCost.parse("{3}{G}"),
        subtypes = setOf(Subtype.ELF, Subtype.WARRIOR),
        power = 4,
        toughness = 4,
        oracleText = "This creature can't be blocked by more than one creature.\n{5}: Target Elf you control gets +2/+2 until end of turn.",
        metadata = ScryfallMetadata(
            collectorNumber = "191",
            rarity = Rarity.COMMON,
            artist = "Milivoj Ćeran",
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bbcc2345-6789-0123-defg-bbcc23456789.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Safewright Cavalry") {
        // TODO: "Can't be blocked by more than one creature" needs evasion infrastructure

        // Pump an Elf target
        val elfTarget = targets(
            TargetCreature(
                filter = CreatureTargetFilter.And(
                    listOf(
                        CreatureTargetFilter.YouControl,
                        CreatureTargetFilter.WithSubtype(Subtype.ELF)
                    )
                )
            )
        )

        // {5}: Target Elf you control gets +2/+2 until end of turn
        activated(
            cost = AbilityCost.Mana(generic = 5),
            effect = ModifyStatsEffect(
                powerModifier = 2,
                toughnessModifier = 2,
                target = EffectTarget.ContextTarget(elfTarget.index),
                untilEndOfTurn = true
            ),
            timing = TimingRestriction.INSTANT
        )
    }
}
