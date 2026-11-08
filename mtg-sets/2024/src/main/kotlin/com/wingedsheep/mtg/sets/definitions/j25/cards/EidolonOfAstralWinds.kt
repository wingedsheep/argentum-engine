package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eidolon of Astral Winds — Foundations Jumpstart #2
 * {2}{W} · Enchantment Creature — Spirit · 2/4
 *
 * Vigilance
 * Constellation — Whenever this creature or another enchantment you control enters, choose target
 * creature you control. Until end of turn, that creature has base power and toughness 4/4 and
 * gains flying.
 *
 * The Eidolon is itself an enchantment, so "this creature or another enchantment you control" is
 * one watcher over enchantments you control (Eidolon of Blossoms' shape). The payoff is
 * Creeperhulk's: `SetBasePowerAndToughness` sets base P/T in layer 7b, so counters and pumps still
 * apply on top, then flying is granted until end of turn — one target, two effects.
 */
val EidolonOfAstralWinds = card("Eidolon of Astral Winds") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Spirit"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n" +
        "Constellation — Whenever this creature or another enchantment you control enters, choose target " +
        "creature you control. Until end of turn, that creature has base power and toughness 4/4 and gains flying."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.SetBasePowerAndToughness(4, 4, creature) then
            Effects.GrantKeyword(Keyword.FLYING, creature)
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "target creature you control has base power and toughness 4/4 and gains flying until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd62f1de-853e-4fc3-9137-8f419c5972f6.jpg?1783908870"
        ruling(
            "2024-11-08",
            "If Eidolon of Astral Winds enters at the same time as one or more other enchantments you " +
                "control, its last ability will trigger for each of those enchantments, including itself."
        )
        ruling(
            "2024-11-08",
            "The effect of Eidolon of Astral Winds's last ability will overwrite any previous effects that set " +
                "the creature's power and toughness to specific values. Effects that otherwise modify the target " +
                "creature's power and toughness will still apply no matter when they took effect. The same is " +
                "true for +1/+1 counters."
        )
    }
}
