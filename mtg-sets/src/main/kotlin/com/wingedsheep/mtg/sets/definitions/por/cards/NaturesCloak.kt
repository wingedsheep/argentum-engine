// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Nature's Cloak
 * {2}{G}
 * Sorcery
 * Green creatures you control gain forestwalk until end of turn. (They can't be blocked as long as defending player controls a Forest.)
 */
val NaturesCloak = card("Nature's Cloak") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN).youControl()), GrantKeywordEffect(Keyword.FORESTWALK, EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Rebecca Guay"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1dfaba58-d0ab-4d1d-91dd-48543c862165.jpg"
    }
}
