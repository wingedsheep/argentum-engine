package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Springbloom Druid
 * {2}{G}
 * Creature — Elf Druid
 * 1/1
 * When this creature enters, you may sacrifice a land. If you do, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle.
 */
val SpringbloomDruid = card("Springbloom Druid") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you may sacrifice a land. If you do, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Sacrifice(GameObjectFilter.Land, count = 1, target = EffectTarget.Controller) then
                Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.BasicLand,
                    count = 2,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = true
                )
        )
        description = "When this creature enters, you may sacrifice a land. If you do, search your library for up to two basic land cards, put them onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Randy Gallegos"
        flavorText = "\"New growth applies a healing poultice to wounds long scabbed over.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/1/6161d2ed-7cff-4c90-9e74-1d179a6c1498.jpg?1783933092"
    }
}
