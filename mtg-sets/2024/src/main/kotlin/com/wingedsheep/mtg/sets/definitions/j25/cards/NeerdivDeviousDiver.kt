package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Neerdiv, Devious Diver — Foundations Jumpstart #36
 * {2}{U} · Legendary Creature — Merfolk Rogue · 2/2
 *
 * Whenever Neerdiv becomes tapped, target player mills cards equal to its power.
 * Whenever you cast a spell from your graveyard or activate an ability of a card in your
 * graveyard, draw a card and put a +1/+1 counter on Neerdiv.
 *
 * - The mill reads Neerdiv's power on resolution, or as it last existed if it has left the
 *   battlefield (ruling).
 * - "A card in your graveyard" is a card you own in a graveyard — every graveyard holds only its
 *   owner's cards. The unqualified "activate an ability" counts mana abilities too. A card its own
 *   cost exiles from the graveyard (embalm, scavenge) still counts: the engine matches it as it was
 *   when the ability was activated.
 * - The trigger resolves before the spell or ability that caused it, even if that one is countered
 *   (ruling).
 */
val NeerdivDeviousDiver = card("Neerdiv, Devious Diver") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Rogue"
    power = 2
    toughness = 2
    oracleText = "Whenever Neerdiv becomes tapped, target player mills cards equal to its power.\n" +
        "Whenever you cast a spell from your graveyard or activate an ability of a card in your " +
        "graveyard, draw a card and put a +1/+1 counter on Neerdiv."

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.sourcePower(), player)
    }

    triggeredAbility {
        trigger = Triggers.or(
            Triggers.you.casts(
                requires = setOf(
                    SpellCastPredicate.CastFromZone(Zone.GRAVEYARD),
                    SpellCastPredicate.SpellMatches(GameObjectFilter.Any.ownedByYou()),
                )
            ),
            Triggers.you.activatesAbility(
                of = GameObjectFilter.Any.ownedByYou().currentlyIn(Zone.GRAVEYARD),
                includeManaAbilities = true,
            ),
        )
        effect = Effects.DrawCards(1) then
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cast a spell from your graveyard or activate an ability of a card " +
            "in your graveyard, draw a card and put a +1/+1 counter on Neerdiv."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "36"
        artist = "Yuchi Yuki"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/070e0081-b0fe-4417-b943-d0496e3b8cd7.jpg?1783908859"
    }
}
