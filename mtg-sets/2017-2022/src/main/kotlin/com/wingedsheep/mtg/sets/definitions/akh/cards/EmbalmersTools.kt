package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbilityReach
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Embalmer's Tools — Amonkhet #227
 * {2} · Artifact · Uncommon
 *
 * Activated abilities of creature cards in your graveyard cost {1} less to activate.
 * Tap an untapped Zombie you control: Target player mills a card.
 *
 * The reduction reaches graveyard activations (embalm, eternalize, scavenge, unearth) through the
 * [ActivatedAbilityReach.ACTIVATED_BY_YOU] reach, whose filter is matched against the ability's
 * source wherever it is — narrowed here to creature cards you own that are in the graveyard right
 * now, so the abilities of creatures on the battlefield and of cards in hand stay at full price.
 * Like every activated-ability reduction it trims generic mana only: Sacred Cat's embalm {W} still
 * costs {W}.
 */
val EmbalmersTools = card("Embalmer's Tools") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Activated abilities of creature cards in your graveyard cost {1} less to activate.\n" +
        "Tap an untapped Zombie you control: Target player mills a card."

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(
                GameObjectFilter.Creature.ownedByYou().currentlyIn(Zone.GRAVEYARD)
            ),
            amount = DynamicAmounts.fixed(1),
            reach = ActivatedAbilityReach.ACTIVATED_BY_YOU,
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 1,
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).untapped().youControl(),
        )
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(1, player)
        description = "Tap an untapped Zombie you control: Target player mills a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "227"
        artist = "Adrian Majkrzak"
        flavorText = "\"They might be dead, but these mummies are masters of their trade.\"\n—Jace Beleren"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c044f47-7435-4cb4-b4ad-48bb146daa9f.jpg?1783936452"
    }
}
