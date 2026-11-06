package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Paradox Engine
 * {5}
 * Legendary Artifact
 * Whenever you cast a spell, untap all nonland permanents you control.
 *
 * A cast trigger ([Triggers.you].casts) over Dramatic Reversal's group untap: it resolves before
 * the spell that caused it, and untaps Paradox Engine itself along with every other nonland
 * permanent you control.
 */
val ParadoxEngine = card("Paradox Engine") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "Whenever you cast a spell, untap all nonland permanents you control."

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Patterns.Group.untapGroup(
            GroupFilter(GameObjectFilter.NonlandPermanent.youControl())
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "169"
        artist = "Christine Choi"
        flavorText = "\"One look at this glorious item convinced me that Kaladesh deserved my full attention. Sure enough, my time here has yielded exquisite fruit.\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd8ccd81-9e11-47fa-8e16-064c52c24506.jpg"
        ruling("2017-02-09", "Paradox Engine's triggered ability resolves before the spell that caused it to trigger. The ability will resolve even if that spell is countered.")
    }
}
