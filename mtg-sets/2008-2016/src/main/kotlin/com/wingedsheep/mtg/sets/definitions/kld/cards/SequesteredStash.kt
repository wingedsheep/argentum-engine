package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sequestered Stash (KLD 248)
 * Land
 * {T}: Add {C}.
 * {4}, {T}, Sacrifice this land: Mill five cards. Then you may put an artifact card from your
 * graveyard on top of your library.
 *
 * The artifact isn't targeted and is chosen on resolution after the mill, so it may be one of the
 * five cards just milled (see ruling).
 */
val SequesteredStash = card("Sequestered Stash") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{4}, {T}, Sacrifice this land: Mill five cards. Then you may put an artifact card from your " +
        "graveyard on top of your library."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Pipeline {
            mill(5)
            val artifacts = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Artifact)
            )
            val chosen = chooseUpTo(
                1,
                from = artifacts,
                prompt = "You may put an artifact card from your graveyard on top of your library",
                selectedLabel = "Put on top of library",
                remainderLabel = "Leave in graveyard"
            )
            toLibraryTop(chosen)
        }
        description = "{4}, {T}, Sacrifice this land: Mill five cards. Then you may put an artifact card " +
            "from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86a17084-bb96-4e81-bff0-005bd44a1fbd.jpg?1783937143"
        ruling(
            "2016-09-20",
            "Sequestered Stash's ability doesn't target the artifact card in your graveyard. You may choose one of the five cards you put there from your library."
        )
    }
}
