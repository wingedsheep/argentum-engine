package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Copy Artifact
 * {1}{U}
 * Enchantment
 *
 * You may have this enchantment enter as a copy of any artifact on the battlefield, except it's an
 * enchantment in addition to its other types.
 *
 * The artifact counterpart of [CopyEnchantment][com.wingedsheep.mtg.sets.definitions.rav.cards.CopyEnchantment]:
 * an optional [EntersAsCopy] entry replacement (so the copied artifact's own ETBs still fire and the
 * player may decline), with "enchantment in addition to its other types" as a copy exception — part
 * of the copiable values, so a later copy of this permanent is an enchantment too.
 */
val CopyArtifact = card("Copy Artifact") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "You may have this enchantment enter as a copy of any artifact on the battlefield, " +
        "except it's an enchantment in addition to its other types."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Artifact,
            exceptions = CopyExceptions(addedCardTypes = setOf(CardType.ENCHANTMENT)),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd5ed955-1193-4e6a-a3e2-f54c1f9bf063.jpg?1783948707"
        ruling("2004-10-04", "The copy is both an artifact and an enchantment, so it is an artifact-enchantment (perhaps even an artifact-creature-enchantment). It can be affected by anything which affects either type of permanent.")
        ruling("2004-10-04", "The copy of the artifact is not still blue. It copies the color of the thing it is copying.")
        ruling("2004-10-04", "The artifact to copy is chosen at the time this card enters. If there is no valid artifact to choose, then this card enters as an enchantment that has no effect.")
    }
}
