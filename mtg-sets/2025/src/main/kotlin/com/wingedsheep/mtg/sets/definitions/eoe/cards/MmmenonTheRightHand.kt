package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Mm'menon, the Right Hand
 * {3}{U}{U}
 * Legendary Creature — Jellyfish Advisor
 * 3/4
 *
 * Flying
 * You may look at the top card of your library any time.
 * You may cast artifact spells from the top of your library.
 * Artifacts you control have "{T}: Add {U}. Spend this mana only to cast a spell
 *   from anywhere other than your hand."
 */
val MmmenonTheRightHand = card("Mm'menon, the Right Hand") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Jellyfish Advisor"
    oracleText = "Flying\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast artifact spells from the top of your library.\n" +
        "Artifacts you control have \"{T}: Add {U}. Spend this mana only to cast a spell from anywhere other than your hand.\""
    power = 3
    toughness = 4

    keywords(Keyword.FLYING)

    staticAbility {
        ability = LookAtTopOfLibrary()
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(filter = GameObjectFilter.Artifact)
    }

    // The granted ability is a mana ability (single colored payment, no triggers). The
    // CastFromNonHandOnly restriction is enforced by ManaPool.isSatisfiedBy: only spell
    // casts whose SpellPaymentContext reports isFromHand=false consume this mana, so the
    // {U} can pay for casts from exile/graveyard/library/command zone but not from hand,
    // and never for activated-ability costs.
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.BLUE, 1, restriction = ManaRestriction.CastFromNonHandOnly),
                isManaAbility = true
            ),
            filter = GroupFilter(GameObjectFilter.Artifact.youControl())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82add0a0-e402-4b31-b101-81c0bf332015.jpg?1752946825"
    }
}
