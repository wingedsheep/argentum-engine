package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Never // Return — Amonkhet #212
 *
 * Never {1}{B}{B} — Sorcery: Destroy target creature or planeswalker.
 * Return {3}{B} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Exile target card from a graveyard. Create a 2/2 black Zombie creature token.
 *
 * Return has a single target, so if that card leaves the graveyard before Return resolves the spell
 * doesn't resolve and no Zombie is created.
 */
val NeverReturn = card("Never // Return") {
    layout = CardLayout.SPLIT
    colorIdentity = "B"

    face("Never") {
        manaCost = "{1}{B}{B}"
        typeLine = "Sorcery"
        oracleText = "Destroy target creature or planeswalker."

        spell {
            val t = target(Targets.CreatureOrPlaneswalker)
            effect = Effects.Destroy(t)
        }
    }

    face("Return") {
        manaCost = "{3}{B}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Exile target card from a graveyard. Create a 2/2 black Zombie creature token."
        keywords(Keyword.AFTERMATH)

        spell {
            val t = target(TargetFilter.CardInGraveyard)
            effect = Effects.Move(t, Zone.EXILE) then Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1783936411"
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4b32135-7061-4278-a01a-4fcbaadc9706.jpg?1783936458"
        ruling("2017-07-14", "Once you've started to cast a spell with aftermath from your graveyard, the card is immediately moved to the stack. Opponents can't try to stop the ability by exiling the card with an effect such as that of Crook of Condemnation.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
