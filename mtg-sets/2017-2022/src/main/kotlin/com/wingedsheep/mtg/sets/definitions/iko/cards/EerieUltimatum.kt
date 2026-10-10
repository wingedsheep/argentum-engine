package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Eerie Ultimatum — Ikoria: Lair of Behemoths #184
 * {W}{W}{B}{B}{B}{G}{G} · Sorcery
 *
 * Return any number of permanent cards with different names from your graveyard to the
 * battlefield.
 *
 * Not targeted — the cards are chosen on resolution (per ruling), so this is the Gather →
 * Select → Move pipeline rather than Behold the Sinister Six!'s targeted shape: gather every
 * permanent card in your graveyard → `ChooseAnyNumber` under
 * [SelectionRestriction.OnePerCardName] ("with different names") → move the whole selection to
 * the battlefield in one step, so the cards enter simultaneously.
 */
val EerieUltimatum = card("Eerie Ultimatum") {
    manaCost = "{W}{W}{B}{B}{B}{G}{G}"
    colorIdentity = "WBG"
    typeLine = "Sorcery"
    oracleText = "Return any number of permanent cards with different names from your graveyard to the battlefield."

    spell {
        effect = Effects.Pipeline {
            val graveyardPermanents = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Permanent)
            )
            val returned = chooseAnyNumber(
                from = graveyardPermanents,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Return any number of permanent cards with different names to the battlefield",
                selectedLabel = "Return to the battlefield",
                remainderLabel = "Leave in your graveyard"
            )
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "184"
        artist = "Jason A. Engle"
        flavorText = "\"The ground under our feet is a record of every slight against the world, to be " +
            "avenged at a time of its choosing.\"\n—Gavi, nest warden"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a9fb2db-228f-4d48-acdf-6330baf356c7.jpg?1783931025"
        ruling(
            "2025-04-04",
            "You choose which permanent cards to return while Eerie Ultimatum is resolving. No player " +
                "may take actions between the time you choose and the time those cards return to the battlefield.",
        )
        ruling("2025-04-04", "You may choose to return just one permanent card, regardless of its name.")
        ruling(
            "2025-04-04",
            "An Aura being put onto the battlefield this way can't enchant anything else that is being " +
                "put onto the battlefield at the same time.",
        )
    }
}
