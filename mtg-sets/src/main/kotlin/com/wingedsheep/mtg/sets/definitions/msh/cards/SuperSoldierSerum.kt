package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantSupertype
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * "attach ... to it" — one attach per chosen Equipment. [EffectTarget.Self] is the granted
 * ability's source, i.e. the enchanted creature.
 */
private val attachEquipmentToSelf = ForEachTargetEffect(
    listOf(
        Effects.AttachTargetEquipmentToCreature(
            equipmentTarget = EffectTarget.ContextTarget(0),
            creatureTarget = EffectTarget.Self,
        )
    )
)

/** "any number of target Equipment you control" — `unlimited` implies a minimum of zero. */
private fun equipmentYouControl(): TargetObject = TargetPermanent(
    unlimited = true,
    filter = TargetFilter(
        baseFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()
    ),
)

/**
 * Super-Soldier Serum — Marvel Super Heroes #38
 * {1}{W} · Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2, has first strike and vigilance, and is a legendary Soldier in
 * addition to its other types.
 * Whenever enchanted creature attacks or blocks, attach any number of target Equipment you
 * control to it.
 *
 * The buff line is the On Serra's Wings shape — [ModifyStats], two [GrantKeyword]s, a
 * [GrantSupertype]`("LEGENDARY")` and a [GrantSubtype]`("Soldier")`, every one of them scoped
 * explicitly to [Filters.EnchantedCreature] (GrantSubtype defaults to the *source*, so an Aura
 * that omits the filter would silently make itself a Soldier).
 *
 * The combat ability is one printed ability with two trigger conditions, so it is modeled as two
 * granted triggered abilities — the Astrologian's Planisphere idiom: [GrantTriggeredAbility] over
 * [Filters.EnchantedCreature] installs the trigger *on the enchanted creature*, where
 * [Triggers.Attacks] and [Triggers.Blocks] both fire with SELF binding and "it"
 * ([EffectTarget.Self]) is the creature itself. "Any number of target Equipment you control" is a
 * `TargetPermanent(unlimited = true)` (minimum zero) fanned out with [ForEachTargetEffect] so each
 * chosen Equipment is attached independently by [Effects.AttachTargetEquipmentToCreature].
 *
 * Known deviation: because the ability is granted to the creature rather than kept on the Aura,
 * its controller is the *creature's* controller, so "Equipment you control" reads from that
 * player. That only differs from the printed card when the Aura is attached to a creature its
 * controller doesn't control. Keeping it on the Aura is not an option: the engine's
 * `AttachmentTriggerDetector` has no block branch, so an `ATTACHED`-bound blocks trigger would
 * never fire at all.
 */
val SuperSoldierSerum = card("Super-Soldier Serum") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2, has first strike and vigilance, and is a legendary " +
        "Soldier in addition to its other types.\n" +
        "Whenever enchanted creature attacks or blocks, attach any number of target Equipment " +
        "you control to it."

    auraTarget = Targets.Creature

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantSupertype("LEGENDARY", Filters.EnchantedCreature)
    }
    staticAbility {
        ability = GrantSubtype("Soldier", Filters.EnchantedCreature)
    }

    // "Whenever enchanted creature attacks ..."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Attacks.event,
                binding = Triggers.Attacks.binding,
                effect = attachEquipmentToSelf,
                targetRequirement = equipmentYouControl(),
                descriptionOverride = "Whenever this creature attacks, attach any number of " +
                    "target Equipment you control to it.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    // "... or blocks, attach any number of target Equipment you control to it."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.Blocks.event,
                binding = Triggers.Blocks.binding,
                effect = attachEquipmentToSelf,
                targetRequirement = equipmentYouControl(),
                descriptionOverride = "Whenever this creature blocks, attach any number of " +
                    "target Equipment you control to it.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Rafater"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/845b0be1-4f85-4a8c-8205-dc85c8cf9a61.jpg?1783902965"
    }
}
