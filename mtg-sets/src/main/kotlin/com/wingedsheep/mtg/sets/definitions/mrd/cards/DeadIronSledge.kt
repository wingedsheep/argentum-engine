package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dead-Iron Sledge — Mirrodin #162
 * {1} · Artifact — Equipment
 *
 * Whenever equipped creature blocks or becomes blocked by a creature, destroy both creatures.
 * Equip {2}
 *
 * Modeling notes:
 *  - The combat trigger is **granted to the equipped creature** ([GrantTriggeredAbility] over
 *    [Filters.EquippedCreature], the Pirate Hat / Dire Blunderbuss idiom) rather than living on
 *    the Equipment with an `ATTACHED` binding. That is what makes the 2006-10-15 ruling fall out
 *    for free: *"Moving Dead-Iron Sledge after the ability triggers will not affect which
 *    creatures are destroyed."* Because the granted ability's source is the creature, the
 *    instance already on the stack keeps pointing at that creature — `EffectTarget.Self` resolves
 *    to it — even if the Sledge is moved to another creature (or leaves the battlefield) in
 *    response. An `ATTACHED`-bound ability on the Equipment would re-read the Sledge's *current*
 *    attachment at resolution and destroy the wrong pair.
 *  - "Destroy **both** creatures" = the equipped creature ([EffectTarget.Self] of the granted
 *    ability) and the combat partner ([EffectTarget.TriggeringEntity], which
 *    [Triggers.BlocksOrBecomesBlockedBy] sets to the blocking/blocked creature). They are
 *    destroyed by one resolution; dies-triggers from both still go on the stack together
 *    afterwards, since triggers wait for the resolving ability to finish (CR 603.3b).
 *  - Nothing targets, so a creature with hexproof/shroud is still destroyed, and neither
 *    destruction is prevented by the other's death. Both are ordinary destruction, so
 *    indestructible and regeneration apply as usual.
 */
val DeadIronSledge = card("Dead-Iron Sledge") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature blocks or becomes blocked by a creature, destroy both creatures.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.BlocksOrBecomesBlockedBy(GameObjectFilter.Creature).event,
                binding = TriggerBinding.SELF,
                effect = Effects.Composite(
                    Effects.Destroy(EffectTarget.Self),
                    Effects.Destroy(EffectTarget.TriggeringEntity)
                ),
                descriptionOverride = "Whenever this creature blocks or becomes blocked by a creature, destroy both creatures."
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Ray Lago"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dc312c0-14da-4b23-8293-6fa41bdd3167.jpg?1783944524"
        ruling("2006-10-15", "Moving Dead-Iron Sledge after the ability triggers will not affect which creatures are destroyed.")
    }
}
