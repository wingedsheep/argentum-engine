package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Attuned Hunter — Tarkir: Dragonstorm #135
 * {2}{G} · Creature — Human Ranger · 3/3
 *
 * Trample
 * Whenever one or more cards leave your graveyard during your turn, put a +1/+1 counter
 * on this creature.
 *
 * "During your turn" is the trigger's timing restriction, expressed as
 * triggerCondition = Conditions.IsYourTurn. The batching leave-graveyard trigger fires once
 * per event batch, so it can grow this creature multiple times in a turn (one per batch).
 */
val AttunedHunter = card("Attuned Hunter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Ranger"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "Whenever one or more cards leave your graveyard during your turn, put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard()
        triggerCondition = Conditions.IsYourTurn
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1a4f502-86a9-49fb-9cb9-7918d13c5313.jpg?1743204506"
    }
}
