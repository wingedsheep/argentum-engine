package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enchanted River's Grasp
 * {2}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature and remove all counters from it.
 * Enchanted creature loses all abilities and doesn't untap during its controller's untap step.
 *
 * Frozen in Ice's lock plus a counter wipe: the ETB taps the host and then clears every counter
 * kind on it via [Effects.RemoveAllCounters] (a one-shot, so counters put on later stick).
 * The lock itself is the same two Layer 6 statics — [LoseAllAbilities] and the
 * [AbilityFlag.DOESNT_UNTAP] untap restriction. `DOESNT_UNTAP` is a restriction the Aura imposes
 * on its host rather than an ability the host has, so losing all abilities doesn't shake it off.
 */
val EnchantedRiversGrasp = card("Enchanted River's Grasp") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature and remove all counters from it.\n" +
        "Enchanted creature loses all abilities and doesn't untap during its controller's untap step."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
            .then(Effects.RemoveAllCounters(EffectTarget.EnchantedCreature))
    }

    staticAbility {
        ability = LoseAllAbilities()
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Javier Charro"
        flavorText = "And fast asleep Bombur remained in spite of all they could do."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad40a4b9-9fab-49c1-8e9f-6e0776966833.jpg?1785497045"
    }
}
