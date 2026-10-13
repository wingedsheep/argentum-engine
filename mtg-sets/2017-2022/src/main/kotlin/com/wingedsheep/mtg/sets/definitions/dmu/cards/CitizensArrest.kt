package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Citizen's Arrest — "until this enchantment leaves the battlefield" is
 * [Effects.MoveUntilSourceLeaves]: a one-shot exile whose return needs no leaves trigger, and
 * nothing is exiled if Citizen's Arrest is already gone when the trigger resolves.
 */
val CitizensArrest = card("Citizen's Arrest") {
    manaCost = "{1}{W}{W}"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, exile target creature or planeswalker an opponent controls until this enchantment leaves the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.MoveUntilSourceLeaves(victim, Zone.EXILE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9806370b-51e3-4144-a847-6b367fb937ef.jpg?1783921370"
        flavorText = "\"What lurks behind the faces of those you think you can trust?\""
        ruling("2022-09-09", "If Citizen's Arrest leaves the battlefield before its triggered ability resolves, the target permanent won't be exiled.")
        ruling("2022-09-09", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
    }
}
