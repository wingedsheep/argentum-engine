package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Doomwake Giant
 * {4}{B}
 * Enchantment Creature — Giant
 * 4/6
 *
 * Constellation — Whenever this creature or another enchantment you control enters, creatures your
 * opponents control get -1/-1 until end of turn.
 *
 * Eidolon of Blossoms' constellation trigger (an ANY-bound enchantment-you-control enters, which
 * includes this enchantment creature itself) driving Night Clubber's one-shot -1/-1 sweep: the
 * affected creatures are locked in as the trigger resolves, so a creature an opponent gains later
 * that turn isn't shrunk.
 */
val DoomwakeGiant = card("Doomwake Giant") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Giant"
    power = 4
    toughness = 6
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, " +
        "creatures your opponents control get -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity),
        )
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "creatures your opponents control get -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Kev Walker"
        flavorText = "Crippling fear precedes it. A foul stench accompanies it. Death follows in its wake."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b20eabfb-f086-45dd-97e2-b2ed5286bc3c.jpg?1783939437"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
