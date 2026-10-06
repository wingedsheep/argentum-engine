package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kari Zev, Skyship Raider
 * {1}{R}
 * Legendary Creature — Human Pirate
 * 1/3
 * First strike, menace
 * Whenever Kari Zev attacks, create Ragavan, a legendary 2/1 red Monkey creature token. Ragavan enters
 * tapped and attacking. Exile that token at end of combat.
 */
val KariZevSkyshipRaider = card("Kari Zev, Skyship Raider") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Pirate"
    oracleText =
        "First strike, menace\n" +
            "Whenever Kari Zev attacks, create Ragavan, a legendary 2/1 red Monkey creature token. " +
            "Ragavan enters tapped and attacking. Exile that token at end of combat."
    power = 1
    toughness = 3

    keywords(Keyword.FIRST_STRIKE, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Monkey"),
            name = "Ragavan",
            legendary = true,
            tapped = true,
            attacking = true,
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1ebc91a9-23e0-4ca1-bc6d-e710ad2efb31.jpg?1783936712",
        ) then Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Move(
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
                destination = Zone.EXILE,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Brad Rigney"
        flavorText = "Aboard her ship, the *Dragon's Smile*, Kari follows no one's rules but her own."
        imageUri =
            "https://cards.scryfall.io/normal/front/7/2/72495879-39ce-449d-ad2f-ef32ea46f3aa.jpg?1783936752"
        ruling(
            "2017-02-09",
            "The delayed triggered ability that exiles Ragavan triggers at end of combat even if Kari Zev is no longer on the battlefield.",
        )
    }
}
