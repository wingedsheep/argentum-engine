package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Brightglass Gearhulk
 * {G}{G}{W}{W}
 * Artifact Creature — Construct
 * 4/4
 * First strike, trample
 * When this creature enters, you may search your library for up to two artifact, creature,
 * and/or enchantment cards with mana value 1 or less, reveal them, put them into your hand,
 * then shuffle.
 */
val BrightglassGearhulk = card("Brightglass Gearhulk") {
    manaCost = "{G}{G}{W}{W}"
    colorIdentity = "GW"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "First strike, trample\nWhen this creature enters, you may search your library for " +
        "up to two artifact, creature, and/or enchantment cards with mana value 1 or less, reveal " +
        "them, put them into your hand, then shuffle."
    keywords(Keyword.FIRST_STRIKE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.ArtifactCreatureOrEnchantment.manaValueAtMost(1),
                count = 2,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "191"
        artist = "José Parodi"
        flavorText = "\"A new day dawns for Avishkar, and we shall draw our power from its light.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dea5b45-925c-4732-8e9d-fa8232792736.jpg?1782687811"
    }
}
