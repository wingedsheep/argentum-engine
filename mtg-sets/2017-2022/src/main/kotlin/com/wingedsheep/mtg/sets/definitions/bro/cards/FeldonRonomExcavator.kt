package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Feldon, Ronom Excavator — The Brothers' War #135
 * {1}{R} · Legendary Creature — Human Artificer · 2/2
 *
 * Haste
 * Feldon can't block.
 * Whenever Feldon is dealt damage, exile that many cards from the top of your library. Choose one
 * of them. Until the end of your next turn, you may play that card.
 *
 * "That many" is the damage dealt in the triggering event ([DynamicAmounts.triggerDamageAmount],
 * as Broodhatch Nantuko). The exiled-then-choose-one shape is Fireglass Mentor's pipeline with the
 * turn-keyed `UntilEndOfNextTurn` expiry. Each damage event triggers separately.
 */
val FeldonRonomExcavator = card("Feldon, Ronom Excavator") {
    manaCost = "{1}{R}"
    typeLine = "Legendary Creature — Human Artificer"
    power = 2
    toughness = 2
    oracleText = "Haste\n" +
        "Feldon can't block.\n" +
        "Whenever Feldon is dealt damage, exile that many cards from the top of your library. " +
        "Choose one of them. Until the end of your next turn, you may play that card."

    keywords(Keyword.HASTE)

    staticAbility {
        ability = CantBlock(filter = GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.triggerDamageAmount()))
            exile(exiled)
            val chosen = chooseExactly(1, from = exiled, prompt = "Choose a card you may play until the end of your next turn")
            run(Effects.GrantMayPlayFromExile(from = chosen, expiry = MayPlayExpiry.UntilEndOfNextTurn))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5ca4cb0e-63ad-4275-a27f-da476260b467.jpg?1783920069"
        ruling("2022-10-14", "You must pay all costs and follow all normal timing rules when casting a spell this way.")
    }
}
