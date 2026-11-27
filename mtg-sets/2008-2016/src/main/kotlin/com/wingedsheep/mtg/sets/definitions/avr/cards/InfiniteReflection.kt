package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Infinite Reflection
 * {5}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters attached to a creature, each other nontoken creature you control becomes a
 * copy of that creature.
 * Nontoken creatures you control enter as a copy of enchanted creature.
 *
 * "That creature" is [EffectTarget.AttachedToTriggeringPermanent]: the creature the Aura entered
 * attached to, frozen when the trigger fires, so moving the Aura in response doesn't change what is
 * copied (ruling). The copies are permanent and outlast the Aura.
 *
 * The last line is a printed [EntersAsCopy] whose `copyOf` is [EffectTarget.EnchantedCreature]: it
 * applies to *other* creatures entering under your control and copies the creature the Aura is
 * attached to as each one enters. As a copy effect it applies before the entrant's own as-enters
 * replacements (an entering Clone ends up a copy of the enchanted creature either way), and with
 * several Infinite Reflections you choose which copy applies last.
 */
val InfiniteReflection = card("Infinite Reflection") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters attached to a creature, each other nontoken creature you control " +
        "becomes a copy of that creature.\n" +
        "Nontoken creatures you control enter as a copy of enchanted creature."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = EffectTarget.AttachedToTriggeringPermanent,
            filter = GroupFilter(GameObjectFilter.Creature.nontoken().youControl()),
            excludeTarget = true,
        )
        description = "When this Aura enters attached to a creature, each other nontoken creature " +
            "you control becomes a copy of that creature."
    }

    replacementEffect(
        EntersAsCopy(
            optional = false,
            copyOf = EffectTarget.EnchantedCreature,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().youControl(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42506c54-e9bf-4d0f-8dd5-8b218668c925.jpg?1783940717"

        ruling(
            "2012-05-01",
            "If Infinite Reflection enters attached to a creature and then becomes attached to a " +
                "different creature before its \"enters\" trigger resolves, each other nontoken " +
                "creature you control will become a copy of the first creature.",
        )
        ruling(
            "2012-05-01",
            "If Infinite Reflection leaves the battlefield or becomes attached to a different " +
                "creature, none of the copy effects end. Your creatures will still be copies of whatever " +
                "creature each was a copy of.",
        )
    }
}
