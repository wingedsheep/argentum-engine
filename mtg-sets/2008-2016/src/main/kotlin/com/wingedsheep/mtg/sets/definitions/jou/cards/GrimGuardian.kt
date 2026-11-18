package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Guardian
 * {2}{B}
 * Enchantment Creature — Zombie
 * 1/4
 *
 * Constellation — Whenever this creature or another enchantment you control enters, each opponent
 * loses 1 life.
 *
 * The Guardian is itself an enchantment, so one unbound watcher over enchantments you control
 * (`Triggers.a`) covers its own entry too (Eidolon of Blossoms' shape).
 */
val GrimGuardian = card("Grim Guardian") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Zombie"
    power = 1
    toughness = 4
    oracleText = "Constellation — Whenever this creature or another enchantment you control enters, each " +
        "opponent loses 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Constellation — Whenever this creature or another enchantment you control enters, " +
            "each opponent loses 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Ryan Barger"
        flavorText = "Occasionally the living wander to the Rivers, but the wardens of Athreos ensure that " +
            "only the dead pass."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/732b5444-0410-430b-87ca-cf2d9f165bd7.jpg?1783939434"
        ruling(
            "2014-04-26",
            "A constellation ability triggers whenever an enchantment enters the battlefield under your " +
                "control for any reason. Enchantments with other card types, such as enchantment creatures, " +
                "will also cause constellation abilities to trigger."
        )
    }
}
