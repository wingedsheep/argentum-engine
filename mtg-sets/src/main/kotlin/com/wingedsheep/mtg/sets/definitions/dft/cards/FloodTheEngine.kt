package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Flood the Engine — Aetherdrift #42
 * {2}{U} · Enchantment — Aura
 *
 * Enchant creature or Vehicle
 * When this Aura enters, tap enchanted permanent.
 * Enchanted permanent loses all abilities and doesn't untap during its controller's untap step.
 *
 * Same shape as Stop Cold, widened to Vehicles: the enchant clause is
 * [GameObjectFilter.CreatureOrVehicle] (a Vehicle matched by its subtype), and the one-shot tap
 * points at [EffectTarget.EnchantedPermanent] rather than `EnchantedCreature` so it also works
 * when the host is a still-uncrewed, non-creature Vehicle. The two static clauses use the default
 * `GroupFilter.attachedCreature()` scope, which is `Scope.AttachedTo` over any permanent, so they
 * apply regardless of the host's types. "Doesn't untap" reuses the untap step's existing
 * [AbilityFlag.DOESNT_UNTAP] handling.
 */
val FloodTheEngine = card("Flood the Engine") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature or Vehicle\n" +
        "When this Aura enters, tap enchanted permanent.\n" +
        "Enchanted permanent loses all abilities and doesn't untap during its controller's " +
        "untap step."

    auraTarget = TargetPermanent(filter = TargetFilter(GameObjectFilter.CreatureOrVehicle))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedPermanent)
        description = "When this Aura enters, tap enchanted permanent."
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "42"
        artist = "Eric Wilkerson"
        flavorText = "There had been a time when the Endriders would've killed for water."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57402f7c-5d4c-4f1e-8bce-a2328a297111.jpg?1783907909"
    }
}
