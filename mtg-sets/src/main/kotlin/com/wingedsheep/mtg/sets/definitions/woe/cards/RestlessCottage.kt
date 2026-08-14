package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Restless Cottage
 * Land
 *
 * This land enters tapped.
 * {T}: Add {B} or {G}.
 * {2}{B}{G}: This land becomes a 4/4 black and green Horror creature until end of turn. It's still
 *   a land.
 * Whenever this land attacks, create a Food token and exile up to one target card from a graveyard.
 *
 * The black-green member of the Wilds of Eldraine "Restless" creature-land cycle (see
 * [RestlessBivouac]). As with the rest of the cycle the attack trigger is an intrinsic triggered
 * ability of the land, not one granted by the animate ability.
 *
 * The exile is "up to one target", so the ability still resolves — and still makes the Food — when no
 * card is chosen or when the chosen card leaves the graveyard before resolution. The Food is not
 * conditional on the exile: the two clauses are joined by "and", not "if you do".
 */
val RestlessCottage = card("Restless Cottage") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {B} or {G}.\n" +
        "{2}{B}{G}: This land becomes a 4/4 black and green Horror creature until end of turn. " +
        "It's still a land.\n" +
        "Whenever this land attacks, create a Food token and exile up to one target card from a " +
        "graveyard."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}{G}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Horror"),
            colors = setOf(Color.BLACK.name, Color.GREEN.name),
            duration = Duration.EndOfTurn,
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        val exiled = target(
            "target card in a graveyard",
            TargetObject(optional = true, filter = TargetFilter.CardInGraveyard),
        )
        effect = Effects.Composite(
            Effects.CreateFood(),
            Effects.Move(exiled, Zone.EXILE),
        )
        description = "Whenever this land attacks, create a Food token and exile up to one target " +
            "card from a graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Jesper Ejsing"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/787eadf3-5005-4ae5-820f-4012a4d4e1a5.jpg?1783915056"

        ruling(
            "2023-09-01",
            "If this becomes a creature because of an effect other than its own ability, its last " +
                "ability will still trigger whenever it attacks."
        )
        ruling(
            "2023-09-01",
            "If this becomes a creature but you haven't controlled it continuously since your most " +
                "recent turn began, you won't be able to activate its mana ability or attack with " +
                "it that turn."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a " +
                "creature type."
        )
    }
}
