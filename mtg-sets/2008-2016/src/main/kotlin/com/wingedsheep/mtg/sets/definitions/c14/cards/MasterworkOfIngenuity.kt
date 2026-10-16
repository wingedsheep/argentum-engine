package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Masterwork of Ingenuity
 * {1}
 * Artifact — Equipment
 *
 * You may have this Equipment enter as a copy of any Equipment on the battlefield.
 *
 * An optional [EntersAsCopy] entry replacement filtered to Equipment. Copying takes the copied
 * Equipment's copiable values, equip ability included; declining (or no Equipment to copy) leaves
 * an Equipment with no equip ability. It always enters unattached.
 */
val MasterworkOfIngenuity = card("Masterwork of Ingenuity") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "You may have this Equipment enter as a copy of any Equipment on the battlefield."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "57"
        artist = "Ulrich Brunin"
        flavorText = "\"The crucible yields what you have forged in your mind. You need only reach in and draw the masterwork forth.\"\n—Wahara, Keeper of the Crucible"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09686fb3-ae0f-481a-aee6-3fd75fa43557.jpg?1783938862"
        ruling("2020-08-07", "Masterwork of Ingenuity enters the battlefield unattached. It doesn't enter attached to the same creature as the Equipment it copies.")
    }
}
