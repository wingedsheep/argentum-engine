package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chromium, the Mutable — Core Set 2019 #214 (canonical printing)
 * {4}{W}{U}{B} · Legendary Creature — Elder Dragon · 7/7
 *
 * Flash
 * This spell can't be countered.
 * Flying
 * Discard a card: Until end of turn, Chromium becomes a Human with base power and toughness 1/1,
 * loses all abilities, and gains hexproof. It can't be blocked this turn.
 *
 * "Becomes a Human" replaces its creature types (layer 4) but keeps its card types, supertype and
 * color — it stays a legendary creature named Chromium. The base 1/1 is a layer 7b set, so pumps and
 * counters still apply on top. The ability strip is composed **before** the hexproof and
 * can't-be-blocked grants so they carry the later timestamps within layer 6 and survive it (the
 * Hellcat, Undying Vigilante ordering). Everything lasts until end of turn.
 */
val ChromiumTheMutable = card("Chromium, the Mutable") {
    manaCost = "{4}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 7
    toughness = 7
    oracleText = "Flash\n" +
        "This spell can't be countered.\n" +
        "Flying\n" +
        "Discard a card: Until end of turn, Chromium becomes a Human with base power and toughness " +
        "1/1, loses all abilities, and gains hexproof. It can't be blocked this turn."

    cantBeCountered = true
    keywords(Keyword.FLASH, Keyword.FLYING)

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.SetCreatureSubtypes(setOf("Human"), EffectTarget.Self, Duration.EndOfTurn) then
            Effects.SetBasePowerAndToughness(1, 1, EffectTarget.Self, Duration.EndOfTurn) then
            Effects.RemoveAllAbilities(EffectTarget.Self, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self, Duration.EndOfTurn) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self, Duration.EndOfTurn)
        description = "Discard a card: Until end of turn, Chromium becomes a 1/1 Human, loses all " +
            "abilities, and gains hexproof. It can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "214"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50c1de2c-1acc-47c8-9b5e-a9dae3da8a49.jpg?1783934522"
        ruling("2018-07-13", "Chromium stops being an Elder Dragon for the turn once its last ability has resolved. It's still a legendary creature named Chromium, the Mutable.")
        ruling("2018-07-13", "Chromium's activated ability overwrites all previous effects that set its base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after that ability resolves will overwrite this effect.")
        ruling("2018-07-13", "If Chromium gains an ability after its activated ability resolves, it will keep that ability.")
        ruling("2018-07-13", "Activating Chromium's last ability after it has become blocked won't cause Chromium to become unblocked.")
    }
}
