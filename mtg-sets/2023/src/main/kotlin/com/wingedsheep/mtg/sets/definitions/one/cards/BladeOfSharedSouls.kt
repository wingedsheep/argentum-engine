package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blade of Shared Souls
 * {2}{U}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Whenever this Equipment becomes attached to a creature, for as long as this Equipment remains
 * attached to it, you may have that creature become a copy of another target creature you control.
 * Equip {2}
 *
 * "Another" is relative to the creature it just attached to, so the target excludes the creature
 * this Equipment is attached to. The copy is keyed to the attachment
 * ([Duration.WhileSourceAttachedToAffected]) and reverts the moment the Equipment moves or leaves.
 */
val BladeOfSharedSouls = card("Blade of Shared Souls") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Whenever this Equipment becomes attached to a creature, for as long as this Equipment remains " +
        "attached to it, you may have that creature become a copy of another target creature you control.\n" +
        "Equip {2}"

    forMirrodin()

    triggeredAbility {
        trigger = Triggers.self.becomesAttached()
        val other = target(TargetFilter(GameObjectFilter.Creature.youControl().notAttachedToBySource()))
        effect = Effects.May(
            Effects.EachPermanentBecomesCopyOfTarget(
                target = other,
                affected = EffectTarget.AttachedToTriggeringPermanent,
                duration = Duration.WhileSourceAttachedToAffected,
            ),
            prompt = "Have the equipped creature become a copy of another creature you control?",
        )
        description = "Whenever this Equipment becomes attached to a creature, for as long as this " +
            "Equipment remains attached to it, you may have that creature become a copy of another " +
            "target creature you control."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/277aeb73-4c7c-4132-b9f9-55181d57e75d.jpg?1783918068"
    }
}
