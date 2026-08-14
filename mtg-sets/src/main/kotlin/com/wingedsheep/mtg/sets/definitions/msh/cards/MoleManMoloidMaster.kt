package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect

/**
 * Mole Man, Moloid Master (MSH #177) — {2}{G} Legendary Creature — Human Villain · 1/1
 *
 * You may play lands from your graveyard.
 * Landfall — Whenever a land you control enters, create a 1/1 green Minion creature token named
 * Moloid with "Whenever this token attacks, you may mill a card."
 *
 * The graveyard land permission is the Icetill Explorer static ([MayPlayLandsFromGraveyard]) — it
 * grants permission only, so the normal one-land-per-turn limit still applies. "Landfall" is an
 * ability word, so it lives in the text and not in the trigger: [Triggers.LandYouControlEnters]
 * fires once per land entering under your control, including lands that enter without being
 * played.
 *
 * Moloid carries its own attack trigger, so it is a registered `PredefinedTokens` definition
 * minted via [CreatePredefinedTokenEffect] rather than an inline token spec.
 */
val MoleManMoloidMaster = card("Mole Man, Moloid Master") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Villain"
    power = 1
    toughness = 1
    oracleText = "You may play lands from your graveyard.\n" +
        "Landfall — Whenever a land you control enters, create a 1/1 green Minion creature token " +
        "named Moloid with \"Whenever this token attacks, you may mill a card.\""

    staticAbility { ability = MayPlayLandsFromGraveyard }

    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = CreatePredefinedTokenEffect("Moloid")
        description = "Landfall — Whenever a land you control enters, create a 1/1 green Minion " +
            "creature token named Moloid with \"Whenever this token attacks, you may mill a card.\""
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "177"
        artist = "Michele Giorgi"
        flavorText = "\"Forward, my mindless minions! To the surface, to take what's ours!\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b406cc5-75c9-430a-83a2-de0f2a8aeae6.jpg?1783902919"
    }
}
