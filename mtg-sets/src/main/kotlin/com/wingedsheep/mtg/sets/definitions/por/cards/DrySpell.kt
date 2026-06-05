// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Dry Spell
 * {1}{B}
 * Sorcery
 * Dry Spell deals 1 damage to each creature and each player.
 */
val DrySpell = card("Dry Spell") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        effect = CompositeEffect(
        listOf(
            ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature), DealDamageEffect(1, EffectTarget.Self)),
            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(1, EffectTarget.Controller)))
        )
    )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Roger Raupp"
        flavorText = "A fist of dust to line your throat, a bowl of sand to fill your belly."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a142f369-8fdd-4dc8-b5d9-3493455cc588.jpg"
    }
}
