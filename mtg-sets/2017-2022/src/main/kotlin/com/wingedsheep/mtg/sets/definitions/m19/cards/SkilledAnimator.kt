package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skilled Animator
 * {2}{U}
 * Creature — Human Artificer
 * 1/3
 *
 * When this creature enters, target artifact you control becomes an artifact creature with base
 * power and toughness 5/5 for as long as this creature remains on the battlefield.
 *
 * The animate adds the creature type (Layer 4) and sets base P/T 5/5 (Layer 7b) without removing
 * abilities, types, or subtypes (per the 2020-11-10 rulings). The source-keyed
 * [Duration.WhileSourceOnBattlefield] ends the effect the moment the Animator leaves, and means it
 * never applies if the Animator is already gone when the trigger resolves.
 */
val SkilledAnimator = card("Skilled Animator") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, target artifact you control becomes an artifact creature " +
        "with base power and toughness 5/5 for as long as this creature remains on the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val artifact = target(TargetFilter.Artifact.youControl())
        effect = Effects.BecomeCreature(
            target = artifact,
            power = 5,
            toughness = 5,
            duration = Duration.WhileSourceOnBattlefield("Skilled Animator")
        )
        description = "When this creature enters, target artifact you control becomes an artifact " +
            "creature with base power and toughness 5/5 for as long as this creature remains on the " +
            "battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/586d5000-1fdb-4bfe-aa34-63b25ee94bb9.jpg?1783934583"
        ruling("2020-11-10", "Skilled Animator doesn't remove any abilities the target artifact has.")
        ruling("2020-11-10", "The artifact retains any types, subtypes, or supertypes it has.")
        ruling("2020-11-10", "If an Equipment becomes an artifact creature, it usually can't be attached to another creature. If it was attached to a creature, it becomes unattached.")
        ruling("2020-11-10", "If the artifact was already a creature, its base power and toughness will each become 5. This overwrites any previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Skilled Animator's ability resolves will overwrite this effect.")
        ruling("2020-11-10", "Effects that modify a creature's power and/or toughness, such as the ones created by Titanic Growth or a +/1+1 counter, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness and effects that switch power and toughness.")
    }
}
