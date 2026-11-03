package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Combadge — Star Trek #260
 * {3} · Artifact — Communicator Equipment
 *
 * When you cast this spell, copy it. (The copy becomes a token.)
 * Equipped creature has "Whenever this creature attacks while you control another creature with a
 * Communicator attached to it, this creature can't be blocked this turn."
 * Equip {2}
 *
 * The cast trigger is the Brass Knuckles shape: `Triggers.self.isCast()` copying the triggering
 * spell, which resolves into a token permanent (CR 707.10f) without re-triggering.
 *
 * The quoted ability is granted to the equipped creature ([GrantTriggeredAbility] over
 * [Filters.EquippedCreature]), so a granted trigger's source is that creature. "Attacks while …"
 * is a trigger restriction checked only as the creature attacks, not again on resolution. "Another
 * creature with a Communicator attached to it" reads as: some Communicator on the battlefield
 * (any controller's) is attached to a creature you control that is not this creature — so two
 * Combadges on the same attacker don't count.
 */
val Combadge = card("Combadge") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Communicator Equipment"
    oracleText = "When you cast this spell, copy it. (The copy becomes a token.)\n" +
        "Equipped creature has \"Whenever this creature attacks while you control another creature " +
        "with a Communicator attached to it, this creature can't be blocked this turn.\"\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.isCast()
        effect = Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity, mayChooseNewTargets = false)
        description = "When you cast this spell, copy it."
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                triggerRestriction = Conditions.AnyPlayerControls(
                    GameObjectFilter.Permanent
                        .withSubtype("Communicator")
                        .attachedTo(GameObjectFilter.Creature.youControl().notSourceItself())
                ),
                effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
            ),
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "260"
        artist = "Paolo Puggioni"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bcf6bb5-f689-4d61-8f69-3290eeab539d.jpg?1793359559"
    }
}
