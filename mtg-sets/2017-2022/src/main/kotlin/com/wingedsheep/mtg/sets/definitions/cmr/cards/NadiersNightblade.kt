package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nadier's Nightblade — Commander Legends #136
 * {2}{B} · Creature — Elf Warrior 1/3 · Uncommon
 *
 * Whenever a token you control leaves the battlefield, each opponent loses 1 life and you gain
 * 1 life.
 *
 * **Any token, any destination.** The filter is the bare [GameObjectFilter.Token] — not creature
 * tokens only — so Treasures, Clues and Food leaving count, and the trigger is a destination-less
 * `leaves()` so sacrifice, exile and bounce all fire it. Control is read off the token as it last
 * existed on the battlefield.
 *
 * The drain is two separate effects so the loss reaches every opponent while the gain stays a
 * fixed 1, however many opponents there are.
 */
val NadiersNightblade = card("Nadier's Nightblade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Elf Warrior"
    power = 1
    toughness = 3
    oracleText = "Whenever a token you control leaves the battlefield, each opponent loses 1 life " +
        "and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Token.youControl()).leaves()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever a token you control leaves the battlefield, each opponent loses 1 " +
            "life and you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Randy Vargas"
        flavorText = "Checking around every corner doesn't help if you forget to look up."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11fbba08-0d93-4750-9dd6-d6a2779c6cf3.jpg?1783928832"
    }
}
