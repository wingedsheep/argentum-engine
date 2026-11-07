package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vengeful Rebel
 * {2}{B}
 * Creature — Aetherborn Warrior
 * 3/2
 *
 * Revolt — When this creature enters, if a permanent left the battlefield under your control
 * this turn, target creature an opponent controls gets -3/-3 until end of turn.
 *
 * Revolt is a CR 603.4 intervening-if (checked on entering and again on resolution), so it rides
 * on `interveningIf` with the per-player [Conditions.YouHadPermanentLeaveBattlefieldThisTurn]
 * tracker, as on Deadeye Harpooner.
 *
 * Aether Revolt is the earliest printing; the only later one is Kaladesh Remastered (KLR),
 * which isn't scaffolded.
 */
val VengefulRebel = card("Vengeful Rebel") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Aetherborn Warrior"
    oracleText = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, target creature an opponent controls gets -3/-3 until end of turn."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.ModifyStats(-3, -3, creature)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, target creature an opponent controls gets -3/-3 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Tomasz Jedruszek"
        flavorText = "Some aetherborn joined the rebellion for profit, others for the sake of their freedom, still others for revenge."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e34edfc-77fc-43b5-bad6-1c4c2a76c8c3.jpg?1783936758"
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
