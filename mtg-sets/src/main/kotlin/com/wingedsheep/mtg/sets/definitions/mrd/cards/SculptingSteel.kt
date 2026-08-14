package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sculpting Steel — Mirrodin #238
 * {3} · Artifact
 *
 * You may have this artifact enter as a copy of any artifact on the battlefield.
 *
 * Clone for artifacts. Same [EntersAsCopy] replacement effect as [
 * com.wingedsheep.mtg.sets.definitions.ktk.cards.CleverImpersonator], narrowed to artifacts —
 * "any artifact", so an opponent's is fair game, and so is another Sculpting Steel.
 *
 * `optional = true` because the copy is a "may": declining leaves a plain colorless {3} artifact
 * with no abilities on the battlefield, which is a real (if bad) choice, not a no-op. The choice
 * is made as the permanent enters (CR 706.2), so it never uses the stack and can't be responded to.
 */
val SculptingSteel = card("Sculpting Steel") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "You may have this artifact enter as a copy of any artifact on the battlefield."

    replacementEffect(EntersAsCopy(optional = true, copyFilter = GameObjectFilter.Artifact))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Heather Hudson"
        flavorText = "An artificer once dropped one in a vault full of coins. She has yet to find it."
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3aac5f6f-97c1-4546-94ed-016292e98c9d.jpg?1783944505"
    }
}
