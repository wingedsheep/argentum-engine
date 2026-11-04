package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm the Vault // Vault of Catlacan (Rivals of Ixalan — the card's earliest real printing)
 * {2}{U}{R}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Storm the Vault (Legendary Enchantment, {2}{U}{R})
 *   Whenever one or more creatures you control deal combat damage to a player, create a Treasure
 *   token.
 *   At the beginning of your end step, if you control five or more artifacts, transform Storm the
 *   Vault.
 *
 * Back — Vault of Catlacan (Legendary Land)
 *   {T}: Add one mana of any color.
 *   {T}: Add {U} for each artifact you control.
 *
 * Implementation:
 *  - The Treasure trigger is the batched `oneOrMore(Creature).dealCombatDamageToAPlayer()`: one
 *    trigger per player hit in each combat-damage step, so first strike or hitting two players at
 *    once triggers it more than once, as the rulings say.
 *  - The transform is an intervening "if" ([Conditions.YouControlAtLeast] five artifacts), checked
 *    as the end step begins and again on resolution.
 */

private val StormTheVaultFront = card("Storm the Vault") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Enchantment"
    oracleText = "Whenever one or more creatures you control deal combat damage to a player, create " +
        "a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any " +
        "color.\")\n" +
        "At the beginning of your end step, if you control five or more artifacts, transform Storm " +
        "the Vault."

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.CreateTreasure()
        description = "Whenever one or more creatures you control deal combat damage to a player, " +
            "create a Treasure token."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouControlAtLeast(5, GameObjectFilter.Artifact)
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of your end step, if you control five or more artifacts, " +
            "transform Storm the Vault."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c16ba84e-a0cc-4c6c-9b80-713247b8fef9.jpg?1783935274"
        ruling(
            "2018-01-19",
            "Storm the Vault's first ability can trigger more than once in a turn if creatures you " +
                "control deal combat damage at different times in a turn (most likely because one or " +
                "more has first strike) or if creatures you control deal combat damage to more than " +
                "one player at once."
        )
        ruling(
            "2018-01-19",
            "The last ability of Storm the Vault doesn't trigger if you don't control five or more " +
                "artifacts as your end step begins. If it does trigger but you don't control five or " +
                "more artifacts as it resolves, it does nothing."
        )
    }
}

private val VaultOfCatlacan = card("Vault of Catlacan") {
    manaCost = ""
    colorIdentity = "UR"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Storm the Vault.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{T}: Add {U} for each artifact you control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.BLUE,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Artifact).count(),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/back/c/1/c16ba84e-a0cc-4c6c-9b80-713247b8fef9.jpg?1783935274"
    }
}

val StormTheVault: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = StormTheVaultFront,
    backFace = VaultOfCatlacan,
)
