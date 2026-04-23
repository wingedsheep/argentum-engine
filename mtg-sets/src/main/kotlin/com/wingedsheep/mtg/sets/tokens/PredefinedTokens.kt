package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Predefined token CardDefinitions.
 *
 * These are registered in the CardRegistry so that the engine can look up token abilities
 * by name (e.g., "Treasure" → its mana ability). The unified [CreatePredefinedTokenExecutor]
 * creates entities with a matching `name` field, and the engine resolves abilities via
 * `cardRegistry.getCard(name)`.
 *
 * To add a new predefined token type, define it here and add a facade method to `Effects.kt`.
 */
object PredefinedTokens {

    /**
     * Treasure token — an artifact with:
     * "{T}, Sacrifice this artifact: Add one mana of any color."
     */
    val Treasure = card("Treasure") {
        typeLine = "Artifact - Treasure"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.AddAnyColorMana(1)
            manaAbility = true
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/4/8/4837a3f1-ca7f-41e5-a5d1-729c8495b0e8.jpg?1771590279"
        }
    }

    /**
     * Food token — an artifact with:
     * "{2}, {T}, Sacrifice this artifact: You gain 3 life."
     */
    val Food = card("Food") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/0/d/0dce2241-e58b-41d4-b57c-9794fc8ee004.jpg?1721425221"
        }
    }

    /**
     * Lander token — an artifact with:
     * "{2}, {T}, Sacrifice this token: Search your library for a basic land card,
     * put it onto the battlefield tapped, then shuffle."
     */
    val Lander = card("Lander") {
        typeLine = "Artifact - Lander"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/8/5/85ef1950-219f-401b-8ff5-914f9aaec122.jpg?1752946491"
            artist = "Jorge Jacinto"
            collectorNumber = "8"
        }
    }

    /**
     * "Just One Glass" — a named Food token created by Sekshaas, Early Sleeper.
     * Functionally identical to a Food token, but with custom name and art.
     */
    val JustOneGlass = card("Just One Glass") {
        typeLine = "Artifact - Food"

        activatedAbility {
            cost = Costs.Composite(
                Costs.Mana("{2}"),
                Costs.Tap,
                Costs.SacrificeSelf
            )
            effect = Effects.GainLife(3)
        }

        metadata {
            imageUri = "/images/custom/just-one-glass.jpeg"
        }
    }

    /**
     * Sword token — a colorless Equipment artifact with:
     * "Equipped creature gets +1/+1" and equip {2}.
     * Created by Blacksmith's Talent.
     */
    val Sword = card("Sword") {
        typeLine = "Artifact — Equipment"

        staticAbility {
            effect = Effects.ModifyStats(+1, +1)
            filter = Filters.EquippedCreature
        }

        equipAbility("{2}")

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/b/b/bb1e78e6-a9e7-48a4-9231-61fb331c5837.jpg?1721426299"
        }
    }

    /**
     * Cragflame — a legendary colorless Equipment artifact token with:
     * "Equipped creature gets +1/+1 and has vigilance, trample, and haste" and equip {2}.
     * Created by Mabel, Heir to Cragflame.
     */
    val Cragflame = card("Cragflame") {
        typeLine = "Legendary Artifact — Equipment"

        staticAbility {
            effect = Effects.ModifyStats(+1, +1)
            filter = Filters.EquippedCreature
        }

        staticAbility {
            effect = Effects.GrantKeyword(Keyword.VIGILANCE)
            filter = Filters.EquippedCreature
        }

        staticAbility {
            effect = Effects.GrantKeyword(Keyword.TRAMPLE)
            filter = Filters.EquippedCreature
        }

        staticAbility {
            effect = Effects.GrantKeyword(Keyword.HASTE)
            filter = Filters.EquippedCreature
        }

        equipAbility("{2}")

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/c/7/c76fa1c6-6000-47b2-9188-9c15b2c73f8f.jpg?1721431172"
        }
    }

    /**
     * Mutavault — a Land token with:
     * "{T}: Add {C}."
     * "{1}: This token becomes a 2/2 creature with all creature types until end of turn.
     *  It's still a land."
     * Created by Mutable Explorer.
     */
    val Mutavault = card("Mutavault") {
        typeLine = "Land — Mutavault"

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }

        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = BecomeCreatureEffect(
                target = EffectTarget.Self,
                power = 2,
                toughness = 2,
                keywords = setOf(Keyword.CHANGELING)
            )
        }

        metadata {
            imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2f5d31-a1c6-465f-b518-b40acdfab8aa.jpg?1767955820"
        }
    }

    /**
     * All predefined token definitions.
     * Register these in the CardRegistry so token abilities are resolved.
     */
    val allTokens: List<CardDefinition> = listOf(
        Treasure,
        Food,
        Lander,
        JustOneGlass,
        Sword,
        Cragflame,
        Mutavault
    )
}
