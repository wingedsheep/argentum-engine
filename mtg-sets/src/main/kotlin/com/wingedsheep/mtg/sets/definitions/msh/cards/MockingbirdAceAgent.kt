package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mockingbird, Ace Agent — Marvel Super Heroes #22 (uncommon)
 * {3}{W} · Legendary Creature — Human Spy Hero · 2/2
 *
 * Double strike
 * Whenever you cast a spell that targets a creature you control, put a +1/+1 counter on
 * Mockingbird.
 *
 * The trigger is the existing [Triggers.youCastSpellTargeting] facade — a `SpellCastEvent` with
 * `SpellCastPredicate.TargetsMatching(Creature.youControl())`, evaluated against the spell's
 * chosen targets relative to Mockingbird's controller. It fires once per qualifying spell no
 * matter how many of your creatures it targets, and Mockingbird itself counts as "a creature you
 * control", so a spell aimed at her triggers it too.
 */
val MockingbirdAceAgent = card("Mockingbird, Ace Agent") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Spy Hero"
    power = 2
    toughness = 2
    oracleText = "Double strike\n" +
        "Whenever you cast a spell that targets a creature you control, put a +1/+1 counter on " +
        "Mockingbird."

    keywords(Keyword.DOUBLE_STRIKE)

    triggeredAbility {
        trigger = Triggers.youCastSpellTargeting(GameObjectFilter.Creature.youControl())
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast a spell that targets a creature you control, put a " +
            "+1/+1 counter on Mockingbird."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Sveta Pikul"
        flavorText = "\"My first love was lab work, but these days my heart's in the field.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f5701ac-ec30-4fb1-bd71-4bac4693c075.jpg?1783902972"
    }
}
