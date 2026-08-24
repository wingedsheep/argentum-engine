package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/** Dust to Dust — exile two target artifacts. */
val DustToDust = card("Dust to Dust") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile two target artifacts."

    spell {
        target = TargetPermanent(count = 2, filter = TargetFilter.Artifact)
        effect = ForEachTargetEffect(listOf(Effects.Exile(EffectTarget.ContextTarget(0))))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Drew Tucker"
        flavorText = "Tervish never noticed that the amulet had vanished. It had disappeared not only from his possession, but from his memory as well."
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ade075fd-73ee-4d12-a2da-48e5938043af.jpg?1783947948"
        ruling("2004-10-04", "If one target is removed or becomes illegal after declaration, the other target is still affected.")
    }
}
