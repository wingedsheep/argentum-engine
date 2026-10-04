package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Selesnya Sanctuary
 * Land
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand.
 * {T}: Add {G}{W}.
 *
 * "Return a land you control" doesn't target (CR 115.10a): the land is chosen as the
 * trigger resolves, so this gathers, chooses and moves rather than binding a target —
 * the same shape as Shrieking Drake's ETB. With no other land, this one is the only choice.
 *
 * Canonical printing: Scryfall lists the "Salvat 2005" box product (`psal`, 2005-08-22) as a
 * slightly earlier date than Ravnica: City of Guilds (2005-10-07), but `psal` is a regional
 * book-bundle exclusive, not a mainline expansion (matches this project's
 * `check-card-printing` scaffoldable-set-type policy, which already excludes it) — RAV is the
 * canonical printing.
 */
val SelesnyaSanctuary = card("Selesnya Sanctuary") {
    typeLine = "Land"
    colorIdentity = "GW"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand.\n" +
        "{T}: Add {G}{W}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val lands = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Land, player = Player.You))
            val returned = chooseExactly(
                1,
                from = lands,
                prompt = "Return a land you control to its owner's hand",
                useTargetingUI = true
            )
            toHand(returned)
        }
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN) then Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "281"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5e51787-f9c9-4926-9df1-a384a3092676.jpg?1783943590"
        ruling("2013-04-15", "If this land enters the battlefield and you control no other lands, its ability will force you to return it to your hand.")
    }
}
