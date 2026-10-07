package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

val PlanarAtlas = card("Planar Atlas") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "When this artifact enters, you may look at the top four cards of your library. " +
        "If you do, reveal up to one land card from among them, then put that card on top of your library " +
        "and the rest on the bottom in a random order.\n{T}: Add {C}."

    replacementEffect(EntersTapped())
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val looked = gather(CardSource.TopOfLibrary(4))
                val (land, rest) = chooseUpToSplit(
                    1,
                    from = looked,
                    filter = GameObjectFilter.Land,
                    selectedLabel = "Reveal and put on top",
                    remainderLabel = "Put on bottom",
                    showAllCards = true
                )
                reveal(land)
                toLibraryTop(land, order = CardOrder.Preserve)
                toLibraryBottom(rest, order = CardOrder.Random)
            },
            prompt = "Look at the top four cards of your library?"
        )
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Alexander Forssberg"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/909b15e4-c6c8-44bf-a993-13a97de40e36.jpg?1783919176"
    }
}
