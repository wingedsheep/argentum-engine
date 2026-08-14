package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Galvanic Discharge
 * {R}
 * Instant
 * Choose target creature or planeswalker. You get {E}{E}{E} (three energy counters), then you
 * may pay any amount of {E}. Galvanic Discharge deals that much damage to that permanent.
 *
 * Energy counters (CR 107.14) are tracked per player, not per permanent. Paying 0 is always legal
 * (2024-06-07 ruling: "You may pay zero {E}. You will get {E}{E}{E}, but Galvanic Discharge won't
 * deal any damage."). If the target becomes illegal before resolution, the whole spell fizzles
 * (CR 608.2b) — no energy gained, no damage dealt — handled by the engine's standard
 * illegal-target check for the spell's declared target, same as any other targeted spell.
 */
val GalvanicDischarge = card("Galvanic Discharge") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose target creature or planeswalker. You get {E}{E}{E} (three energy counters), " +
        "then you may pay any amount of {E}. Galvanic Discharge deals that much damage to that permanent."

    spell {
        val t = target("target", Targets.CreatureOrPlaneswalker)
        effect = Effects.Composite(
            Effects.GetEnergy(3),
            Effects.PayCounters(Counters.ENERGY, storeAmountAs = "paid"),
            Effects.DealDamage(DynamicAmount.VariableReference("paid"), t)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "122"
        artist = "Zoltan Boros"
        flavorText = "\"Remember, if you find a rock making crackly noises, just put it back. Carefully.\"\n" +
            "—Bo, Great Desert prospector"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32aa6e33-221f-414c-9b51-850d97a7e051.jpg?1783911272"
        ruling("2024-06-07", "You may pay zero {E}. You will get {E}{E}{E}, but Galvanic Discharge won't deal any damage.")
        ruling("2024-06-07", "Energy counters are a kind of counter that a player may have. They're not associated with any specific permanents.")
        ruling("2024-06-07", "If a spell or ability with one or more targets states that you \"may pay\" some amount of {E}, and each permanent that it targets has become an illegal target, the spell or ability won't resolve. You can't pay any {E} even if you want to.")
    }
}
