package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ria Ivor, Bane of Bladehold
 * {2}{W}{B}
 * Legendary Creature — Phyrexian Knight
 * 3/4
 * Battle cry
 * At the beginning of combat on your turn, the next time target creature would deal combat damage
 * to one or more players this combat, prevent that damage. If damage is prevented this way, create
 * that many 1/1 colorless Phyrexian Mite artifact creature tokens with toxic 1 and "This token
 * can't block."
 */
val RiaIvorBaneOfBladehold = card("Ria Ivor, Bane of Bladehold") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Phyrexian Knight"
    power = 3
    toughness = 4
    oracleText = "Battle cry (Whenever this creature attacks, each other attacking creature gets +1/+0 until end of turn.)\n" +
        "At the beginning of combat on your turn, the next time target creature would deal combat damage to one or more " +
        "players this combat, prevent that damage. If damage is prevented this way, create that many 1/1 colorless " +
        "Phyrexian Mite artifact creature tokens with toxic 1 and \"This token can't block.\""

    // Battle cry: whenever this creature attacks, each other attacking creature gets +1/+0 UEOT.
    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                baseFilter = GameObjectFilter.Creature.attacking(),
                excludeSelf = true,
            ),
            Effects.ModifyStats(1, 0, EffectTarget.IterationEntity),
        )
    }

    triggeredAbility {
        val creature = target(TargetFilter.Creature)
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.PreventDamage(
            target = creature,
            direction = PreventionDirection.FromTarget,
            combatOnly = true,
            toPlayersOnly = true,
            nextInstanceOnly = true,
            onPrevented = Effects.CreatePhyrexianMite(DynamicAmounts.preventedDamage()),
            duration = Duration.EndOfCombat,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7531ed3-235f-4368-98a8-e4e1947c53fb.jpg?1783917997"
    }
}
