package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Farrel's Zealot
 * {1}{W}{W}
 * Creature — Human
 * 2/2
 * Whenever this creature attacks and isn't blocked, you may have it deal 3 damage to target
 * creature. If you do, this creature assigns no combat damage this turn.
 *
 * "Assigns no combat damage" is not prevention: the Zealot assigns nothing at all, so no damage
 * event happens and nothing downstream — a damage trigger, lifelink, trample — has anything to
 * work with. `AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE`, granted until end of turn, is read at the
 * engine's single combat-damage assignment chokepoint.
 */
val FarrelsZealot = card("Farrel's Zealot") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human"
    oracleText = "Whenever this creature attacks and isn't blocked, you may have it deal 3 damage " +
        "to target creature. If you do, this creature assigns no combat damage this turn."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.AttacksAndIsntBlocked
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = MayEffect(
            Effects.Composite(
                Effects.DealDamage(3, t),
                GrantKeywordEffect(
                    AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE.name,
                    EffectTarget.Self,
                    Duration.EndOfTurn,
                ),
            ),
            descriptionOverride = "have this creature deal 3 damage to that creature. If you do, it assigns no combat damage this turn",
        )
        description = "Whenever this creature attacks and isn't blocked, you may have it deal 3 damage to target creature. If you do, this creature assigns no combat damage this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3a"
        artist = "Melissa A. Benson"
        flavorText = "After the fall of Trokair, Farrel and his followers formally broke their ties with the rest of Icatia."
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0401bd23-9f81-40b7-a6c2-e3f9847d175c.jpg?1783947921"
    }
}
