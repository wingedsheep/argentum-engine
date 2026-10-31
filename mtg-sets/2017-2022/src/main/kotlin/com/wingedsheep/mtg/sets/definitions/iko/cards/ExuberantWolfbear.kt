package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Exuberant Wolfbear — Ikoria: Lair of Behemoths #151
 * {3}{G} · Creature — Wolf Bear · 4/4
 *
 * Whenever this creature attacks, you may have the base power and toughness of target Human you
 * control become equal to this creature's power and toughness until end of turn.
 *
 * The Wolfbear's *actual* (projected) power and toughness are read once, as the ability resolves,
 * and set as the Human's layer-7b base values; later changes to the Wolfbear don't follow. If the
 * Wolfbear has left the battlefield, the source reads use its last-known information. The "may"
 * is folded into the optional target: choosing no Human declines.
 */
val ExuberantWolfbear = card("Exuberant Wolfbear") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wolf Bear"
    power = 4
    toughness = 4
    oracleText = "Whenever this creature attacks, you may have the base power and toughness of target " +
        "Human you control become equal to this creature's power and toughness until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        optional = true
        val human = target(TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN).youControl()))
        effect = Effects.SetBasePowerAndToughness(
            power = DynamicAmounts.sourcePower(),
            toughness = DynamicAmounts.sourceToughness(),
            target = human,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Jesper Ejsing"
        flavorText = "Sometimes the *eludha* is a sacred harmony of souls. Sometimes the bonder grabs " +
            "some fur and holds on tight."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c8ff0da-fbe3-4c95-bc39-c975c55b6aea.jpg?1783931038"
        ruling("2020-04-17", "The Human's new base power and toughness are set to Exuberant Wolfbear's actual power and toughness, not just its base power and toughness. If Exuberant Wolfbear's power or toughness changes later in the turn, the Human isn't affected.")
        ruling("2020-04-17", "If Exuberant Wolfbear leaves the battlefield while its triggered ability is on the stack, use its power and toughness as it last existed on the battlefield to determine the Human's new base power and toughness.")
        ruling("2020-04-17", "Any effects that modify the Human's power and/or toughness without setting them to a specific number or value will apply after its base power and toughness are set, regardless of the order in which those effects were created. The same is true of counters that modify its power and toughness.")
    }
}
