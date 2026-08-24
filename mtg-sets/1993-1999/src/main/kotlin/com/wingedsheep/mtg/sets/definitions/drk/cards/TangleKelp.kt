package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tangle Kelp
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step if it attacked during its
 * controller's last turn.
 *
 * Goblin Rock Sled's untap clause handed to someone else, and the reason the condition can't simply
 * be source-scoped: the *Aura* is the source here and an Aura never attacks, so
 * `SourceAttackedLastTurn` would always read false. `EnchantedPermanentMatches` aims the same
 * predicate at the host instead.
 *
 * The ETB tap is what makes the Kelp bite immediately — without it the creature would be untapped
 * and the untap clause would have nothing to hold down until after its next attack.
 */
val TangleKelp = card("Tangle Kelp") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step if it attacked " +
        "during its controller's last turn."
    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name, GroupFilter.attachedCreature()),
            condition = Conditions.EnchantedPermanentMatches(
                GameObjectFilter.Any.attackedLastTurn()
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/497bc42d-ab81-4d84-bdf7-e3a05a7c984d.jpg?1783947941"
    }
}
