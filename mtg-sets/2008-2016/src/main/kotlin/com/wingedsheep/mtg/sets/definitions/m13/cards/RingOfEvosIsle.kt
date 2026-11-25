package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ring of Evos Isle
 * {2}
 * Artifact — Equipment
 * {2}: Equipped creature gains hexproof until end of turn.
 * At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's blue.
 * Equip {1}
 *
 * Unlike its cycle-mates, the keyword is not a static grant: the Equipment's own {2} activated
 * ability gives whatever creature it equips on resolution hexproof until end of turn (it may be
 * activated while unattached; it then does nothing). The upkeep trigger mirrors Ring of Kalonia —
 * "if it's blue" is checked only on resolution, against the equipped creature's projected colours.
 */
val RingOfEvosIsle = card("Ring of Evos Isle") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "{2}: Equipped creature gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)\n" +
        "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's blue.\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.EquippedCreature)
        description = "{2}: Equipped creature gains hexproof until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                EffectTarget.EquippedCreature,
                GameObjectFilter.Creature.withColor(Color.BLUE),
            ),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EquippedCreature),
        )
        description = "At the beginning of your upkeep, put a +1/+1 counter on equipped creature if it's blue."
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Erica Yang"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7c740a8-1bbc-4ec8-a72c-01aee9e48f3d.jpg?1783940459"
    }
}
