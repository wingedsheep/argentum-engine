package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Idol of Endurance — Core Set 2021 #23 (canonical printing)
 * {2}{W} · Artifact
 *
 * When this artifact enters, exile all creature cards with mana value 3 or less from your
 * graveyard until this artifact leaves the battlefield.
 * {1}{W}, {T}: Until end of turn, you may cast a creature spell from among cards exiled with this
 * artifact without paying its mana cost.
 *
 * The exile is a [Effects.MoveUntilSourceLeaves] per card: the return to the graveyard is part of
 * the one-shot effect, not a leaves trigger, and a card cast off the pile is a new object that
 * stays where it is (2020-06-23 rulings). Each activation is its own `maxPlays = 1` grant over the
 * creature cards still in the pile, with the mana cost waived — activating twice lets you cast two.
 * `nonLandOnly` keeps a land creature card (Dryad Arbor) from being *played* through a grant that
 * only lets you cast.
 */
val IdolOfEndurance = card("Idol of Endurance") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, exile all creature cards with mana value 3 or less from " +
        "your graveyard until this artifact leaves the battlefield.\n" +
        "{1}{W}, {T}: Until end of turn, you may cast a creature spell from among cards exiled with " +
        "this artifact without paying its mana cost."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val idolExiles = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature.manaValueAtMost(3))
            )
            run(Effects.ForEachInCollection(idolExiles, Effects.MoveUntilSourceLeaves(EffectTarget.IterationEntity, Zone.EXILE)))
        }
        description = "When this artifact enters, exile all creature cards with mana value 3 or less " +
            "from your graveyard until this artifact leaves the battlefield."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        effect = Effects.Pipeline {
            val idolCreatures = filter(gather(CardSource.FromLinkedExile()), GameObjectFilter.Creature)
            run(Effects.GrantMayPlayFromExile(idolCreatures, maxPlays = 1, nonLandOnly = true))
            run(Effects.GrantPlayWithoutPayingCost(idolCreatures))
        }
        description = "Until end of turn, you may cast a creature spell from among cards exiled with " +
            "this artifact without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Kim Sokol"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d60e84e-be05-49da-9720-4225abe9d003.jpg?1783930739"
    }
}
