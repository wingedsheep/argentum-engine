package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Yosei, the Morning Star
 * {4}{W}{W}
 * Legendary Creature — Dragon Spirit
 * 5/5
 * Flying
 * When Yosei dies, target player skips their next untap step. Tap up to five target permanents
 * that player controls.
 *
 * The permanents slot reads the player slot, so the trigger picks the player first and then offers
 * only that player's permanents (dependent target selection, its last slot taking up to five).
 */
val YoseiTheMorningStar = card("Yosei, the Morning Star") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Dragon Spirit"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhen Yosei dies, target player skips their next untap step. Tap up to five " +
        "target permanents that player controls."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        val player = target(Targets.Player)
        val permanents = targets(
            TargetFilter(GameObjectFilter.Permanent.targetPlayerControls(player)),
            count = 5,
            optional = true,
        )
        effect = permanents.fold(Effects.SkipNextUntapStep(player)) { effect, permanent ->
            effect then Effects.Tap(permanent)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Hiro Izawa"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b38f14a-bd10-47c1-8772-5abe0a0b243f.jpg?1783944330"
    }
}
