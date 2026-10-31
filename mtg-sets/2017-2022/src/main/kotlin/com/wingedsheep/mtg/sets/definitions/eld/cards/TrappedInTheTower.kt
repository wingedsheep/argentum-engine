package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Trapped in the Tower
 * {1}{W}
 * Enchantment — Aura
 *
 * Enchant creature without flying
 * Enchanted creature can't attack or block, and its activated abilities can't be activated.
 *
 * Arrest's three-static lock behind a narrower enchant restriction. The `auraTarget` filter is
 * also the enchant state-based action's filter, read against projected state, so a host that
 * later gains flying puts this Aura into its owner's graveyard (per the ruling). The activation
 * lock has no mana-ability carve-out, so `nonManaAbilitiesOnly` stays false.
 */
val TrappedInTheTower = card("Trapped in the Tower") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature without flying\n" +
        "Enchanted creature can't attack or block, and its activated abilities can't be activated."

    auraTarget = TargetObject(filter = TargetFilter.Creature.withoutKeyword(Keyword.FLYING))

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Howard Lyon"
        flavorText = "\"At least up here I have some peace and quiet.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/974e84ce-5b51-4bd7-9a4d-b64d8f8f62d4.jpg?1783932663"
        ruling(
            "2019-10-04",
            "If the enchanted creature gains flying, Trapped in the Tower is put into your graveyard " +
                "as a state-based action. The creature won't get stuck in the tower again when it loses flying."
        )
        ruling(
            "2019-10-04",
            "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" " +
                "Some keyword abilities are activated abilities and will have colons in their reminder text. " +
                "Triggered abilities (starting with \"when,\" \"whenever,\" or \"at\") are unaffected by " +
                "Trapped in the Tower."
        )
    }
}
