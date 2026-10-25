package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rewind — Urza's Saga #93 (canonical printing)
 * {2}{U}{U} · Instant
 *
 * Counter target spell. Untap up to four lands.
 *
 * Snap's shape: the untap is a resolution-time choice among every land on the battlefield, not
 * targets, and the lands needn't be yours (ruling). If the spell target is illegal, the whole
 * spell fizzles and nothing untaps.
 */
val Rewind = card("Rewind") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. Untap up to four lands."

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell() then
            Effects.Pipeline {
                val lands = gather(CardSource.BattlefieldMatching(GameObjectFilter.Land))
                val toUntap = chooseUpTo(4, from = lands)
                run(Effects.TapCollection(collection = toUntap, tap = false))
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Dermot Power"
        flavorText = "\"Time flows like a river. In Tolaria we practice the art of building dams.\"\n—Barrin, master wizard"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e51c4fb-fb29-4b1c-b78e-1fadf94fc9a5.jpg?1783946357"
        ruling("2020-06-23", "Rewind targets only a spell. It doesn't target any lands. The lands are chosen as Rewind resolves.")
        ruling("2020-06-23", "You can choose up to four lands, no matter who controls any of them. Those lands untap once.")
    }
}
