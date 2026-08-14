package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Swordsman, Sharp Scoundrel — Marvel Super Heroes #116
 * {1}{B} · Legendary Creature — Human Hero Villain · Uncommon
 * 2/2
 *
 * Whenever another Villain you control enters, attach up to one target Equipment you control to
 * target creature you control.
 * Whenever an equipped creature you control attacks, it connives.
 *
 * Modeling notes:
 *  - The Villain trigger watches any Villain *permanent* you control (Villain also shows up on
 *    artifact creatures and tokens in this set), with [TriggerBinding.OTHER] so Swordsman — himself
 *    a Villain — doesn't trigger on his own arrival.
 *  - "Up to one target Equipment" is an `optional = true` [TargetPermanent]; declining it, or the
 *    Equipment becoming illegal, leaves [Effects.AttachTargetEquipmentToCreature] a graceful no-op
 *    (the Raubahn / Weapons Vendor shape). The creature is a required target, so with no creature you
 *    control the trigger is simply removed from the stack for having no legal targets.
 *  - The attack trigger is a filtered [Triggers.attacks] with [TriggerBinding.ANY] — the equipped
 *    state is a projected state predicate, so an Equipment attached during declare attackers by an
 *    earlier trigger doesn't retroactively add a connive, and Swordsman himself connives if he's the
 *    equipped attacker. Connive lands its +1/+1 counter on [EffectTarget.TriggeringEntity], i.e. the
 *    attacking creature ("it connives"), not on Swordsman.
 */
val SwordsmanSharpScoundrel = card("Swordsman, Sharp Scoundrel") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Hero Villain"
    power = 2
    toughness = 2
    oracleText = "Whenever another Villain you control enters, attach up to one target Equipment " +
        "you control to target creature you control.\n" +
        "Whenever an equipped creature you control attacks, it connives. (Draw a card, then discard " +
        "a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.VILLAIN).youControl(),
            binding = TriggerBinding.OTHER
        )
        val equipment = target(
            "up to one target Equipment you control",
            TargetPermanent(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
                ),
                optional = true
            )
        )
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachTargetEquipmentToCreature(equipment, creature)
    }

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl().equipped(),
            binding = TriggerBinding.ANY
        )
        effect = Effects.Connive(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Lordigan"
        flavorText = "\"None can match me with the blade!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/5/6572aafe-18ed-4182-92e9-25f003f5fe3d.jpg?1783902937"
    }
}
