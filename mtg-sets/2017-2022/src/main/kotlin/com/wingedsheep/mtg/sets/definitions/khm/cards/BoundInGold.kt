package com.wingedsheep.mtg.sets.definitions.khm.cards

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
 * Bound in Gold
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant permanent
 * Enchanted permanent can't attack, block, or crew Vehicles, and its activated abilities can't be
 * activated unless they're mana abilities.
 *
 * Same shape as Intercessor's Arrest: Arrest over any permanent, [AbilityFlag.CANT_CREW] on the
 * host, and the non-mana-ability activation lock (`nonManaAbilitiesOnly = true`).
 */
val BoundInGold = card("Bound in Gold") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant permanent\n" +
        "Enchanted permanent can't attack, block, or crew Vehicles, and its activated abilities can't be " +
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
        collectorNumber = "5"
        artist = "Victor Adame Minguez"
        flavorText = "\"Trespassers will be smelted.\"\n—Axgard inscription"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/074d526a-1eef-4045-bd38-f6d68c4bc4b9.jpg?1783928289"
        ruling("2021-02-05", "A creature that \"can't crew Vehicles\" can't be tapped to pay the crew cost of a Vehicle.")
        ruling(
            "2021-02-05",
            "Bound in Gold doesn't stop static abilities from affecting the game, and it doesn't stop " +
                "triggered abilities from triggering. It also doesn't stop mana abilities from being " +
                "activated. Most activated abilities that produce mana as they resolve are mana abilities."
        )
    }
}
