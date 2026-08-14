package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oviya, Automech Artisan — Aetherdrift #173
 * {3}{G} · Legendary Creature — Human Artificer · 1/2
 *
 * Each creature that's attacking one of your opponents has trample.
 * {G}, {T}: You may put a creature or Vehicle card from your hand onto the battlefield. If you put
 * an artifact onto the battlefield this way, put two +1/+1 counters on it.
 *
 * Modeling notes:
 *
 *  - **"Each creature that's attacking one of your opponents"** is deliberately not "each attacking
 *    creature you control": the grant follows the *defender*, not the attacker's controller. A
 *    creature attacking an opponent's planeswalker or battle is attacking, but it isn't attacking
 *    *the opponent*, so it gets no trample; a creature an ally controls that's attacking your
 *    opponent does. `StatePredicate.IsAttackingAnOpponent` is that check — the attacker's
 *    `defenderId` must be one of Oviya's controller's opponents, and `getOpponents` only ever
 *    yields players, so planeswalker/battle attacks drop out by construction.
 *
 *  - **"You may put …"** is a resolution-time choice from a hidden zone, not a target — so the pool
 *    is gathered from hand at resolution and offered as `chooseUpTo(1)`. Declining is legal (the
 *    activation still costs {G} and the tap).
 *
 *  - **"If you put an artifact onto the battlefield this way"** is checked *after* the move, on the
 *    permanent as it now exists — per the card's 2025-02-07 ruling, a nonartifact card can pick up
 *    the counters when something like Mycosynth Lattice makes it an artifact on the battlefield.
 *    The pipeline therefore filters the *moved* collection (post-move entity ids, already on the
 *    battlefield) for artifacts rather than pre-screening the hand pick. Nothing chosen, or a
 *    nonartifact creature chosen ⇒ the filter is empty and no counters are placed.
 *
 *  - **The counters go on the new permanent** ("on it"), not on Oviya, hence
 *    `AddCountersToCollection` over the filtered slot rather than a `Self`-targeted effect.
 */
val OviyaAutomechArtisan = card("Oviya, Automech Artisan") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Artificer"
    oracleText = "Each creature that's attacking one of your opponents has trample.\n" +
        "{G}, {T}: You may put a creature or Vehicle card from your hand onto the battlefield. If " +
        "you put an artifact onto the battlefield this way, put two +1/+1 counters on it."
    power = 1
    toughness = 2

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.attackingAnOpponent())
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        effect = Effects.Pipeline(
            descriptionOverride = "You may put a creature or Vehicle card from your hand onto the " +
                "battlefield. If you put an artifact onto the battlefield this way, put two +1/+1 " +
                "counters on it."
        ) {
            val hand = gather(
                CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.CreatureOrVehicle)
            )
            val chosen = chooseUpTo(
                1,
                from = hand,
                prompt = "You may put a creature or Vehicle card from your hand onto the battlefield",
                selectedLabel = "Put onto the battlefield"
            )
            val put = moveTracked(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            // Artifact-ness is read off the permanent that just entered, not off the card in hand.
            val artifacts = filter(put, GameObjectFilter.Artifact)
            run(Effects.AddCountersToCollection(artifacts.key, Counters.PLUS_ONE_PLUS_ONE, 2))
        }
        description = "{G}, {T}: You may put a creature or Vehicle card from your hand onto the " +
            "battlefield. If you put an artifact onto the battlefield this way, put two +1/+1 " +
            "counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Julia Metzger"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee5f504c-33fc-4a91-b69b-8ef555987c79.jpg?1783907867"
        ruling(
            "2025-02-07",
            "In some rare cases, a nonartifact card put onto the battlefield this way will get two " +
                "+1/+1 counters because some other effect (like that of Mycosynth Lattice) makes it " +
                "an artifact on the battlefield."
        )
    }
}
