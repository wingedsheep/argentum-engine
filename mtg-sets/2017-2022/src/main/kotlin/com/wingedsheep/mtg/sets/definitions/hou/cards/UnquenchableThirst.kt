package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unquenchable Thirst — Hour of Devastation #53 (canonical printing)
 * {1}{U} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, if you control a Desert or there is a Desert card in your graveyard,
 * tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Mystic Restraints' enter-tap + `DOESNT_UNTAP` lock, with Desert's Hold's Desert intervening-if
 * (CR 603.4): checked as the trigger fires and again on resolution, either half satisfying each check.
 */
val UnquenchableThirst = card("Unquenchable Thirst") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, if you control a Desert or there is a Desert card in your graveyard, " +
        "tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.Any(
            Conditions.ControlPermanentOfType(Subtype.DESERT),
            Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Jeremy Wilson"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a418b2aa-dbc2-409b-9f19-e16a0ad8cbac.jpg?1783936046"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn't matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
        ruling("2017-07-14", "For abilities that trigger only if you control a Desert or there is a Desert card in your graveyard, one condition must be true as the ability triggers and one must be true as the ability resolves. They don't have to be the same condition, though. For example, you could sacrifice your only Desert after the ability triggers but before it has resolved.")
    }
}
