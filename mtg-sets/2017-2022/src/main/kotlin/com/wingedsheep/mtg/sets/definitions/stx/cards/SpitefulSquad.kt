package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spiteful Squad — Strixhaven: School of Mages #237 (canonical printing)
 * {2}{W}{B} · Creature — Human Warlock · 0/0
 *
 * Deathtouch
 * This creature enters with two +1/+1 counters on it.
 * When this creature dies, put its counters on target creature you control.
 *
 * The dies trigger is [Effects.MoveAllLastKnownCounters]: it reads every counter kind the Squad
 * had as it died (last-known information) and puts the same number of each on the target —
 * -1/-1 counters included, per the ruling.
 */
val SpitefulSquad = card("Spiteful Squad") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Warlock"
    power = 0
    toughness = 0
    oracleText = "Deathtouch\n" +
        "This creature enters with two +1/+1 counters on it.\n" +
        "When this creature dies, put its counters on target creature you control."

    keywords(Keyword.DEATHTOUCH)

    replacementEffect(EntersWithCounters(count = 2, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.self.dies()
        val recipient = target(TargetFilter.CreatureYouControl)
        effect = Effects.MoveAllLastKnownCounters(recipient)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "237"
        artist = "Manuel Castañón"
        flavorText = "\"Don't you know first-years pay a toll to walk through here?\""
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be88355e-c986-4b69-93b4-0abf04916e86.jpg?1783927290"
        ruling("2021-04-16", "Spiteful Squad's ability doesn't cause you to move counters from itself to the target creature. Rather, you put the same number of each kind of counter Spiteful Squad had when it died onto that creature.")
        ruling("2021-04-16", "If Spiteful Squad has -1/-1 counters on it when it dies, the last ability will include those as well. This may result in the recipient also dying. Spiteful indeed.")
    }
}
