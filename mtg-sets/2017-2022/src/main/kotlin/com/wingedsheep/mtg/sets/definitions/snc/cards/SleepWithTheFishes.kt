package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sleep with the Fishes
 * {2}{U}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, tap enchanted creature and you create a 1/1 blue Fish creature token
 * with "This token can't be blocked."
 * Enchanted creature doesn't untap during its controller's untap step.
 *
 * Mystic Restraints' enter-tap + `DOESNT_UNTAP` grant, with Expendable Lackey's Fish token.
 */
val SleepWithTheFishes = card("Sleep with the Fishes") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature and you create a 1/1 blue Fish creature token " +
        "with \"This token can't be blocked.\"\n" +
        "Enchanted creature doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Fish"),
            staticAbilities = listOf(CantBeBlocked(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2e726b-f124-43f7-87e4-bb06bbe8e3d7.jpg?1783922964",
        )
        description = "When this Aura enters, tap enchanted creature and you create a 1/1 blue Fish " +
            "creature token with \"This token can't be blocked.\""
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3742a0fb-4b86-49d6-b6fb-bd6a42fa1bcb.jpg?1783923138"
    }
}
