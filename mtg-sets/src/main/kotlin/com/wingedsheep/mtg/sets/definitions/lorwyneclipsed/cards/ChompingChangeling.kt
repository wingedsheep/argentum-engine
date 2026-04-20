package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Chomping Changeling
 * {2}{G}
 * Creature — Shapeshifter
 * 1/2
 *
 * Changeling (This card is every creature type.)
 * When this creature enters, destroy up to one target artifact or enchantment.
 */
val ChompingChangeling = card("Chomping Changeling") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, destroy up to one target artifact or enchantment."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val permanent = target(
            "up to one target artifact or enchantment",
            TargetPermanent(optional = true, filter = TargetFilter.ArtifactOrEnchantment)
        )
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Jeff Lau"
        flavorText = "The knight was irritated when her rations went missing and truly baffled when her weapons started disappearing too."
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e187dcc6-19ad-4cf6-94b4-daf07f5144e5.jpg?1767658367"
    }
}
