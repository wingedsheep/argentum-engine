package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Culling Ritual — Strixhaven #172
 * {2}{B}{G} · Sorcery · Rare
 *
 * Destroy each nonland permanent with mana value 2 or less. Add {B} or {G} for each permanent
 * destroyed this way.
 *
 * The count is the stored list of permanents *actually* destroyed (`storeDestroyedAs`), so an
 * indestructible or regenerated permanent adds nothing. Each mana is independently {B} or {G}
 * (the 2021-04-16 ruling), which is `AddManaInAnyCombination` over those two colors.
 */
val CullingRitual = card("Culling Ritual") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Destroy each nonland permanent with mana value 2 or less. " +
        "Add {B} or {G} for each permanent destroyed this way."

    spell {
        effect = Effects.Pipeline {
            val destroyed = runStoringCollection {
                Effects.DestroyAll(GameObjectFilter.NonlandPermanent.manaValueAtMost(2), storeDestroyedAs = it)
            }
            run(Effects.AddManaInAnyCombination(destroyed.count, setOf(Color.BLACK, Color.GREEN)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Lorenzo Mastroianni"
        flavorText = "\"Your own frailty is your undoing. I am merely the one to expose it.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f6f91d3-cc07-4a42-99a0-5fb83b29cc25.jpg?1783927321"
        ruling("4/16/2021", "You can choose to add either {B} or {G} for each permanent destroyed. You aren't limited to one color.")
    }
}
