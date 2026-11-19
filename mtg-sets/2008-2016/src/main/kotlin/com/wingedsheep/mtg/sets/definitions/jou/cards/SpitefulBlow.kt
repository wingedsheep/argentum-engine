package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spiteful Blow
 * {4}{B}{B}
 * Sorcery
 * Destroy target creature and target land.
 */
val SpitefulBlow = card("Spiteful Blow") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature and target land."

    spell {
        val creature = target(TargetFilter.Creature)
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(creature) then Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Jason Chan"
        flavorText = "\"Only a fool takes Phenax at his word. What do you expect from a god born of lies and betrayal?\"\n—Verenes, priest of Iroas"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fafaa798-e534-4cd0-b369-9e767a02fe3d.jpg?1783939430"
        ruling("2014-04-26", "You can't cast Spiteful Blow without a legal creature and land to target. If one of those targets is illegal when Spiteful Below tries to resolve, the other one will still be destroyed.")
    }
}
