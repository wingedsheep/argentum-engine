package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Protean Thaumaturge
 * {1}{U}
 * Creature — Human Wizard
 * 1/1
 *
 * Constellation — Whenever an enchantment you control enters, you may have this creature become a
 * copy of another target creature, except it has this ability.
 *
 * Sage of Mysteries' constellation watcher feeding Vesuvan Doppelganger's upkeep re-copy:
 * [Effects.EachPermanentBecomesCopyOfTarget] on itself with `retainResolvingTriggeredAbility`,
 * which stamps the resolving constellation trigger onto the copy as copiable rules text — so the
 * Thaumaturge keeps re-shaping on later enchantments, and anything that copies it gets the ability
 * too (ruling). The copy is indefinite (`Duration.Permanent`, the facade default).
 */
val ProteanThaumaturge = card("Protean Thaumaturge") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "Constellation — Whenever an enchantment you control enters, you may have this creature " +
        "become a copy of another target creature, except it has this ability."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Enchantment.youControl()).enters()
        val creature = target(TargetFilter.OtherCreature)
        description = "Constellation — Whenever an enchantment you control enters, you may have this creature " +
            "become a copy of another target creature, except it has this ability."
        effect = Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = creature,
                affected = EffectTarget.Self,
                exceptions = CopyExceptions(retainResolvingTriggeredAbility = true),
            ),
            sourceRequiredZone = Zone.BATTLEFIELD,
            descriptionOverride = "Have Protean Thaumaturge become a copy of the target creature?",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Izzy"
        flavorText = "\"Once you see past the veil we call 'reality,' altering your own shape is elementary.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/4/749bfd81-c61b-4901-8efb-72611fd08244.jpg?1783931580"
        ruling(
            "2020-01-24",
            "Protean Thaumaturge copies the printed values of the target creature, plus any copy effects that " +
                "have been applied to it. It won't copy counters on that creature or effects that have changed " +
                "its power, toughness, types, color, or so on."
        )
        ruling("2020-01-24", "If something becomes a copy of Protean Thaumaturge, it also has the constellation ability.")
        ruling(
            "2020-01-24",
            "Protean Thaumaturge's ability causes it to become a copy indefinitely. It doesn't expire during the " +
                "cleanup step or if the target creature leaves the battlefield after the ability has resolved."
        )
    }
}
