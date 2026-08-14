package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Geier Reach Bandit // Vildin-Pack Alpha (Shadows over Innistrad — the card's earliest
 * printing; also reprinted in Shadows over Innistrad Remastered and Innistrad Remastered)
 *
 * Front — Geier Reach Bandit ({2}{R}, Creature — Human Rogue Werewolf, 3/2)
 *   Haste
 *   At the beginning of each upkeep, if no spells were cast last turn, transform this creature.
 *
 * Back — Vildin-Pack Alpha (Creature — Werewolf, 4/3)
 *   Whenever a Werewolf you control enters, you may transform it.
 *   At the beginning of each upkeep, if a player cast two or more spells last turn, transform
 *   this creature.
 *
 * Implementation:
 *  - Both upkeep flips are the standard Werewolf pair: [Triggers.EachUpkeep] with an
 *    intervening-if on [DynamicAmounts.spellsCastLastTurn] (== 0 front, >= 2 back).
 *  - The back's ETB watcher is [Triggers.entersBattlefield] with [TriggerBinding.ANY] over
 *    "Werewolf you control" — Oracle says *a* Werewolf, not *another*, so Vildin-Pack Alpha
 *    entering (e.g. put onto the battlefield transformed) triggers it on itself too. The
 *    optional flip is [MayEffect] over a [TransformEffect] aimed at
 *    [EffectTarget.TriggeringEntity], so it flips the permanent that entered rather than the
 *    Alpha.
 */

private val GeierReachBanditFront = card("Geier Reach Bandit") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Rogue Werewolf"
    power = 3
    toughness = 2
    oracleText = "Haste\n" +
        "At the beginning of each upkeep, if no spells were cast last turn, transform this creature."

    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.EQ, DynamicAmount.Fixed(0)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Slawomir Maniak"
        flavorText = "The cathars realized they had not tracked her—she had led them here."
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4570117d-c04b-4bdc-b804-21d49154721b.jpg?1783937760"
    }
}

private val VildinPackAlpha = card("Vildin-Pack Alpha") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Werewolf"
    power = 4
    toughness = 3
    oracleText = "Whenever a Werewolf you control enters, you may transform it.\n" +
        "At the beginning of each upkeep, if a player cast two or more spells last turn, " +
        "transform this creature."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype("Werewolf").youControl(),
            binding = TriggerBinding.ANY,
        )
        effect = MayEffect(effect = TransformEffect(EffectTarget.TriggeringEntity))
        description = "Whenever a Werewolf you control enters, you may transform it."
    }

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        triggerCondition = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastLastTurn(), ComparisonOperator.GTE, DynamicAmount.Fixed(2)
        )
        effect = TransformEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/back/4/5/4570117d-c04b-4bdc-b804-21d49154721b.jpg?1783937760"
    }
}

val GeierReachBandit: CardDefinition =
    CardDefinition.doubleFacedCreature(GeierReachBanditFront, VildinPackAlpha)
