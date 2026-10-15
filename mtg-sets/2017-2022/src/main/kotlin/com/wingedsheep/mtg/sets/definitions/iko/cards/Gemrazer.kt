package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gemrazer
 * {3}{G}
 * Creature — Beast
 * 4/4
 * Mutate {1}{G}{G}
 * Reach, trample
 * Whenever this creature mutates, destroy target artifact or enchantment an opponent controls.
 */
val Gemrazer = card("Gemrazer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Mutate {1}{G}{G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Reach, trample\n" +
        "Whenever this creature mutates, destroy target artifact or enchantment an opponent controls."

    keywordAbility(KeywordAbility.mutate("{1}{G}{G}"))
    keywords(Keyword.REACH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.mutates()
        val permanent = target(TargetFilter.ArtifactOrEnchantment.opponentControls())
        effect = Effects.Destroy(permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0095245c-a30e-4e2a-88c9-632c678e9f03.jpg"
    }
}
