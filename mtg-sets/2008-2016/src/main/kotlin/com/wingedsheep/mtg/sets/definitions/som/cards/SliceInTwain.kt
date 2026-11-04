package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val SliceInTwain = card("Slice in Twain") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment.\nDraw a card."

    spell {
        val permanent = target(TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment))
        effect = Effects.Destroy(permanent) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "127"
        artist = "Efrem Palacios"
        flavorText = "\"The hypocrisy of these elves is thicker than steel—destroying 'unnatural metal' with their own enchanted swords.\"\n—Kara Vrist, Neurok agent"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de9c572a-6dc0-432f-92e9-c52fb0efddb5.jpg?1783941716"
        ruling("2017-09-29", "If the target artifact or enchantment is an illegal target by the time Slice in Twain resolves, the entire spell doesn’t resolve. You won’t draw a card. If, on the other hand, the target is a legal target but isn’t destroyed (most likely because it has indestructible), you’ll draw a card.")
    }
}
