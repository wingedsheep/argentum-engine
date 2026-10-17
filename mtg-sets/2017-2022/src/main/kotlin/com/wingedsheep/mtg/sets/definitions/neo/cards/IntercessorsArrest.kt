package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Intercessor's Arrest
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant permanent
 * Enchanted permanent can't attack, block, or crew Vehicles. Its activated abilities can't be
 * activated unless they're mana abilities.
 *
 * Arrest's shape over any permanent, plus [AbilityFlag.CANT_CREW] on the host and the
 * "unless they're mana abilities" carve-out (`nonManaAbilitiesOnly = true`). Enchanting a Vehicle
 * shuts off its crew ability too — crew is an activated ability of the Vehicle (CR 702.122a).
 */
val IntercessorsArrest = card("Intercessor's Arrest") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\n" +
        "Enchanted permanent can't attack, block, or crew Vehicles. Its activated abilities can't be " +
        "activated unless they're mana abilities."

    auraTarget = TargetObject(filter = TargetFilter.Permanent)

    staticAbility {
        ability = CantAttack(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.CANT_CREW.name)
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
            nonManaAbilitiesOnly = true,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83f2148-bb5d-4236-8d12-150deed0577e.jpg?1783923919"
        ruling("2022-02-18", "A creature that \"can't crew Vehicles\" can't be tapped to pay the crew cost of a Vehicle.")
        ruling(
            "2022-02-18",
            "Intercessor's Arrest doesn't stop static abilities from affecting the game, and it doesn't " +
                "stop triggered abilities from triggering. It also doesn't stop mana abilities from being " +
                "activated. Most activated abilities that produce mana as they resolve are mana abilities."
        )
    }
}
