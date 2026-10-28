package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lithoform Blight — Zendikar Rising #109 (canonical printing)
 * {1}{B} · Enchantment — Aura
 *
 * Enchant land
 * When this Aura enters, draw a card.
 * Enchanted land loses all land types and abilities and has "{T}: Add {C}" and "{T}, Pay 1 life:
 * Add one mana of any color."
 *
 * "Loses all land types and abilities" is [SetLandTypesForGroup] with no land types over the
 * enchanted land: every land type (basic or not) goes, card types and supertypes stay (per the
 * ruling). The two granted mana abilities survive the ability wipe, like Imprisoned in the Moon's.
 */
private val enchantedLand = GroupFilter(GameObjectFilter.Land, scope = Scope.AttachedTo)

val LithoformBlight = card("Lithoform Blight") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, draw a card.\n" +
        "Enchanted land loses all land types and abilities and has \"{T}: Add {C}\" and " +
        "\"{T}, Pay 1 life: Add one mana of any color.\""
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Land))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = SetLandTypesForGroup(filter = enchantedLand, landTypes = emptySet())
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.AddColorlessMana(1),
                timing = TimingRule.ManaAbility,
                isManaAbility = true,
                descriptionOverride = "{T}: Add {C}."
            ),
            filter = enchantedLand
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Composite(Costs.Tap, Costs.PayLife(1)),
                effect = Effects.AddAnyColorMana(1),
                timing = TimingRule.ManaAbility,
                isManaAbility = true,
                descriptionOverride = "{T}, Pay 1 life: Add one mana of any color."
            ),
            filter = enchantedLand
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "Denis Nigmatullin"
        flavorText = "Heedless of Nissa's warnings, Nahiri repeated history."
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b36d12e-9a68-4d3d-92e8-9840cd5320bc.jpg?1783929375"

        ruling(
            "2020-09-25",
            "If the target land is an illegal target by the time Lithoform Blight tries to resolve, " +
                "the spell doesn't resolve. It doesn't enter the battlefield, so you don't draw a card."
        )
        ruling(
            "2020-09-25",
            "The enchanted land keeps any other card types (such as artifact) and supertypes (such as " +
                "basic or legendary) when it loses its land types."
        )
    }
}
