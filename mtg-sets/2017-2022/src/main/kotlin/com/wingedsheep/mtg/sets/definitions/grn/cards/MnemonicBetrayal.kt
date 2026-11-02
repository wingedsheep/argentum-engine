package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mnemonic Betrayal
 * {1}{B}{B}
 * Sorcery
 *
 * Exile all opponents' graveyards. You may cast spells from among those cards this turn, and mana
 * of any type can be spent to cast them. At the beginning of the next end step, if any of those
 * cards remain exiled, return them to their owners' graveyards.
 * Exile Mnemonic Betrayal.
 *
 * Modeling notes:
 *  - The exiled pile is a named collection so the end-step delayed trigger can carry it
 *    (`carryCollections`, CR 603.7c "those cards"). A carried card that has since changed zones
 *    (cast, or cast and countered back to a graveyard) is a new object and is dropped when the
 *    trigger fires, so only the cards that "remain exiled" are returned. With none left the move
 *    is empty, which is what the intervening "if" yields.
 *  - "Cast spells" — `nonLandOnly = true`, so an exiled land can't be played.
 *  - The graveyard move routes each card to its owner's graveyard.
 *  - "Exile Mnemonic Betrayal." is `selfExile()`.
 */
val MnemonicBetrayal = card("Mnemonic Betrayal") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Exile all opponents' graveyards. You may cast spells from among those cards this turn, " +
        "and mana of any type can be spent to cast them. At the beginning of the next end step, if any " +
        "of those cards remain exiled, return them to their owners' graveyards.\nExile Mnemonic Betrayal."

    spell {
        selfExile()
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.EachOpponent))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(
                from = exiled,
                expiry = MayPlayExpiry.EndOfTurn,
                withAnyManaType = true,
                nonLandOnly = true
            ))
            run(Effects.CreateDelayedTrigger(
                step = Step.END,
                effect = Effects.Pipeline {
                    toGraveyard(exiled)
                },
                carryCollections = listOf(exiled.key)
            ))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "189"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5cf45aa-ed34-4add-a2ec-fc11f8c15ffa.jpg?1783934127"
    }
}
